package com.aldo.event_pass.service.implementaciones;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aldo.event_pass.dto.boleto.BoletoResponse;
import com.aldo.event_pass.dto.boleto.UsarBoletoResponse;
import com.aldo.event_pass.entity.Boleto;
import com.aldo.event_pass.entity.Compra;
import com.aldo.event_pass.entity.Evento;
import com.aldo.event_pass.entity.TipoBoleto;
import com.aldo.event_pass.entity.Usuario;
import com.aldo.event_pass.enums.EstadoBoleto;
import com.aldo.event_pass.exception.BoletoNoEncontradoException;
import com.aldo.event_pass.exception.PermisoDenegadoParaGenerarBoletoException;
import com.aldo.event_pass.repository.BoletoRepository;
import com.aldo.event_pass.security.CurrentUserService;
import com.aldo.event_pass.service.interfaces.BoletoService;
import com.aldo.event_pass.service.interfaces.PdfService;
import com.aldo.event_pass.support.BoletoValidezResolver;

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

        boolean valido = BoletoValidezResolver.esValido(
                boleto.getDetalleCompra().getCompra(),
                evento,
                boleto.getEstado()
        );

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

        Compra compra = boleto.getDetalleCompra().getCompra();

        Evento evento = boleto.getDetalleCompra().getTipoBoleto().getEvento();

        BoletoValidezResolver.validar(compra, evento, boleto.getEstado());

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
    public byte[] generarPdf(Long boletoId) {
        
        Usuario usuario = currentUserService.obtenerUsuarioActual();

        Boleto boleto = boletoRepository.findByIdForPdf(boletoId)
            .orElseThrow(() -> new BoletoNoEncontradoException());

        Compra compra = boleto.getDetalleCompra().getCompra();

        Evento evento = boleto.getDetalleCompra().getTipoBoleto().getEvento();

        if (!compra.getUsuario().getId().equals(usuario.getId())) {
            throw new PermisoDenegadoParaGenerarBoletoException();
        }

        BoletoValidezResolver.validar(compra, evento, boleto.getEstado());

        return pdfService.generarPdf(boleto);
    }

}
