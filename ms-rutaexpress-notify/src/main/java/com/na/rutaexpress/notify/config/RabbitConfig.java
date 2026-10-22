package com.na.rutaexpress.notify.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
public class RabbitConfig {

    private final RabbitProperties props;

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(props.getExchanges().getEvents(), true, false);
    }

    /** Direct: enruta por coincidencia exacta. Lo usan los comandos puntuales. */
    @Bean
    public DirectExchange commandsExchange() {
        return new DirectExchange(props.getExchanges().getCommands(), true, false);
    }

    /** Dead Letter Exchange: recibe los mensajes rechazados desde las colas de trabajo. */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(props.getExchanges().getDlx(), true, false);
    }

    // ------------------------------------------------------- COLAS DE TRABAJO (con DLX)

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(props.getQueues().getEmail())
                .deadLetterExchange(props.getExchanges().getDlx())
                .deadLetterRoutingKey(props.getRouting().getDlqEmail())
                .build();
    }

    @Bean
    public Queue warehouseQueue() {
        return QueueBuilder.durable(props.getQueues().getWarehouse())
                .deadLetterExchange(props.getExchanges().getDlx())
                .deadLetterRoutingKey(props.getRouting().getDlqWarehouse())
                .build();
    }

    @Bean
    public Queue labelQueue() {
        return QueueBuilder.durable(props.getQueues().getLabel())
                .deadLetterExchange(props.getExchanges().getDlx())
                .deadLetterRoutingKey(props.getRouting().getDlqLabel())
                .build();
    }

    // ------------------------------------------------------------------------- DLQ

    @Bean
    public Queue emailDlq() {
        return QueueBuilder.durable(props.getDlqs().getEmail()).build();
    }

    @Bean
    public Queue warehouseDlq() {
        return QueueBuilder.durable(props.getDlqs().getWarehouse()).build();
    }

    @Bean
    public Queue labelDlq() {
        return QueueBuilder.durable(props.getDlqs().getLabel()).build();
    }

    // --------------------------------------------------------------------- BINDINGS

    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailQueue()).to(eventsExchange())
                .with(props.getRouting().getEmail());
    }

    @Bean
    public Binding warehouseBinding() {
        return BindingBuilder.bind(warehouseQueue()).to(eventsExchange())
                .with(props.getRouting().getWarehouse());
    }

    @Bean
    public Binding labelBinding() {
        return BindingBuilder.bind(labelQueue()).to(commandsExchange())
                .with(props.getRouting().getLabel());
    }

    @Bean
    public Binding emailDlqBinding() {
        return BindingBuilder.bind(emailDlq()).to(deadLetterExchange())
                .with(props.getRouting().getDlqEmail());
    }

    @Bean
    public Binding warehouseDlqBinding() {
        return BindingBuilder.bind(warehouseDlq()).to(deadLetterExchange())
                .with(props.getRouting().getDlqWarehouse());
    }

    @Bean
    public Binding labelDlqBinding() {
        return BindingBuilder.bind(labelDlq()).to(deadLetterExchange())
                .with(props.getRouting().getDlqLabel());
    }


    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
