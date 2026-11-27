package com.aldo.event_pass.service.implementaciones;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
import com.aldo.event_pass.exception.CompraInvalidaException;
import com.aldo.event_pass.exception.CompraNoEncontradaException;
import com.aldo.event_pass.exception.CompraNoReservadaException;
import com.aldo.event_pass.exception.CompraVaciaException;
import com.aldo.event_pass.exception.CompraYaPagadaException;
import com.aldo.event_pass.exception.EventoIniciadoException;
import com.aldo.event_pass.exception.EventoNoEncontradoException;
import com.aldo.event_pass.exception.ExcederMaximosBoletosPorUsuarioException;
import com.aldo.event_pass.exception.InsuficienciaDeBoletosException;
import com.aldo.event_pass.exception.NoFuePosibleIniciarCheckoutException;
import com.aldo.event_pass.exception.ReservaExpiradaException;
import com.aldo.event_pass.exception.SesionDeCheckoutNoDevueltaException;
import com.aldo.event_pass.exception.ReservarBoletosEnEventosPublicadosException;
import com.aldo.event_pass.exception.StripeNoDisponibleException;
import com.aldo.event_pass.exception.TipoBoletoInexistenteException;
import com.aldo.event_pass.exception.UsuarioNoEncontradoException;
import com.aldo.event_pass.support.BoletoValidezResolver;
import com.aldo.event_pass.support.EventoEstadoResolver;
import com.aldo.event_pass.repository.BoletoRepository;
import com.aldo.event_pass.repository.CompraRepository;
import com.aldo.event_pass.repository.DetalleCompraRepository;
import com.aldo.event_pass.repository.EventoRepository;
import com.aldo.event_pass.repository.PagoRepository;
import com.aldo.event_pass.repository.TipoBoletoRepository;
import com.aldo.event_pass.repository.UsuarioRepository;
import com.aldo.event_pass.security.CurrentUserService;
import com.aldo.event_pass.service.interfaces.CompraService;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompraServiceImpl implements CompraService {

    private final CurrentUserService currentUserService;
    private final EventoRepository eventoRepository;
    private final BoletoRepository boletoRepository;
    private final TipoBoletoRepository tipoBoletoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DetalleCompraRepository detalleCompraRepository;
    private final CompraRepository compraRepository;
    private final PagoRepository pagoRepository;
    private final StripeClient stripeClient;
    private final StripeWebhookService stripeWebhookService;
    private final PagoTransaccionService pagoTransaccionService;

    private static final int RESERVA_MINUTOS = 15;
    private static final int STRIPE_SESION_MINUTOS = 30;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.stripe.currency:mxn}")
    private String stripeCurrency;

    @Override
    @Transactional 
    public ReservaResponse reservarBoletos(ReservarBoletosRequest reservarBoletosRequest) {

        Usuario usuarioActual = currentUserService.obtenerUsuarioActual();

        Evento evento = eventoRepository.findById(reservarBoletosRequest.getEventoId())
            .orElseThrow(() -> new EventoNoEncontradoException());

        EstadoEvento estadoEfectivo = EventoEstadoResolver.estadoEfectivo(evento);
        
        if (estadoEfectivo == EstadoEvento.INICIADO) {
            throw new EventoIniciadoException();
        }
        
        if (estadoEfectivo != EstadoEvento.PUBLICADO) {
            throw new ReservarBoletosEnEventosPublicadosException();
        }

        // 1 = {titular1, titular2},
        // 2 = {titular3}
        Map<Long, List<BoletoReservaRequest>> boletosAgrupados = reservarBoletosRequest.getBoletos().stream()
            .collect(Collectors.groupingBy(
                BoletoReservaRequest::getTipoBoletoId
            ));

        // Ordena los ids para adquirir los locks siempre en el mismo orden
        List<Long> ids = boletosAgrupados.keySet().stream().sorted().toList();

        // Serializa las reservas del mismo usuario antes de contar sus boletos
        Usuario usuario = usuarioRepository.findByIdForUpdate(usuarioActual.getId())
            .orElseThrow(() -> new UsuarioNoEncontradoException());

        List<TipoBoleto> tiposBoleto = new ArrayList<>();

        for (Long id : ids) {
            TipoBoleto tipoBoleto = tipoBoletoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new TipoBoletoInexistenteException());

            tiposBoleto.add(tipoBoleto);
        }

        Map<Long, TipoBoleto> tiposBoletoMap = tiposBoleto.stream()
            .collect(Collectors.toMap(
                    TipoBoleto::getId,
                    tipoBoleto -> tipoBoleto
            ));

        // Revalida el máximo después de los locks para que el conteo sea estable
        Long boletosActuales = boletoRepository.contarBoletosUsuarioPorEvento(usuario.getId(), evento.getId());

        int boletosNuevos = reservarBoletosRequest.getBoletos().size();

        if((boletosActuales + boletosNuevos) > evento.getMaxBoletosPorUsuario()){
            throw new ExcederMaximosBoletosPorUsuarioException();
        }

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
    public CheckoutResponse pagar(Long compraId) {

        Usuario usuario = currentUserService.obtenerUsuarioActual();

        Compra compra = compraRepository.findParaCheckout(compraId, usuario.getId())
            .orElseThrow(() -> new CompraNoEncontradaException());

        validarReserva(compra);

        var pagoPendiente = pagoRepository.findFirstByCompraIdAndProveedorAndEstadoOrderByFechaCreacionDesc(
            compraId,
            "STRIPE",
            EstadoPago.PENDIENTE
        );

        if (pagoPendiente.isPresent() && pagoPendiente.get().getUrlCheckout() != null) {

            Pago pago = pagoPendiente.get();
            Session vigente = obtenerSesion(pago.getIdPagoExterno());

            if ("open".equals(vigente.getStatus())) {
                return new CheckoutResponse(
                    compra.getId(),
                    pago.getEstado().name(),
                    pago.getUrlCheckout()
                );
            }

            stripeWebhookService.procesarSesionReconciliada(vigente);

            Compra actual = compraRepository.findParaCheckout(compraId, usuario.getId())
                .orElseThrow(() -> new CompraNoEncontradaException());

            validarTrasReconciliar(actual);
        }

        if (compra.getDetalles().isEmpty()) {
            throw new CompraVaciaException();
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

        for (DetalleCompra detalle : compra.getDetalles()) {

            long cantidad = detalle.getCantidad().longValue();

            long precioEnCentavos = detalle
                .getPrecioUnitario()
                .movePointRight(2)
                .longValueExact();

            if (cantidad <= 0 || precioEnCentavos < 0) {
                throw new CompraInvalidaException();
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

        final Session session;

        try {
            session = stripeClient.v1().checkout().sessions().create(paramsBuilder.build());
        } catch (StripeException e) {
            throw new NoFuePosibleIniciarCheckoutException(e);
        }

        if (session.getUrl() == null) {
            throw new SesionDeCheckoutNoDevueltaException();
        }

        try {
            return pagoTransaccionService.registrarSesion(
                compraId,
                usuario.getId(),
                session,
                STRIPE_SESION_MINUTOS
            );
        } catch (CompraNoEncontradaException | CompraNoReservadaException
                | CompraYaPagadaException | ReservaExpiradaException ex) {
            expirarSesionStripe(session.getId());
            throw ex;
        }
    }

    @Override
    public CompraDetalleResponse verificarPago(Long compraId) {

        Usuario usuario = currentUserService.obtenerUsuarioActual();

        compraRepository.buscarMiCompra(compraId, usuario.getId())
            .orElseThrow(() -> new CompraNoEncontradaException());

        pagoRepository.findFirstByCompraIdAndProveedorAndEstadoOrderByFechaCreacionDesc(
            compraId,
            "STRIPE",
            EstadoPago.PENDIENTE
        ).ifPresent(pago -> {
            Session session = obtenerSesion(pago.getIdPagoExterno());
            stripeWebhookService.procesarSesionReconciliada(session);
        });

        return obtenerCompraPorId(compraId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompraResumenResponse> obtenerCompras() {

        Usuario usuario = currentUserService.obtenerUsuarioActual();

        return compraRepository.findResumenByUsuarioId(usuario.getId())
            .stream()
            .map(resumen -> new CompraResumenResponse(
                resumen.getCompraId(),
                resumen.getNombreEvento(),
                resumen.getEventoId(),
                resumen.getEstado(),
                resumen.getCantidadBoletos().intValue(),
                resumen.getTotal(),
                resumen.getFechaReserva(),
                resumen.getFechaExpiracion()
            ))
            .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public CompraDetalleResponse obtenerCompraPorId(Long compraId) {

        Usuario usuario = currentUserService.obtenerUsuarioActual();

        Compra compra = compraRepository.buscarMiCompraConDetalles(compraId, usuario.getId())
            .orElseThrow(() -> new CompraNoEncontradaException());

        Evento evento = compra.getDetalles().stream()
            .map(detalle -> detalle.getTipoBoleto().getEvento())
            .findFirst()
            .orElse(null);

        String nombreEvento = evento != null ? evento.getNombre() : null;
        Long eventoId = evento != null ? evento.getId() : null;

        int cantidadBoletos = compra.getDetalles().stream()
            .mapToInt(DetalleCompra::getCantidad)
            .sum();

        List<BoletoDetalleResponse> boletos = boletoRepository.findDetallesByCompraId(compraId)
            .stream()
            .map(boleto -> new BoletoDetalleResponse(
                boleto.getBoletoId(),
                boleto.getTipoBoletoNombre(),
                boleto.getTitularNombre(),
                boleto.getFolio(),
                boleto.getEstado(),
                BoletoValidezResolver.esValido(compra, evento, boleto.getEstado())
            ))
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


    private Session obtenerSesion(String idPagoExterno) {

        try {
            return stripeClient.v1().checkout().sessions().retrieve(idPagoExterno);
        } catch (StripeException e) {
            throw new StripeNoDisponibleException("No fue posible validar la sesión de pago con Stripe.", e);
        }
    }

    private void expirarSesionStripe(String sessionId) {

        try {
            stripeClient.v1().checkout().sessions().expire(sessionId);
        } catch (StripeException e) {
            log.warn("No se pudo expirar la sesión de Stripe {} de una compra ya inválida.", sessionId, e);
        }
    }

    private void validarReserva(Compra compra) {

        if (compra.getEstado() == EstadoCompra.PAGADA) {
            throw new CompraYaPagadaException();
        }

        if (compra.getEstado() != EstadoCompra.RESERVADA) {
            throw new CompraNoReservadaException();
        }

        if (compra.getFechaExpiracionReserva().isBefore(LocalDateTime.now())) {
            throw new ReservaExpiradaException();
        }
    }

    private void validarTrasReconciliar(Compra compra) {

        if (compra.getEstado() == EstadoCompra.PAGADA) {
            throw new CompraYaPagadaException();
        }

        if (compra.getFechaExpiracionReserva().isBefore(LocalDateTime.now())) {
            throw new ReservaExpiradaException();
        }
    }

}
