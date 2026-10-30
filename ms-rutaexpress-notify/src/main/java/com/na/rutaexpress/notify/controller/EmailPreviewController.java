package com.na.rutaexpress.notify.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.na.rutaexpress.notify.email.EmailService;

import lombok.RequiredArgsConstructor;

// Permite ver en el navegador el correo que se genero para un envio 
@RestController
@RequestMapping("/api/notify/emails")
@RequiredArgsConstructor
public class EmailPreviewController {

    private final EmailService emailService;

    //GET /api/notify/emails/{tracking}/{estado}   (estado: creado, aceptado, en-bodega, en-ruta, entregado, cancelado) 
    @GetMapping("/{tracking}/{estado}")
    public ResponseEntity<String> preview(@PathVariable String tracking,
                                          @PathVariable String estado) throws IOException {
        Path file = emailService.resolvePreview(tracking, estado);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(Files.readString(file, StandardCharsets.UTF_8));
    }
}
