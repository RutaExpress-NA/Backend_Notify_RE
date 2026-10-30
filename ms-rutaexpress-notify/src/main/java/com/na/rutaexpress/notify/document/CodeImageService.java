package com.na.rutaexpress.notify.document;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;

//Genera codigo QR y codigo de barras (Code 128) con ZXing devuelve la imagen como "data URI" para incrustarla directo en el HTML
@Service
public class CodeImageService {

    public String qrDataUri(String text, int size) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(
                    text, BarcodeFormat.QR_CODE, size, size, Map.of(EncodeHintType.MARGIN, 1));
            return toDataUri(matrix);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el QR: " + e.getMessage(), e);
        }
    }

    public String barcodeDataUri(String text, int width, int height) {
        try {
            BitMatrix matrix = new Code128Writer().encode(text, BarcodeFormat.CODE_128, width, height);
            return toDataUri(matrix);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el codigo de barras: " + e.getMessage(), e);
        }
    }

    private String toDataUri(BitMatrix matrix) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        }
    }
}
