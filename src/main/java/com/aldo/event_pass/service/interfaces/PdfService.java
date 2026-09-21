package com.aldo.event_pass.service.interfaces;

import com.aldo.event_pass.entity.Boleto;

public interface PdfService {
    
    public byte[] generarPdf(Boleto boleto);
}
