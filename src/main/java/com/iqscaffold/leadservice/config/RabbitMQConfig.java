package com.iqscaffold.leadservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for Lead Service.
 * Configures exchanges, queues, and bindings for CRM events.
 */
@Configuration
public class RabbitMQConfig {

  public static final String EXCHANGE_NAME = "iqscaffold.events";
  public static final String DLX_EXCHANGE = "iqscaffold.dlx";
  public static final String CONTACT_CREATED_QUEUE = "iqscaffold.lead.contact.created";
  public static final String CONTACT_UPDATED_QUEUE = "iqscaffold.lead.contact.updated";
  public static final String CONTACT_DELETED_QUEUE = "iqscaffold.lead.contact.deleted";
  public static final String DLQ = "iqscaffold.dlq";
  public static final String CONTACT_CREATED_ROUTING_KEY = "contact.created";
  public static final String CONTACT_UPDATED_ROUTING_KEY = "contact.updated";
  public static final String CONTACT_DELETED_ROUTING_KEY = "contact.deleted";
  public static final String LEAD_CREATED_ROUTING_KEY = "lead.created";
  public static final String LEAD_UPDATED_ROUTING_KEY = "lead.updated";
  public static final String LEAD_DELETED_ROUTING_KEY = "lead.deleted";
  public static final String LEAD_CONVERTED_ROUTING_KEY = "lead.converted";

  /**
   * Creates the CRM events topic exchange.
   * This exchange is shared across all CRM services.
   *
   * @return TopicExchange for CRM events
   */
  @Bean
  public TopicExchange crmEventsExchange() {
    return new TopicExchange(EXCHANGE_NAME, true, false);
  }

  /**
   * Dead Letter Exchange for failed messages
   */
  @Bean
  public TopicExchange deadLetterExchange() {
    return new TopicExchange(DLX_EXCHANGE, true, false);
  }

  /**
   * Creates queue for contact created events with dead letter routing.
   *
   * @return Durable queue for contact created events
   */
  @Bean
  public Queue contactCreatedQueue() {
    return QueueBuilder
        .durable(CONTACT_CREATED_QUEUE)
        .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
        .withArgument("x-message-ttl", 86400000) // 24 hours
        .build();
  }

  /**
   * Creates queue for contact updated events with dead letter routing.
   *
   * @return Durable queue for contact updated events
   */
  @Bean
  public Queue contactUpdatedQueue() {
    return QueueBuilder
        .durable(CONTACT_UPDATED_QUEUE)
        .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
        .withArgument("x-message-ttl", 86400000) // 24 hours
        .build();
  }

  /**
   * Creates queue for contact deleted events with dead letter routing.
   *
   * @return Durable queue for contact deleted events
   */
  @Bean
  public Queue contactDeletedQueue() {
    return QueueBuilder
        .durable(CONTACT_DELETED_QUEUE)
        .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
        .withArgument("x-message-ttl", 86400000) // 24 hours
        .build();
  }

  /**
   * Dead Letter Queue for failed messages
   */
  @Bean
  public Queue deadLetterQueue() {
    return QueueBuilder
        .durable(DLQ)
        .build();
  }

  /**
   * Binds contact created queue to exchange.
   *
   * @param contactCreatedQueue Queue to bind
   * @param crmEventsExchange   Exchange to bind to
   * @return Binding configuration
   */
  @Bean
  public Binding contactCreatedBinding(
      final Queue contactCreatedQueue,
      final TopicExchange crmEventsExchange) {
    return BindingBuilder.bind(contactCreatedQueue)
        .to(crmEventsExchange)
        .with(CONTACT_CREATED_ROUTING_KEY);
  }

  /**
   * Binds contact updated queue to exchange.
   *
   * @param contactUpdatedQueue Queue to bind
   * @param crmEventsExchange   Exchange to bind to
   * @return Binding configuration
   */
  @Bean
  public Binding contactUpdatedBinding(
      final Queue contactUpdatedQueue,
      final TopicExchange crmEventsExchange) {
    return BindingBuilder.bind(contactUpdatedQueue)
        .to(crmEventsExchange)
        .with(CONTACT_UPDATED_ROUTING_KEY);
  }

  /**
   * Binds contact deleted queue to exchange.
   *
   * @param contactDeletedQueue Queue to bind
   * @param crmEventsExchange   Exchange to bind to
   * @return Binding configuration
   */
  @Bean
  public Binding contactDeletedBinding(
      final Queue contactDeletedQueue,
      final TopicExchange crmEventsExchange) {
    return BindingBuilder.bind(contactDeletedQueue)
        .to(crmEventsExchange)
        .with(CONTACT_DELETED_ROUTING_KEY);
  }

  /**
   * Bind dead letter queue to DLX with all routing keys
   */
  @Bean
  public Binding deadLetterBinding() {
    return BindingBuilder
        .bind(deadLetterQueue())
        .to(deadLetterExchange())
        .with("#");
  }

  /**
   * Creates a JSON message converter for RabbitMQ messages.
   *
   * @return Jackson2JsonMessageConverter
   */
  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  /**
   * Creates a RabbitAdmin for auto-declaring queues, exchanges, and bindings.
   *
   * @param connectionFactory RabbitMQ connection factory
   * @return Configured RabbitAdmin
   */
  @Bean
  public RabbitAdmin rabbitAdmin(final ConnectionFactory connectionFactory) {
    return new RabbitAdmin(connectionFactory);
  }

  /**
   * Creates a RabbitTemplate with JSON message converter.
   *
   * @param connectionFactory RabbitMQ connection factory
   * @return Configured RabbitTemplate
   */
  @Bean
  public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
    final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(jsonMessageConverter());
    return rabbitTemplate;
  }

  /**
   * Creates a RabbitListener container factory with JSON message converter.
   *
   * @param connectionFactory RabbitMQ connection factory
   * @return Configured SimpleRabbitListenerContainerFactory
   */
  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      final ConnectionFactory connectionFactory) {
    final SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(jsonMessageConverter());
    return factory;
  }


  /**
   * Tenant events queue with dead letter routing
   * Receives tenant lifecycle events from User Service
   */
  @Bean
  public Queue tenantEventsQueue() {
    return QueueBuilder
        .durable("iqscaffold.lead.tenant.events")
        .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
        .withArgument("x-message-ttl", 86400000) // 24 hours
        .build();
  }

  /**
   * Bind tenant events queue to exchange with tenant.# routing key
   * Receives all tenant lifecycle events (created, updated, deleted)
   */
  @Bean
  public Binding tenantEventsBinding(
      final Queue tenantEventsQueue,
      final TopicExchange crmEventsExchange) {
    return BindingBuilder.bind(tenantEventsQueue)
        .to(crmEventsExchange)
        .with("tenant.#");
  }

}
