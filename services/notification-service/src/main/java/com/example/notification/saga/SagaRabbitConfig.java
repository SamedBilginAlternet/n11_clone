package com.example.notification.saga;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
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
    public Queue userRegisteredQueue() {
        return new Queue(SagaTopology.USER_REGISTERED_QUEUE, true);
    }

    @Bean
    public Queue orderConfirmedQueue() {
        return new Queue(SagaTopology.ORDER_CONFIRMED_QUEUE, true);
    }

    @Bean
    public Queue orderCancelledQueue() {
        return new Queue(SagaTopology.ORDER_CANCELLED_QUEUE, true);
    }

    @Bean
    public Binding bindUserRegistered(Queue userRegisteredQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(userRegisteredQueue).to(sagaExchange)
                .with(SagaTopology.USER_REGISTERED_ROUTING_KEY);
    }

    @Bean
    public Binding bindOrderConfirmed(Queue orderConfirmedQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(orderConfirmedQueue).to(sagaExchange)
                .with(SagaTopology.ORDER_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    public Binding bindOrderCancelled(Queue orderCancelledQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(orderCancelledQueue).to(sagaExchange)
                .with(SagaTopology.ORDER_CANCELLED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
