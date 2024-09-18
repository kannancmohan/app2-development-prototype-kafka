package com.kcm.msp.dev.app2.development.prototype.kafka.consumer.config;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.springframework.kafka.support.serializer.JsonDeserializer.TRUSTED_PACKAGES;

import com.fasterxml.jackson.databind.JsonNode;
import com.kcm.msp.dev.app2.development.prototype.kafka.consumer.models.Message;
import com.kcm.msp.dev.app2.development.prototype.kafka.consumer.properties.KafkaProperty;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@EnableKafka
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

  private final KafkaProperty kafkaProperty;

  @Bean // for consuming string message
  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
    final ConcurrentKafkaListenerContainerFactory<String, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    return factory;
  }

  @Bean // for consuming Message object
  public ConcurrentKafkaListenerContainerFactory<String, Message>
      messageKafkaListenerContainerFactory() {
    final ConcurrentKafkaListenerContainerFactory<String, Message> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(messageConsumerFactory());
    return factory;
  }

  @Bean // for consuming json object
  public ConcurrentKafkaListenerContainerFactory<String, JsonNode>
      jsonObjectKafkaListenerContainerFactory() {
    // remove if custom errorHandler is not required
    final var errorHandler =
        new DefaultErrorHandler(
            (consumerRecord, exception) -> {
              // add logic to execute retry attempts are exhausted. eg send to a dead-letter topic
              log.error(
                  "Message from topic {} could not be processed after multiple retries: {}",
                  consumerRecord.topic(),
                  consumerRecord.value(),
                  exception);
            },
            new FixedBackOff(1000L, 2L)); // Retry twice with 1 second delay

    final ConcurrentKafkaListenerContainerFactory<String, JsonNode> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(jsonObjectConsumerFactory());
    factory
        .getContainerProperties()
        .setAckMode(
            AckMode.RECORD); // If precise error handling and resilience are critical for your
    // application, it's recommended to explicitly set the acknowledgment mode
    // to AckMode.RECORD
    factory.setCommonErrorHandler(errorHandler);
    return factory;
  }

  @Bean // for batch consuming string messages
  public ConcurrentKafkaListenerContainerFactory<String, String>
      batchKafkaListenerContainerFactory() {
    final ConcurrentKafkaListenerContainerFactory<String, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(batchConsumerFactory());
    factory.setBatchListener(true); // Enable batch mode
    factory.setConcurrency(3); // Optional: sets the number of concurrent threads
    return factory;
  }

  private ConsumerFactory<String, String> consumerFactory() {
    final Map<String, Object> props = new HashMap<>();
    props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaProperty.getBootstrapServers());
    props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    // props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    // props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, "20971520");
    // props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, "20971520");
    return new DefaultKafkaConsumerFactory<>(props);
  }

  private ConsumerFactory<String, Message> messageConsumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaProperty.getBootstrapServers());

    /**
     * Setting ErrorHandlingDeserializer for both key and value deserializer to handle
     * deserialization error. The ErrorHandlingDeserializer wraps the actual deserializer and
     * delegates the deserialization process to the underlying deserializer. If deserialization
     * fails, it catches the error and provides a default behavior or delegates to a custom error
     * handler *
     */
    props.put(KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    props.put(VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

    props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
    props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
    props.put(
        TRUSTED_PACKAGES,
        "*"); // whitelist of package names that deserializer is allowed to deserialize
    props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Message.class);
    return new DefaultKafkaConsumerFactory<>(props);
  }

  private ConsumerFactory<String, JsonNode> jsonObjectConsumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaProperty.getBootstrapServers());
    props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(
        TRUSTED_PACKAGES,
        "*"); // whitelist of package names that deserializer is allowed to deserialize
    props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, JsonNode.class);
    return new DefaultKafkaConsumerFactory<>(props);
  }

  private ConsumerFactory<String, String> batchConsumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaProperty.getBootstrapServers());
    props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ENABLE_AUTO_COMMIT_CONFIG, false); // Disable auto-commit for better control
    props.put(MAX_POLL_RECORDS_CONFIG, 100); // Max number of records per batch
    props.put(FETCH_MIN_BYTES_CONFIG, 1024); // Minimum data to fetch in bytes
    props.put(FETCH_MAX_WAIT_MS_CONFIG, 5000); // Max wait time if data is less than FETCH_MIN_BYTES
    return new DefaultKafkaConsumerFactory<>(props);
  }
}
