package com.aldo.event_pass.service.implementaciones;

import java.io.ByteArrayOutputStream;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aldo.event_pass.entity.Boleto;
import com.aldo.event_pass.entity.Evento;
import com.aldo.event_pass.entity.TipoBoleto;
import com.aldo.event_pass.exception.GeneracionPdfException;
import com.aldo.event_pass.service.interfaces.PdfService;
import com.aldo.event_pass.service.interfaces.QrService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PdfServiceImpl implements PdfService {

    private final QrService qrService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    public byte[] generarPdf(Boleto boleto) {
        
        try {

            Evento evento = boleto.getDetalleCompra().getTipoBoleto().getEvento();
            
            TipoBoleto tipoBoleto = boleto.getDetalleCompra().getTipoBoleto();

            String urlVerificacion = baseUrl + "/boletos/verificar/" + boleto.getCodigoQr();

            byte[] qrBytes = qrService.generarQr(urlVerificacion);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Document document = new Document(
                            PageSize.A4,
                            50,
                            50,
                            50,
                            50
                    );

            PdfWriter.getInstance(document, outputStream);

            document.open();

            // -------------------------------------------------
            // Fuentes
            // -------------------------------------------------

            Font titulo = FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            24
                        );

            Font subtitulo = FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            16
                        );

            Font texto = FontFactory.getFont(
                            FontFactory.HELVETICA,
                            12
                        );

            Font textoNegrita = FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            12
                        );

            // -------------------------------------------------
            // Título
            // -------------------------------------------------

            Paragraph nombreEvento = new Paragraph(
                            evento.getNombre(),
                            titulo
                        );

            nombreEvento.setAlignment(Element.ALIGN_CENTER);

            document.add(nombreEvento);

            document.add(new Paragraph(" "));

            // -------------------------------------------------
            // Información del boleto
            // -------------------------------------------------

            Paragraph tituloBoleto = new Paragraph(
                            "BOLETO DIGITAL",
                            subtitulo
                    );

            tituloBoleto.setAlignment(Element.ALIGN_CENTER);

            document.add(tituloBoleto);

            document.add(new Paragraph(" "));

            // -------------------------------------------------
            // Tabla de información
            // -------------------------------------------------

            PdfPTable tabla = new PdfPTable(2);

            tabla.setWidthPercentage(100);

            tabla.setSpacingBefore(20);

            tabla.addCell(new Phrase("Titular", textoNegrita));
            tabla.addCell(new Phrase(boleto.getTitularNombre(),texto));

            tabla.addCell(new Phrase("Tipo de boleto", textoNegrita));
            tabla.addCell(new Phrase(tipoBoleto.getNombre(),texto));

            tabla.addCell(new Phrase("Folio",textoNegrita));
            tabla.addCell(new Phrase(boleto.getFolio(), texto));

            tabla.addCell(new Phrase("Fecha de inicio", textoNegrita));
            tabla.addCell(new Phrase(evento.getFechaInicio().toString(), texto));

            tabla.addCell(new Phrase("Fecha de finalización", textoNegrita));
            tabla.addCell(new Phrase(evento.getFechaFin().toString(), texto));

            tabla.addCell(new Phrase("Ubicación", textoNegrita));
            tabla.addCell(new Phrase(evento.getUbicacion(), texto));

            tabla.addCell(new Phrase("Estado", textoNegrita));
            tabla.addCell(new Phrase(boleto.getEstado().name(), texto));

            document.add(tabla);

            // -------------------------------------------------
            // QR
            // -------------------------------------------------

            document.add(new Paragraph(" "));

            Paragraph textoQr = new Paragraph(
                            "Escanea este código para verificar tu boleto",
                            texto
                    );

            textoQr.setAlignment(Element.ALIGN_CENTER);

            document.add(textoQr);

            Image qr = Image.getInstance(qrBytes);

            qr.scaleToFit(200, 200);

            qr.setAlignment(Element.ALIGN_CENTER);

            document.add(qr);

            // -------------------------------------------------
            // Información adicional
            // -------------------------------------------------

            document.add( new Paragraph(" "));

            Paragraph aviso = new Paragraph(
                            "Presenta este boleto al ingresar al evento.",
                            texto
                    );

            aviso.setAlignment(Element.ALIGN_CENTER);

            document.add(aviso);

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new GeneracionPdfException(e);
        }
    }

}
