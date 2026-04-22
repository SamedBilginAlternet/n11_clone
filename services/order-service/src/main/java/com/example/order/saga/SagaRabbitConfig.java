package com.example.order.saga;

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
public class SagaRabbitConfig {

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(SagaTopology.EXCHANGE, true, false);
    }

    @Bean
    public Queue paymentSucceededQueue() {
        return new Queue(SagaTopology.PAYMENT_SUCCEEDED_QUEUE, true);
    }

    @Bean
    public Queue paymentFailedQueue() {
        return new Queue(SagaTopology.PAYMENT_FAILED_QUEUE, true);
    }

    @Bean
    public Queue inventoryOutOfStockQueue() {
        return new Queue(SagaTopology.INVENTORY_OUT_OF_STOCK_QUEUE, true);
    }

    @Bean
    public Binding paymentSucceededBinding(Queue paymentSucceededQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(paymentSucceededQueue).to(sagaExchange)
                .with(SagaTopology.PAYMENT_SUCCEEDED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentFailedBinding(Queue paymentFailedQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(paymentFailedQueue).to(sagaExchange)
                .with(SagaTopology.PAYMENT_FAILED_ROUTING_KEY);
    }

    @Bean
    public Binding inventoryOutOfStockBinding(Queue inventoryOutOfStockQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(inventoryOutOfStockQueue).to(sagaExchange)
                .with(SagaTopology.INVENTORY_OUT_OF_STOCK_ROUTING_KEY);
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
