package com.example.search.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProductEventRabbitConfig {

    public static final String EXCHANGE = "product.exchange";
    public static final String QUEUE_CREATED = "search.product-created.queue";
    public static final String QUEUE_UPDATED = "search.product-updated.queue";
    public static final String QUEUE_DELETED = "search.product-deleted.queue";

    @Bean
    public TopicExchange productExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue productCreatedQueue() {
        return new Queue(QUEUE_CREATED, true);
    }

    @Bean
    public Queue productUpdatedQueue() {
        return new Queue(QUEUE_UPDATED, true);
    }

    @Bean
    public Queue productDeletedQueue() {
        return new Queue(QUEUE_DELETED, true);
    }

    @Bean
    public Binding createdBinding(Queue productCreatedQueue, TopicExchange productExchange) {
        return BindingBuilder.bind(productCreatedQueue).to(productExchange).with("product.created");
    }

    @Bean
    public Binding updatedBinding(Queue productUpdatedQueue, TopicExchange productExchange) {
        return BindingBuilder.bind(productUpdatedQueue).to(productExchange).with("product.updated");
    }

    @Bean
    public Binding deletedBinding(Queue productDeletedQueue, TopicExchange productExchange) {
        return BindingBuilder.bind(productDeletedQueue).to(productExchange).with("product.deleted");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter converter) {
        RabbitTemplate tpl = new RabbitTemplate(cf);
        tpl.setMessageConverter(converter);
        return tpl;
    }
}
