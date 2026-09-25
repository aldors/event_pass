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
import com.aldo.event_pass.exception.BoletoInvalidadoException;
import com.aldo.event_pass.exception.BoletoNoEncontradoException;
import com.aldo.event_pass.exception.BoletoUtilizadoException;
import com.aldo.event_pass.exception.EventoFinalizadoException;
import com.aldo.event_pass.exception.PermisoDenegadoParaGenerarBoletoException;
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
            .orElseThrow(() -> new BoletoNoEncontradoException());

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
            .orElseThrow(() -> new BoletoNoEncontradoException());

        if (boleto.getEstado() == EstadoBoleto.UTILIZADO) {
            throw new BoletoUtilizadoException();
        }

        if (boleto.getEstado() == EstadoBoleto.INVALIDADO) {
            throw new BoletoInvalidadoException();
        }

        Evento evento = boleto.getDetalleCompra().getTipoBoleto().getEvento();

        if (evento.getFechaFin().isBefore(LocalDateTime.now())) {
            throw new EventoFinalizadoException();
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
            .orElseThrow(() -> new BoletoNoEncontradoException());

        Long propietarioId = boleto.getDetalleCompra().getCompra().getUsuario().getId();

        if (!propietarioId.equals(usuario.getId())) {
            throw new PermisoDenegadoParaGenerarBoletoException();
        }

        return pdfService.generarPdf(boleto);
    }
    
}
