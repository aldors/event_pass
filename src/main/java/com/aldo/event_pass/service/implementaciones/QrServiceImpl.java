package com.aldo.event_pass.service.implementaciones;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.stereotype.Service;

import com.aldo.event_pass.service.interfaces.QrService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

@Service
public class QrServiceImpl implements QrService {
    
    @Override
    public byte[] generarQr(String contenido) {

        try {

            BitMatrix matrix = new MultiFormatWriter().encode(
                    contenido,
                    BarcodeFormat.QR_CODE,
                    300,
                    300
                );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            MatrixToImageWriter.writeToStream(
                    matrix,
                    "PNG",
                    outputStream
            );

            return outputStream.toByteArray();

        } catch (WriterException | IOException e) {
            throw new RuntimeException("Error al generar el código QR", e);
        }
    }
    
}
