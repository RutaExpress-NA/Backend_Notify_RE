package com.na.rutaexpress.notify.admin.controller;

import com.na.rutaexpress.notify.admin.dto.BindingRequest;
import com.na.rutaexpress.notify.admin.dto.ExchangeRequest;
import com.na.rutaexpress.notify.admin.dto.QueueRequest;
import com.na.rutaexpress.notify.admin.service.RabbitAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/api/rabbit-admin")
@RequiredArgsConstructor
public class RabbitAdminController {

    private final RabbitAdminService adminService;


    @PostMapping("/queues")
    public ResponseEntity<Map<String, Object>> createQueue(@Valid @RequestBody QueueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createQueue(request));
    }

    @GetMapping("/queues/{name}")
    public ResponseEntity<Map<String, Object>> getQueue(@PathVariable String name) {
        return ResponseEntity.ok(adminService.getQueue(name));
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> deleteQueue(@PathVariable String name) {
        adminService.deleteQueue(name);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, Object>> createExchange(@Valid @RequestBody ExchangeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createExchange(request));
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> deleteExchange(@PathVariable String name) {
        adminService.deleteExchange(name);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/bindings")
    public ResponseEntity<Map<String, Object>> createBinding(@Valid @RequestBody BindingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createBinding(request));
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> deleteBinding(@RequestParam String queue,
                                              @RequestParam String exchange,
                                              @RequestParam(defaultValue = "") String routingKey) {
        adminService.deleteBinding(queue, exchange, routingKey);
        return ResponseEntity.noContent().build();
    }
}
