package com.kcm.msp.dev.app2.development.prototype.kafka.consumer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kcm.msp.dev.app2.development.prototype.kafka.consumer.models.Message;
import java.time.Duration;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit.jupiter.DisabledIf;

@Tag("IntegrationTest")
@DisabledIf(expression = "#{environment['skip.integration.test'] == 'true'}")
@SpringBootTest(properties = {"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@EmbeddedKafka(
    partitions = 2,
    topics = {"int_test_string-topic", "int_test_message_obj-topic", "int_test_json_obj-topic"})
public class KafkaConsumerIntegrationTest {

  @Autowired private KafkaProperties kafkaProperties;

  @Autowired private EmbeddedKafkaBroker broker;

  @Autowired
  private ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory;

  @Autowired
  private ConcurrentKafkaListenerContainerFactory<String, String>
      batchKafkaListenerContainerFactory;

  @Autowired
  private ConcurrentKafkaListenerContainerFactory<String, Message>
      messageKafkaListenerContainerFactory;

  @Autowired
  private ConcurrentKafkaListenerContainerFactory<String, JsonNode>
      jsonObjectKafkaListenerContainerFactory;

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
      final var consumer = getConsumer(kafkaListenerContainerFactory, topic, group);
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
      final var consumer = getConsumer(batchKafkaListenerContainerFactory, topic, group);
      final var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(60));
      assertAll(
          () -> assertNotNull(batchKafkaListenerContainerFactory),
          () -> assertTrue(batchKafkaListenerContainerFactory.isBatchListener()),
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

    @Test
    void customObjectMessageShouldInvokeKafkaConsumer() throws JsonProcessingException {
      final var topic = "int_test_message_obj-topic";
      final var group = "test_message-group1";
      final var payloadKey = "test_message-key";
      final var payload =
          new ObjectMapper()
              .writeValueAsString(Message.builder().messageId("msgid").message("msg").build());
      producer.send(new ProducerRecord<>(topic, payloadKey, payload));
      final var consumer = getConsumer(messageKafkaListenerContainerFactory, topic, group);
      final var record = KafkaTestUtils.getSingleRecord(consumer, topic, Duration.ofSeconds(30));
      assertAll(() -> assertNotNull(record), () -> assertEquals(payloadKey, record.key()));
      consumer.close();
    }
  }

  @Nested
  @TestInstance(PER_CLASS)
  class TestJsonPayload {
    private Producer<String, String> producer;

    @BeforeAll
    void beforeAll() {
      final var producerProperties = kafkaProperties.buildProducerProperties(null);
      // final var producerProperties = KafkaTestUtils.producerProps(broker);
      producer = new KafkaProducer<>(producerProperties);
    }

    @Test
    void jsonMessageShouldInvokeKafkaConsumer() {
      final var topic = "int_test_json_obj-topic";
      final var group = "test_message-group1";
      final var payloadKey = "test_message-key";
      final var payload = "{\"id\": 1, \"name\": \"test\"}";
      producer.send(new ProducerRecord<>(topic, payloadKey, payload));
      final var consumer = getConsumer(jsonObjectKafkaListenerContainerFactory, topic, group);
      final var record = KafkaTestUtils.getSingleRecord(consumer, topic, Duration.ofSeconds(30));
      assertAll(() -> assertNotNull(record), () -> assertEquals(payloadKey, record.key()));
      consumer.close();
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
}
