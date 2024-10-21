package com.kcm.msp.dev.app2.development.prototype.kafka.consumer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kcm.msp.dev.app2.development.prototype.kafka.consumer.models.Message;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.CorruptRecordException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.KafkaMessageHeaderAccessor;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.DisabledIf;

@ActiveProfiles("test")
@Tag("IntegrationTest")
@DisabledIf(expression = "#{environment['skip.integration.test'] == 'true'}")
@SpringBootTest(properties = {"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@EmbeddedKafka(
    partitions = 2,
    topics = {"int_test_string-topic", "int_test_message_obj-topic"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class KafkaConsumerIntegrationTest {

  private static final String CUSTOM_OBJECT_TOPIC = "int_test_message_obj-topic";

  @Autowired private KafkaProperties kafkaProperties;

  @Autowired private EmbeddedKafkaBroker broker;

  @Autowired private InMemorySpanExporter spanExporter;

  @Autowired
  private ConcurrentKafkaListenerContainerFactory<String, String> defaultContainerFactory;

  @Autowired private ConcurrentKafkaListenerContainerFactory<String, String> batchContainerFactory;

  @Autowired CheckRetryAttempts checkRetryAttempts;

  @Profile("test")
  @TestConfiguration
  static class TestObservabilityConfig {
    @Bean // added for tracing test
    public SdkTracerProvider sdkTracerProvider(InMemorySpanExporter spanExporter) {
      return SdkTracerProvider.builder()
          .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
          .build();
    }

    @Bean //// added for tracing test
    public InMemorySpanExporter spanExporter() {
      return InMemorySpanExporter.create();
    }
  }

  @Nested
  @TestInstance(PER_CLASS)
  class TestStingPayload {

    private Producer<String, String> producer;

    @BeforeAll
    void beforeAll() {
      final var producerProperties = kafkaProperties.buildProducerProperties(null);
      // final var producerProperties = KafkaTestUtils.producerProps(broker);
      producer = new KafkaProducer<>(producerProperties);
    }

    @Test
    void stringMessageShouldInvokeKafkaConsumer() {
      final var topic = "int_test_string-topic";
      final var group = "test_string-group1";
      final var payloadKey = "test_string-key";
      final var payload = "Sending with our own simple KafkaProducer";
      producer.send(new ProducerRecord<>(topic, payloadKey, payload));
      final var consumer = getConsumer(defaultContainerFactory, topic, group);
      final var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(30));
      assertAll(
          () -> assertNotNull(records),
          () -> assertTrue(records.count() > 0),
          () ->
              assertTrue(
                  StreamSupport.stream(records.spliterator(), false)
                      .anyMatch(r -> payloadKey.equals(r.key()))));
      consumer.close();
    }

    @Test
    void stringMessagesShouldInvokeKafkaBatchConsumer() {
      final var topic = "int_test_string-topic";
      final var group = "test_string-group2";
      int noOfRecords = 5;
      IntStream.range(0, noOfRecords)
          .mapToObj(id -> new ProducerRecord<>(topic, "test-key_" + id, "payload_" + id))
          .forEach(r -> producer.send(r));
      final var consumer = getConsumer(batchContainerFactory, topic, group);
      final var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(60));
      assertAll(
          () -> assertNotNull(batchContainerFactory),
          () -> assertTrue(batchContainerFactory.isBatchListener()),
          () -> assertNotNull(records),
          () -> assertTrue(records.count() >= noOfRecords));
      consumer.close();
    }
  }

  @Nested
  @TestInstance(PER_CLASS)
  class TestCustomPayload {
    private Producer<String, String> producer;

    @BeforeAll
    void beforeAll() {
      final var producerProperties = kafkaProperties.buildProducerProperties(null);
      // final var producerProperties = KafkaTestUtils.producerProps(broker);
      producer = new KafkaProducer<>(producerProperties);
    }

    @BeforeEach
    void beforeEach() {
      spanExporter.reset(); // Clear spans before each test
    }

    @Test
    @DisplayName(
        "invalid payload should be handled by error-handler & listener errors should be retried")
    void customObjectsIncludingInvalidMessageShouldInvokeKafkaConsumer()
        throws JsonProcessingException, InterruptedException {
      final var objectMapper = new ObjectMapper();
      final var payloads =
          Map.of(
              "key_1", "Invalid JSON",
              "key_2",
                  objectMapper.writeValueAsString(
                      Message.builder().messageId(null).message("msg_msg_1").build()),
              "key_3",
                  objectMapper.writeValueAsString(
                      Message.builder().messageId("msg_id_2").message("msg_msg_2").build()));
      payloads.forEach((k, v) -> producer.send(new ProducerRecord<>(CUSTOM_OBJECT_TOPIC, k, v)));
      boolean isExpectedInvocations = checkRetryAttempts.countDown.await(30, TimeUnit.SECONDS);
      final Map<String, Integer> expectedAttempts = checkRetryAttempts.attempts;
      final var spans = spanExporter.getFinishedSpanItems();
      assertFalse(spans.isEmpty()); // ensure observability-tracing data is available
      assertAll(
          () -> assertTrue(isExpectedInvocations),
          () -> assertFalse(expectedAttempts.containsKey("key_1")),
          () ->
              assertTrue(
                  expectedAttempts.containsKey("key_2") && expectedAttempts.get("key_2") > 1),
          () ->
              assertTrue(
                  expectedAttempts.containsKey("key_3") && expectedAttempts.get("key_3") == 1));
    }
  }

  @SuppressWarnings("unchecked")
  private <K, V> Consumer<K, V> getConsumer(
      final ConcurrentKafkaListenerContainerFactory<K, V> containerFactory,
      final String topicId,
      final String groupId) {
    final var consumer = containerFactory.getConsumerFactory().createConsumer(groupId, "clientId");
    broker.consumeFromAnEmbeddedTopic(consumer, topicId);
    return (Consumer<K, V>) consumer;
  }

  @Component
  @Profile("test")
  static class CheckRetryAttempts {
    final int totalInvocation = 4;
    final Map<String, Integer> attempts = new HashMap<>();
    final CountDownLatch countDown = new CountDownLatch(totalInvocation);

    @KafkaListener(
        topics = CUSTOM_OBJECT_TOPIC,
        groupId = "test_message-group1",
        containerFactory = "messageContainerFactory",
        autoStartup = "true")
    void consumeMessage(
        @Payload final Message message,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        KafkaMessageHeaderAccessor accessor) {
      this.countDown.countDown();
      attempts.compute(key, (k, v) -> v != null ? v + 1 : 1);
      if (message == null || message.messageId() == null) {
        throw new CorruptRecordException("Corrupt record");
      }
    }
  }
}
