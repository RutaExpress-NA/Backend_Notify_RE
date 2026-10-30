package com.na.rutaexpress.notify.document;

import java.io.ByteArrayOutputStream;

import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

// Convierte HTML (XHTML bien formado) en PDF con Flying Saucer.


@Service
public class PdfService {

    public byte[] htmlToPdf(String xhtml) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(xhtml);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF: " + e.getMessage(), e);
        }
    }
}
