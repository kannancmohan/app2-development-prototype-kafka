package com.kcm.msp.dev.app2.development.prototype.kafka.consumer.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.springframework.kafka.support.serializer.JsonDeserializer.TRUSTED_PACKAGES;

import com.fasterxml.jackson.databind.JsonNode;
import com.kcm.msp.dev.app2.development.prototype.kafka.consumer.models.Message;
import com.kcm.msp.dev.app2.development.prototype.kafka.consumer.properties.KafkaProperty;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.RecordDeserializationException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonDelegatingErrorHandler;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
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
  public ConcurrentKafkaListenerContainerFactory<String, String> defaultContainerFactory() {
    final ConcurrentKafkaListenerContainerFactory<String, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(defaultConsumerFactory());
    return factory;
  }

  @Bean // for consuming Message object. Its configured to skips deserialization failures
  public ConcurrentKafkaListenerContainerFactory<String, Message> messageContainerFactory() {
    final ConcurrentKafkaListenerContainerFactory<String, Message> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(messageConsumerFactory());
    factory.setConcurrency(3); // Set concurrency for parallelism
    factory.getContainerProperties().setAckMode(AckMode.MANUAL); // Use manual acknowledgment
    factory.setCommonErrorHandler(commonErrorHandler());
    return factory;
  }

  @Bean // for consuming json object
  public ConcurrentKafkaListenerContainerFactory<String, JsonNode> jsonContainerFactory() {
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
    factory.setConsumerFactory(jsonConsumerFactory());
    // If error handling and resilience are critical for you, it's recommended to set AckMode.RECORD
    factory.getContainerProperties().setAckMode(AckMode.RECORD);
    factory.setCommonErrorHandler(errorHandler);
    return factory;
  }

  @Bean // for batch consuming string messages
  public ConcurrentKafkaListenerContainerFactory<String, String> batchContainerFactory() {
    final ConcurrentKafkaListenerContainerFactory<String, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(batchConsumerFactory());
    factory.setBatchListener(true); // Enable batch mode
    factory.setConcurrency(3); // Optional: sets the number of concurrent threads
    return factory;
  }

  private ConsumerFactory<String, String> defaultConsumerFactory() {
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
    final Map<String, Object> props = new HashMap<>();
    props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaProperty.getBootstrapServers());
    // Disable auto commit. make sure its manually done after message is successfully processed
    props.put(ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Message.class);
    props.put(TRUSTED_PACKAGES, "*"); // whitelist of package names allowed to deserialize
    /**
     * Setting ErrorHandlingDeserializer as value deserializer to handle deserialization error. The
     * ErrorHandlingDeserializer wraps the actual deserializer and delegates the deserialization
     * process to the underlying deserializer(in this case JsonDeserializer)
     */
    final var valueDeserializer =
        new ErrorHandlingDeserializer<>(new JsonDeserializer<>(Message.class));
    // custom logic to handle Failed deserialized record
    valueDeserializer.setFailedDeserializationFunction(
        failedInfo -> {
          final var data =
              failedInfo.getData() != null ? new String(failedInfo.getData(), UTF_8) : "null";
          log.error("failed deserializing [topic: {}, record: {}]", failedInfo.getTopic(), data);
          return null; // could also provide a default value or custom error
        });
    return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
  }

  private ConsumerFactory<String, JsonNode> jsonConsumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaProperty.getBootstrapServers());
    // Disable auto commit. make sure its manually done after message is successfully processed
    props.put(ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(TRUSTED_PACKAGES, "*"); // whitelist of package names allowed to deserialize
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

  // An error handler that delegates to different error handlers, depending on the exception type
  private CommonErrorHandler commonErrorHandler() {
    final var defaultHandler = defaultErrorHandler(1000L, 2L);
    final var handler = new CommonDelegatingErrorHandler(defaultHandler);
    // add additional custom error handlers based on exception type
    return handler;
  }

  private DefaultErrorHandler defaultErrorHandler(long delayBtwnRetries, long retryAttempts) {
    // Retry 2 times with a 1 second delay in between retries
    final var fixedBackOff = new FixedBackOff(delayBtwnRetries, retryAttempts);
    final var handler = new DefaultErrorHandler(defaultRecordRecoverer(), fixedBackOff);
    // Ensures record is acknowledged only after error handling
    handler.setAckAfterHandle(false);
    // Add custom logic to handle RecordDeserializationException
    handler.addNotRetryableExceptions(RecordDeserializationException.class);
    // custom logic to execute during retries
    handler.setRetryListeners(defaultRetryListener());
    return handler;
  }

  // logic to execute when attempting retries
  private RetryListener defaultRetryListener() {
    return (rec, ex, deliveryAttempt) -> {
      log.info(
          "Attempting retry for record in [topic:{}, key:{}] attemptCount:{}",
          rec.topic(),
          rec.key(),
          deliveryAttempt);
    };
  }

  // logic to execute when all the retry attempts are exhausted
  private ConsumerRecordRecoverer defaultRecordRecoverer() {
    return (rec, exp) -> {
      log.info(
          "Message from topic {} could not be processed after multiple retries: {}",
          rec.topic(),
          rec.value(),
          exp);
    };
  }
}
