package com.na.rutaexpress.notify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Lee los nombres de la infraestructura desde application.properties (prefijo app.rabbitmq).
 * Es el unico punto por donde el resto del codigo conoce esos nombres:
 * ningun string de cola o exchange se escribe en clases de negocio.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.rabbitmq")
public class RabbitProperties {

    private Exchanges exchanges = new Exchanges();
    private Queues queues = new Queues();
    private Queues dlqs = new Queues();
    private Routing routing = new Routing();
    private Retry retry = new Retry();

    /** Solo demo: prefijo de trackingCode que dispara una falla temporal simulada. */
    private String simulateFailurePrefix = "";

    @Data
    public static class Exchanges {
        private String events;     // topic
        private String commands;   // direct
        private String dlx;        // direct (dead letter exchange)
    }

    @Data
    public static class Queues {
        private String email;
        private String warehouse;
        private String label;
    }

    @Data
    public static class Routing {
        private String email;
        private String warehouse;
        private String label;
        private String dlqEmail;
        private String dlqWarehouse;
        private String dlqLabel;
    }

    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private long backoffMs = 2000;
    }
}
