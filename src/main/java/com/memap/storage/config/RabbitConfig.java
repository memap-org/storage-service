package com.memap.storage.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String PAYMENT_EVENTS_EXCHANGE = "payment.events";
    public static final String ROUTING_KEY_SUBSCRIPTION_CREATED = "subscription.created";
    public static final String ROUTING_KEY_SUBSCRIPTION_CANCELLED = "subscription.cancelled";

    public static final String STORAGE_SUBSCRIPTION_CREATED_QUEUE = "storage.subscription.created";
    public static final String STORAGE_SUBSCRIPTION_CANCELLED_QUEUE = "storage.subscription.cancelled";

    @Bean
    public TopicExchange paymentEventsExchange() {
        return new TopicExchange(PAYMENT_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue storageSubscriptionCreatedQueue() {
        return new Queue(STORAGE_SUBSCRIPTION_CREATED_QUEUE, true);
    }

    @Bean
    public Queue storageSubscriptionCancelledQueue() {
        return new Queue(STORAGE_SUBSCRIPTION_CANCELLED_QUEUE, true);
    }

    @Bean
    public Binding bindingSubscriptionCreated(Queue storageSubscriptionCreatedQueue, TopicExchange paymentEventsExchange) {
        return BindingBuilder.bind(storageSubscriptionCreatedQueue)
                .to(paymentEventsExchange)
                .with(ROUTING_KEY_SUBSCRIPTION_CREATED);
    }

    @Bean
    public Binding bindingSubscriptionCancelled(Queue storageSubscriptionCancelledQueue, TopicExchange paymentEventsExchange) {
        return BindingBuilder.bind(storageSubscriptionCancelledQueue)
                .to(paymentEventsExchange)
                .with(ROUTING_KEY_SUBSCRIPTION_CANCELLED);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
