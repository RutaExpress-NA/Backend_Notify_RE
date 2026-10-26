package com.na.rutaexpress.notify.admin.service;

import com.na.rutaexpress.notify.admin.dto.BindingRequest;
import com.na.rutaexpress.notify.admin.dto.ExchangeRequest;
import com.na.rutaexpress.notify.admin.dto.NamePattern;
import com.na.rutaexpress.notify.admin.dto.QueueRequest;
import com.na.rutaexpress.notify.config.RabbitProperties;
import com.na.rutaexpress.notify.exception.InvalidRequestException;
import com.na.rutaexpress.notify.exception.ResourceConflictException;
import com.na.rutaexpress.notify.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

//Toda la logica para crear y eliminar colas, exchanges y bindings en RabbitMQ
 
//El controller solo recibe la peticion y delega aqui

//Los recursos que usa el propio sistema (colas, DLQ y exchanges definidos en application.properties estan protegidos, no se pueden eliminar desde la API


@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitAdminService {

    private static final Pattern NAME = Pattern.compile(NamePattern.REGEX);

    private final AmqpAdmin amqpAdmin;
    private final RabbitProperties props;

    // ---------------------------------------------------------------- COLAS

    public Map<String, Object> createQueue(QueueRequest req) {
        boolean hasDlx = notBlank(req.getDeadLetterExchange());
        boolean hasKey = notBlank(req.getDeadLetterRoutingKey());
        if (hasDlx != hasKey) {
            throw new InvalidRequestException(
                    "deadLetterExchange y deadLetterRoutingKey deben enviarse juntos o ninguno de los dos");
        }
        if (queueExists(req.getName())) {
            throw new ResourceConflictException("La cola '" + req.getName() + "' ya existe");
        }

        QueueBuilder builder = req.isDurable()
                ? QueueBuilder.durable(req.getName())
                : QueueBuilder.nonDurable(req.getName());
        if (hasDlx) {
            builder.deadLetterExchange(req.getDeadLetterExchange())
                   .deadLetterRoutingKey(req.getDeadLetterRoutingKey());
        }
        Queue queue = builder.build();
        amqpAdmin.declareQueue(queue);

        log.info("[ADMIN] Cola creada name={} durable={} dlx={}",
                req.getName(), req.isDurable(), hasDlx ? req.getDeadLetterExchange() : "-");
        return queueInfo(req.getName());
    }

    public Map<String, Object> getQueue(String name) {
        validateName(name);
        return queueInfo(name);
    }

    public void deleteQueue(String name) {
        validateName(name);
        rejectIfProtected(name);
        if (!queueExists(name)) {
            throw new ResourceNotFoundException("La cola '" + name + "' no existe");
        }
        amqpAdmin.deleteQueue(name);
        log.info("[ADMIN] Cola eliminada name={}", name);
    }

    // ------------------------------------------------------------ EXCHANGES

    public Map<String, Object> createExchange(ExchangeRequest req) {
        Exchange exchange = switch (req.getType()) {
            case DIRECT -> new DirectExchange(req.getName(), req.isDurable(), false);
            case TOPIC -> new TopicExchange(req.getName(), req.isDurable(), false);
            case FANOUT -> new FanoutExchange(req.getName(), req.isDurable(), false);
        };
        amqpAdmin.declareExchange(exchange);

        log.info("[ADMIN] Exchange creado name={} type={} durable={}",
                req.getName(), req.getType(), req.isDurable());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", req.getName());
        result.put("type", req.getType());
        result.put("durable", req.isDurable());
        return result;
    }

    public void deleteExchange(String name) {
        validateName(name);
        rejectIfProtected(name);
        amqpAdmin.deleteExchange(name);
        log.info("[ADMIN] Exchange eliminado name={}", name);
    }

    // ------------------------------------------------------------- BINDINGS

    public Map<String, Object> createBinding(BindingRequest req) {
        rejectIfProtected(req.getQueue());
        if (!queueExists(req.getQueue())) {
            throw new ResourceNotFoundException("La cola '" + req.getQueue() + "' no existe");
        }
        try {
            amqpAdmin.declareBinding(toBinding(req.getQueue(), req.getExchange(), req.getRoutingKey()));
        } catch (AmqpException e) {
            throw new ResourceNotFoundException("No se pudo crear el binding. Verifica que el exchange '"
                    + req.getExchange() + "' exista");
        }

        log.info("[ADMIN] Binding creado queue={} exchange={} routingKey={}",
                req.getQueue(), req.getExchange(), req.getRoutingKey());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("queue", req.getQueue());
        result.put("exchange", req.getExchange());
        result.put("routingKey", req.getRoutingKey());
        return result;
    }

    public void deleteBinding(String queue, String exchange, String routingKey) {
        if (!notBlank(queue) || !notBlank(exchange)) {
            throw new InvalidRequestException("queue y exchange son obligatorios");
        }
        rejectIfProtected(queue);
        if (!queueExists(queue)) {
            throw new ResourceNotFoundException("La cola '" + queue + "' no existe");
        }
        amqpAdmin.removeBinding(toBinding(queue, exchange, routingKey));
        log.info("[ADMIN] Binding eliminado queue={} exchange={} routingKey={}", queue, exchange, routingKey);
    }

    // -------------------------------------------------------------- AYUDAS

    private Binding toBinding(String queue, String exchange, String routingKey) {
        return new Binding(queue, Binding.DestinationType.QUEUE, exchange, routingKey, null);
    }

    private boolean queueExists(String name) {
        return amqpAdmin.getQueueInfo(name) != null;
    }

    private Map<String, Object> queueInfo(String name) {
        QueueInformation info = amqpAdmin.getQueueInfo(name);
        if (info == null) {
            throw new ResourceNotFoundException("La cola '" + name + "' no existe");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", info.getName());
        result.put("messages", info.getMessageCount());
        result.put("consumers", info.getConsumerCount());
        return result;
    }

    private void validateName(String name) {
        if (name == null || !NAME.matcher(name).matches()) {
            throw new InvalidRequestException(NamePattern.MESSAGE);
        }
    }

    private void rejectIfProtected(String name) {
        if (protectedResources().contains(name)) {
            throw new ResourceConflictException("'" + name
                    + "' es parte de la infraestructura del sistema y no se puede modificar ni eliminar");
        }
    }

    /** Colas, DLQ y exchanges definidos en application.properties. */
    private Set<String> protectedResources() {
        return Set.of(
                props.getQueues().getEmail(), props.getQueues().getWarehouse(), props.getQueues().getLabel(),
                props.getDlqs().getEmail(), props.getDlqs().getWarehouse(), props.getDlqs().getLabel(),
                props.getExchanges().getEvents(), props.getExchanges().getCommands(), props.getExchanges().getDlx());
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
