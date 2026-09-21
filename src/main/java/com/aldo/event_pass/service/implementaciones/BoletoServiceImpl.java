package com.aldo.event_pass.service.implementaciones;


import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aldo.event_pass.dto.boleto.BoletoResponse;
import com.aldo.event_pass.dto.boleto.UsarBoletoResponse;
import com.aldo.event_pass.entity.Boleto;
import com.aldo.event_pass.entity.Evento;
import com.aldo.event_pass.entity.TipoBoleto;
import com.aldo.event_pass.entity.Usuario;
import com.aldo.event_pass.enums.EstadoBoleto;
import com.aldo.event_pass.repository.BoletoRepository;
import com.aldo.event_pass.security.CurrentUserService;
import com.aldo.event_pass.service.interfaces.BoletoService;
import com.aldo.event_pass.service.interfaces.PdfService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoletoServiceImpl implements BoletoService {

    private final BoletoRepository boletoRepository;
    private final PdfService pdfService;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public BoletoResponse verificarBoleto(String codigoQr) {
        
        Boleto boleto = boletoRepository.findByCodigoQr(codigoQr)
            .orElseThrow(() -> new RuntimeException("Boleto no encontrado"));

        Evento evento = boleto.getDetalleCompra().getTipoBoleto().getEvento();

        TipoBoleto tipoBoleto = boleto.getDetalleCompra().getTipoBoleto();

        boolean valido = boleto.getEstado() == EstadoBoleto.ACTIVO;

        return new BoletoResponse(
                evento.getNombre(),
                tipoBoleto.getNombre(),
                boleto.getTitularNombre(),
                boleto.getFolio(),
                boleto.getEstado(),
                valido
        );
    }

    @Override
    @Transactional
    public UsarBoletoResponse usarBoleto(String codigoQr) {
        
        Boleto boleto = boletoRepository.findByCodigoQr(codigoQr)
            .orElseThrow(() -> new RuntimeException("Boleto no encontrado"));

        if (boleto.getEstado() == EstadoBoleto.UTILIZADO) {
            throw new RuntimeException("Este boleto ya fue utilizado");
        }

        if (boleto.getEstado() == EstadoBoleto.INVALIDADO) {
            throw new RuntimeException("Este boleto fue invalidado");
        }

        Evento evento = boleto.getDetalleCompra().getTipoBoleto().getEvento();

        if (evento.getFechaFin().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("El evento ya finalizó");
        }

        boleto.setEstado(EstadoBoleto.UTILIZADO);

        boletoRepository.save(boleto);

        return new UsarBoletoResponse(
                evento.getNombre(),
                boleto.getTitularNombre(),
                boleto.getFolio(),
                boleto.getEstado(),
                "Acceso permitido"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarPdf(Long boletoId) {
        
        Usuario usuario = currentUserService.obtenerUsuarioActual();

        Boleto boleto = boletoRepository.findById(boletoId)
            .orElseThrow(() -> new RuntimeException("Boleto no encontrado"));

        Long propietarioId = boleto.getDetalleCompra().getCompra().getUsuario().getId();

        if (!propietarioId.equals(usuario.getId())) {
            throw new RuntimeException("No tienes permiso para descargar este boleto");
        }

        return pdfService.generarPdf(boleto);
    }
    
}
