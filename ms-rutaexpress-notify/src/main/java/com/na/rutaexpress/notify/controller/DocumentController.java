package com.na.rutaexpress.notify.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.na.rutaexpress.notify.document.DocumentService;

import lombok.RequiredArgsConstructor;

// Permite abrir los PDF que generan los consumidores (etiqueta y ticket de picking)
@RestController
@RequestMapping("/api/notify/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    // GET /api/notify/documents/{tracking}/label   o   /picking
    @GetMapping("/{tracking}/{type}")
    public ResponseEntity<byte[]> download(@PathVariable String tracking,
                                           @PathVariable String type) throws IOException {
        Path file = documentService.resolve(tracking, type);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
                .body(Files.readAllBytes(file));
    }
}
