package com.na.rutaexpress.notify.controller;

import com.na.rutaexpress.notify.dto.ShipmentEvent;
import com.na.rutaexpress.notify.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("estado", "Notify operativo"));
    }

    @PostMapping("/events")
    public ResponseEntity<Map<String, String>> receiveEvent(@Valid @RequestBody ShipmentEvent event) {
        notificationService.process(event);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("mensaje", "Evento encolado"));
    }
}
