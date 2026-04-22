package com.example.inventory.saga;

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
    public Queue orderCreatedQueue() {
        return new Queue(SagaTopology.ORDER_CREATED_QUEUE, true);
    }

    @Bean
    public Queue orderCancelledQueue() {
        return new Queue(SagaTopology.ORDER_CANCELLED_QUEUE, true);
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(sagaExchange)
                .with(SagaTopology.ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding orderCancelledBinding(Queue orderCancelledQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(orderCancelledQueue).to(sagaExchange)
                .with(SagaTopology.ORDER_CANCELLED_ROUTING_KEY);
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
