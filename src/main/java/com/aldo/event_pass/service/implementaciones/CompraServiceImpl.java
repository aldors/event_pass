package com.aldo.event_pass.service.implementaciones;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aldo.event_pass.dto.reservacion.BoletoReservaRequest;
import com.aldo.event_pass.dto.reservacion.ReservaResponse;
import com.aldo.event_pass.dto.reservacion.ReservarBoletosRequest;
import com.aldo.event_pass.entity.Boleto;
import com.aldo.event_pass.entity.Compra;
import com.aldo.event_pass.entity.DetalleCompra;
import com.aldo.event_pass.entity.Evento;
import com.aldo.event_pass.entity.TipoBoleto;
import com.aldo.event_pass.entity.Usuario;
import com.aldo.event_pass.enums.EstadoCompra;
import com.aldo.event_pass.enums.EstadoEvento;
import com.aldo.event_pass.repository.BoletoRepository;
import com.aldo.event_pass.repository.CompraRepository;
import com.aldo.event_pass.repository.DetalleCompraRepository;
import com.aldo.event_pass.repository.EventoRepository;
import com.aldo.event_pass.repository.TipoBoletoRepository;
import com.aldo.event_pass.security.CurrentUserService;
import com.aldo.event_pass.service.interfaces.CompraService;

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

    @Override
    @Transactional 
    public ReservaResponse reservarBoletos(ReservarBoletosRequest reservarBoletosRequest) {

        Usuario usuario = currentUserService.obtenerUsuarioActual();

        Evento evento = eventoRepository.findById(reservarBoletosRequest.getEventoId())
            .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        if(evento.getEstado() != EstadoEvento.PUBLICADO){
            throw new RuntimeException("Solo puedes reservar boletos de eventos publicados");
        }

        if(!evento.getFechaInicio().isAfter(LocalDateTime.now())){
            throw new RuntimeException("No puedes reservar boletos de un evento que ya inició");
        }

        // Boletos que ya ha comprado este usuario en este evento
        Long boletosActuales = boletoRepository.contarBoletosUsuarioPorEvento(usuario.getId(), evento.getId());

        int boletosNuevos = reservarBoletosRequest.getBoletos().size();

        if((boletosActuales + boletosNuevos) > evento.getMaxBoletosPorUsuario()){
            throw new RuntimeException("No puedes exceder el limite de compra de boletos por usuario");
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
            throw new RuntimeException("Uno o más tipos de boleto no existen");
        }

        Map<Long, TipoBoleto> tiposBoletoMap = tiposBoleto.stream()
            .collect(Collectors.toMap(
                    TipoBoleto::getId,
                    tipoBoleto -> tipoBoleto
            ));

        Compra compra = new Compra();
        compra.setUsuario(usuario);
        compra.setEstado(EstadoCompra.RESERVADA);
        compra.setFechaExpiracionReserva(LocalDateTime.now().plusMinutes(15));
        compra.setTotal(BigDecimal.ZERO);

        BigDecimal totalCompra = BigDecimal.ZERO;

        for (Map.Entry<Long, List<BoletoReservaRequest>> entry : boletosAgrupados.entrySet()) {

            Long tipoBoletoId = entry.getKey();

            List<BoletoReservaRequest> boletosSolicitados = entry.getValue();

            TipoBoleto tipoBoleto = tiposBoletoMap.get(tipoBoletoId);

            if (tipoBoleto == null) {
                throw new RuntimeException("Tipo de boleto no encontrado");
            }

            if(!tipoBoleto.getEvento().getId().equals(evento.getId())){
                throw new RuntimeException("El tipo de boleto no pertenece al evento");
            }

            Integer ocupados = detalleCompraRepository.obtenerBoletosOcupados(tipoBoleto.getId());

            int disponibles = tipoBoleto.getCantidadTotal() - ocupados;

            // Cantidad de titulares por boleto
            // 1 = {titular1, titular2} -> cantidadSolicitada = 2
            int cantidadSolicitada = boletosSolicitados.size();

            if (cantidadSolicitada > disponibles) {
                throw new RuntimeException("No hay suficientes boletos disponibles para: " + tipoBoleto.getNombre());
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
    
}
