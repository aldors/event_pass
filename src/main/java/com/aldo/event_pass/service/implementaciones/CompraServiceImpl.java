package com.aldo.event_pass.service.implementaciones;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aldo.event_pass.dto.pago.CheckoutResponse;
import com.aldo.event_pass.dto.boleto.BoletoDetalleResponse;
import com.aldo.event_pass.dto.reservacion.BoletoReservaRequest;
import com.aldo.event_pass.dto.reservacion.CompraDetalleResponse;
import com.aldo.event_pass.dto.reservacion.CompraResumenResponse;
import com.aldo.event_pass.dto.reservacion.ReservaResponse;
import com.aldo.event_pass.dto.reservacion.ReservarBoletosRequest;
import com.aldo.event_pass.entity.Boleto;
import com.aldo.event_pass.entity.Compra;
import com.aldo.event_pass.entity.DetalleCompra;
import com.aldo.event_pass.entity.Evento;
import com.aldo.event_pass.entity.Pago;
import com.aldo.event_pass.entity.TipoBoleto;
import com.aldo.event_pass.entity.Usuario;
import com.aldo.event_pass.enums.EstadoCompra;
import com.aldo.event_pass.enums.EstadoEvento;
import com.aldo.event_pass.enums.EstadoPago;
import com.aldo.event_pass.exception.TipoBoletoNoEncontradoException;
import com.aldo.event_pass.exception.TipoBoletoNoPerteneceAlEventoException;
import com.aldo.event_pass.exception.CompraNoEncontradaException;
import com.aldo.event_pass.exception.CompraNoReservadaException;
import com.aldo.event_pass.exception.CompraYaPagadaException;
import com.aldo.event_pass.exception.EventoIniciadoException;
import com.aldo.event_pass.exception.EventoNoEncontradoException;
import com.aldo.event_pass.exception.ExcederMaximosBoletosPorUsuarioException;
import com.aldo.event_pass.exception.InsuficienciaDeBoletosException;
import com.aldo.event_pass.exception.ReservaExpiradaException;
import com.aldo.event_pass.exception.ReservarBoletosEnEventosPublicadosException;
import com.aldo.event_pass.exception.StripeNoDisponibleException;
import com.aldo.event_pass.exception.TipoBoletoInexistenteException;
import com.aldo.event_pass.repository.BoletoRepository;
import com.aldo.event_pass.repository.CompraRepository;
import com.aldo.event_pass.repository.DetalleCompraRepository;
import com.aldo.event_pass.repository.EventoRepository;
import com.aldo.event_pass.repository.PagoRepository;
import com.aldo.event_pass.repository.TipoBoletoRepository;
import com.aldo.event_pass.security.CurrentUserService;
import com.aldo.event_pass.service.interfaces.CompraService;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompraServiceImpl implements CompraService {

    private final CurrentUserService currentUserService;
    private final EventoRepository eventoRepository;
    private final BoletoRepository boletoRepository;
    private final TipoBoletoRepository tipoBoletoRepository;
    private final DetalleCompraRepository detalleCompraRepository;
    private final CompraRepository compraRepository;
    private final PagoRepository pagoRepository;
    private final StripeClient stripeClient;
    private final StripeWebhookService stripeWebhookService;

    private static final int RESERVA_MINUTOS = 15;
    private static final int STRIPE_SESION_MINUTOS = 30;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.stripe.currency:mxn}")
    private String stripeCurrency;

    @Override
    @Transactional 
    public ReservaResponse reservarBoletos(ReservarBoletosRequest reservarBoletosRequest) {

        Usuario usuario = currentUserService.obtenerUsuarioActual();

        Evento evento = eventoRepository.findById(reservarBoletosRequest.getEventoId())
            .orElseThrow(() -> new EventoNoEncontradoException());

        if(evento.getEstado() != EstadoEvento.PUBLICADO){
            throw new ReservarBoletosEnEventosPublicadosException();
        }

        if(!evento.getFechaInicio().isAfter(LocalDateTime.now())){
            throw new EventoIniciadoException();
        }

        // Boletos que ya ha comprado este usuario en este evento
        Long boletosActuales = boletoRepository.contarBoletosUsuarioPorEvento(usuario.getId(), evento.getId());

        int boletosNuevos = reservarBoletosRequest.getBoletos().size();

        if((boletosActuales + boletosNuevos) > evento.getMaxBoletosPorUsuario()){
            throw new ExcederMaximosBoletosPorUsuarioException();
        }

        // 1 = {titular1, titular2},
        // 2 = {titular3}
        Map<Long, List<BoletoReservaRequest>> boletosAgrupados = reservarBoletosRequest.getBoletos().stream()
            .collect(Collectors.groupingBy(
                BoletoReservaRequest::getTipoBoletoId
            ));

        // Ordena los ids de los boletos (key)
        List<Long> ids = boletosAgrupados.keySet().stream().sorted().toList();

       // Bloquea las filas donde TipoBoleto.id IN (ids) para evitar concurrencias
        List<TipoBoleto> tiposBoleto = tipoBoletoRepository.buscarTodosParaReserva(ids);

        if(tiposBoleto.size() != boletosAgrupados.size()) {
            throw new TipoBoletoInexistenteException();
        }

        Map<Long, TipoBoleto> tiposBoletoMap = tiposBoleto.stream()
            .collect(Collectors.toMap(
                    TipoBoleto::getId,
                    tipoBoleto -> tipoBoleto
            ));

        Compra compra = new Compra();
        compra.setUsuario(usuario);
        compra.setEstado(EstadoCompra.RESERVADA);
        compra.setFechaExpiracionReserva(LocalDateTime.now().plusMinutes(RESERVA_MINUTOS));
        compra.setTotal(BigDecimal.ZERO);

        BigDecimal totalCompra = BigDecimal.ZERO;

        for (Map.Entry<Long, List<BoletoReservaRequest>> entry : boletosAgrupados.entrySet()) {

            Long tipoBoletoId = entry.getKey();

            List<BoletoReservaRequest> boletosSolicitados = entry.getValue();

            TipoBoleto tipoBoleto = tiposBoletoMap.get(tipoBoletoId);

            if (tipoBoleto == null) {
                throw new TipoBoletoNoEncontradoException();
            }

            if(!tipoBoleto.getEvento().getId().equals(evento.getId())){
                throw new TipoBoletoNoPerteneceAlEventoException();
            }

            Long ocupados = detalleCompraRepository.obtenerBoletosOcupados(tipoBoleto.getId());

            int disponibles = tipoBoleto.getCantidadTotal() - ocupados.intValue();

            // Cantidad de titulares por boleto
            // 1 = {titular1, titular2} -> cantidadSolicitada = 2
            int cantidadSolicitada = boletosSolicitados.size();

            if (cantidadSolicitada > disponibles) {
                throw new InsuficienciaDeBoletosException(tipoBoleto.getNombre());
            }

            DetalleCompra detalleCompra = new DetalleCompra();
            detalleCompra.setCompra(compra);
            detalleCompra.setTipoBoleto(tipoBoleto);
            detalleCompra.setCantidad(cantidadSolicitada);
            detalleCompra.setPrecioUnitario(tipoBoleto.getPrecio());

            BigDecimal subtotal = tipoBoleto.getPrecio().multiply(BigDecimal.valueOf(cantidadSolicitada));

            detalleCompra.setSubtotal(subtotal);

            for(BoletoReservaRequest boletoRequest : boletosSolicitados) {

                Boleto boleto = new Boleto();

                boleto.setDetalleCompra(detalleCompra);
                boleto.setTitularNombre(boletoRequest.getTitular());

                detalleCompra.getBoletos().add(boleto);
            }

            compra.getDetalles().add(detalleCompra);

            totalCompra = totalCompra.add(subtotal);
        }

        compra.setTotal(totalCompra);

        Compra guardada = compraRepository.save(compra);

        return new ReservaResponse(
            guardada.getId(),
            guardada.getTotal(),
            guardada.getFechaExpiracionReserva()
        );
    }

    @Override
    // Los throws de negocio al final del reuse (ya-pagada / expirada)
    // no deben revertir la reconciliación que se acabó de guardar.
    @Transactional(noRollbackFor = {ReservaExpiradaException.class, CompraYaPagadaException.class})
    public CheckoutResponse pagar(Long compraId) {

        Usuario usuario = currentUserService.obtenerUsuarioActual();

        // Se gurada la compra 'reservada'
        Compra compra = compraRepository.findByIdAndUsuarioId(compraId, usuario.getId())
            .orElseThrow(() -> new CompraNoEncontradaException());

        if (compra.getEstado() == EstadoCompra.PAGADA) {
            throw new CompraYaPagadaException();
        }

        if(compra.getEstado() != EstadoCompra.RESERVADA){
            throw new CompraNoReservadaException();
        }

        if(compra.getFechaExpiracionReserva().isBefore(LocalDateTime.now())){
            throw new ReservaExpiradaException();
        }

        var pagoPendiente = pagoRepository.findFirstByCompraIdAndProveedorAndEstadoOrderByFechaCreacionDesc(
            compraId,
            "STRIPE",
            EstadoPago.PENDIENTE
        );

        if (pagoPendiente.isPresent()) {
            Pago pago = pagoPendiente.get();

            if (pago.getUrlCheckout() != null) {
                final Session vigente;
                try {
                    vigente = stripeClient.v1().checkout().sessions().retrieve(pago.getIdPagoExterno());
                } catch (StripeException e) {
                    throw new StripeNoDisponibleException("No fue posible validar la sesión de pago con Stripe.", e);
                }

                if ("open".equals(vigente.getStatus())) {
                    return new CheckoutResponse(
                        compra.getId(),
                        pago.getEstado().name(),
                        pago.getUrlCheckout()
                    );
                }

                // La sesión ya no está abierta: reconciliar y decidir.
                // Nunca se devuelve una URL muerta al frontend.
                stripeWebhookService.procesarSesionReconciliada(vigente);

                Compra actual = compraRepository.findByIdAndUsuarioId(compraId, usuario.getId())
                    .orElseThrow(() -> new CompraNoEncontradaException());

                if (actual.getEstado() == EstadoCompra.PAGADA) {
                    throw new CompraYaPagadaException();
                }

                if (actual.getFechaExpiracionReserva().isBefore(LocalDateTime.now())) {
                    throw new ReservaExpiradaException();
                }
                // Sesión muerta pero reserva vigente: se sigue al flujo de
                // creación y se genera una sesión nueva (el pago viejo quedó
                // EXPIRADO y ya no estorba).
            }
        }

        try {
            List<DetalleCompra> detalles = detalleCompraRepository.findByCompraId(compra.getId());

            if (detalles.isEmpty()) {
                throw new IllegalStateException("La compra no contiene detalles.");
            }

            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .addAllowedPaymentMethodType(SessionCreateParams.AllowedPaymentMethodType.CARD)
                    .setClientReferenceId(compra.getId().toString())
                    .putMetadata(
                        "compraId",
                        compra.getId().toString()
                    )
                    .setSuccessUrl(
                        frontendUrl
                            + "/index.html?payment=success"
                            + "&compra_id=" + compra.getId()
                            + "&session_id={CHECKOUT_SESSION_ID}"
                    )
                    .setCancelUrl(
                        frontendUrl
                            + "/index.html?payment=cancelled"
                            + "&compra_id=" + compra.getId()
                    )
                    .setExpiresAt(
                        Instant.now()
                            .plus(STRIPE_SESION_MINUTOS, ChronoUnit.MINUTES)
                            .getEpochSecond()
                    );

            for (DetalleCompra detalle : detalles) {

                long cantidad = detalle.getCantidad().longValue();

                long precioEnCentavos = detalle
                    .getPrecioUnitario()
                    .movePointRight(2)
                    .longValueExact();

                if (cantidad <= 0 || precioEnCentavos < 0) {
                    throw new IllegalStateException("La compra contiene cantidades o precios inválidos.");
                }

                SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                        .setQuantity(cantidad)
                        .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(stripeCurrency.toLowerCase())
                                .setUnitAmount(precioEnCentavos)
                                .setProductData(
                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(
                                            detalle.getTipoBoleto().getNombre()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build();

                paramsBuilder.addLineItem(lineItem);
            }

            Session session = stripeClient.v1().checkout().sessions().create(paramsBuilder.build());

            if (session.getUrl() == null) {
                throw new IllegalStateException( "Stripe no devolvió una URL de checkout.");
            }

            Pago pago = Pago.builder()
                .compra(compra)
                .proveedor("STRIPE")
                .idPagoExterno(session.getId())
                .urlCheckout(session.getUrl())
                .monto(compra.getTotal())
                .estado(EstadoPago.PENDIENTE)
                .build();

            pagoRepository.save(pago);

            compra.setFechaExpiracionReserva(LocalDateTime.now().plusMinutes(STRIPE_SESION_MINUTOS));

            compraRepository.save(compra);

            return new CheckoutResponse(compra.getId(), pago.getEstado().name(), pago.getUrlCheckout());
        
        } catch (StripeException e) {
            throw new IllegalStateException("No fue posible iniciar el checkout con Stripe.", e);
        }
    }

    @Override
    @Transactional
    public CompraDetalleResponse verificarPago(Long compraId) {

        Usuario usuario = currentUserService.obtenerUsuarioActual();

        compraRepository.findByIdAndUsuarioId(compraId, usuario.getId())
            .orElseThrow(() -> new CompraNoEncontradaException());

        pagoRepository.findFirstByCompraIdAndProveedorAndEstadoOrderByFechaCreacionDesc(
            compraId,
            "STRIPE",
            EstadoPago.PENDIENTE
        ).ifPresent(pago -> {
            try {
                Session session = stripeClient.v1().checkout().sessions().retrieve(pago.getIdPagoExterno());
                stripeWebhookService.procesarSesionReconciliada(session);
            } catch (StripeException e) {
                throw new StripeNoDisponibleException("No fue posible verificar el pago con Stripe.", e);
            }
        });

        return obtenerCompraPorId(compraId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompraResumenResponse> obtenerCompras() {

        Usuario usuario = currentUserService.obtenerUsuarioActual();

        List<Compra> compras = compraRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuario.getId());

        return compras.stream()
            .map(compra -> {

                DetalleCompra primerDetalle = compra.getDetalles()
                    .stream()
                    .findFirst()
                    .orElse(null);

                String nombreEvento = primerDetalle != null
                    ? primerDetalle.getTipoBoleto().getEvento().getNombre()
                    : null;

                Long eventoId = primerDetalle != null
                    ? primerDetalle.getTipoBoleto().getEvento().getId()
                    : null;

                int cantidadBoletos = compra.getDetalles()
                    .stream()
                    .mapToInt(DetalleCompra::getCantidad)
                    .sum();


                return new CompraResumenResponse(
                compra.getId(),
                nombreEvento,
                eventoId,
                compra.getEstado(),
                cantidadBoletos,
                compra.getTotal(),
                compra.getFechaCreacion(),
                compra.getFechaExpiracionReserva()
                );
            })
            .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public CompraDetalleResponse obtenerCompraPorId(Long compraId) {

        Usuario usuario = currentUserService.obtenerUsuarioActual();

        Compra compra = compraRepository.buscarMiCompra(compraId, usuario.getId())
            .orElseThrow(() -> new CompraNoEncontradaException());

        String nombreEvento = compra.getDetalles().stream()
            .map(detalle -> detalle.getTipoBoleto().getEvento().getNombre())
            .findFirst()
            .orElse(null);

        Long eventoId = compra.getDetalles().stream()
            .map(detalle -> detalle.getTipoBoleto().getEvento().getId())
            .findFirst()
            .orElse(null);

        int cantidadBoletos = compra.getDetalles().stream()
            .mapToInt(DetalleCompra::getCantidad)
            .sum();

        List<BoletoDetalleResponse> boletos = compra.getDetalles().stream()
            .flatMap(detalle -> detalle.getBoletos().stream()
                .map(boleto -> new BoletoDetalleResponse(
                    boleto.getId(),
                    detalle.getTipoBoleto().getNombre(),
                    boleto.getTitularNombre(),
                    boleto.getFolio(),
                    boleto.getEstado(),
                    esDescargable(compra, detalle.getTipoBoleto().getEvento())
                )))
            .toList();

        return new CompraDetalleResponse(
            compra.getId(),
            nombreEvento,
            eventoId,
            compra.getEstado(),
            cantidadBoletos,
            compra.getTotal(),
            compra.getFechaCreacion(),
            compra.getFechaExpiracionReserva(),
            boletos
        );
    }


    /*
     * Replica las reglas de BoletoServiceImpl.generarPdf sin lanzar:
     * compra pagada y evento vigente (no cancelado/finalizado).
     */
    private boolean esDescargable(Compra compra, Evento evento) {

        if (compra.getEstado() != EstadoCompra.PAGADA) {
            return false;
        }

        if (evento.getEstado() == EstadoEvento.CANCELADO) {
            return false;
        }

        if (evento.getEstado() == EstadoEvento.FINALIZADO || evento.getFechaFin().isBefore(LocalDateTime.now())) {
            return false;
        }

        return true;
    }
    
    
}
