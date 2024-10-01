package com.kcm.msp.dev.app2.development.prototype.kafka.consumer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import java.time.Duration;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.DisabledIf;

@ActiveProfiles({"test", "test-ssl"})
@Tag("IntegrationTest")
@DisabledIf(expression = "#{environment['skip.integration.test'] == 'true'}")
@SpringBootTest(
    properties = {
      "spring.kafka.bootstrap-servers=localhost:" + KafkaSSLIntegrationTest.BROKER_SSL_PORT
    })
@EmbeddedKafka(
    partitions = 2,
    brokerProperties = {
      "listeners=PLAINTEXT://:"
          + KafkaSSLIntegrationTest.BROKER_PORT
          + ",SSL://:"
          + KafkaSSLIntegrationTest.BROKER_SSL_PORT,
      // "advertised.listeners=PLAINTEXT://localhost:19092,SSL://localhost:19093",
      "ssl.keystore.location=" + KafkaSSLIntegrationTest.BROKER_KEYSTORE_LOCATION,
      "ssl.keystore.password=" + KafkaSSLIntegrationTest.BROKER_KEYSTORE_PWD,
      // "ssl.key.password=" + KafkaSSLIntegrationTest.SERVER_KEYSTORE_PWD,
      "ssl.truststore.location=" + KafkaSSLIntegrationTest.BROKER_TRUSTSTORE_LOCATION,
      "ssl.truststore.password=" + KafkaSSLIntegrationTest.BROKER_TRUSTSTORE_PWD,
      "ssl.client.auth=none",
      "ssl.endpoint.identification.algorithm="
    },
    topics = {KafkaSSLIntegrationTest.STRING_TOPIC, KafkaSSLIntegrationTest.MESSAGE_TOPIC})
public class KafkaSSLIntegrationTest {

  public static final String BROKER_PORT = "19092";
  public static final String BROKER_SSL_PORT = "19093";

  public static final String BROKER_KEYSTORE_LOCATION =
      "src/test/resources/self-signed-certs/app-keystore.p12";
  public static final String BROKER_KEYSTORE_PWD = "test@keystore_pwd1";

  public static final String BROKER_TRUSTSTORE_LOCATION =
      "src/test/resources/self-signed-certs/app-truststore.p12";
  public static final String BROKER_TRUSTSTORE_PWD = "test@truststore_pwd1";

  public static final String STRING_TOPIC = "int_test_ssl_string-topic";
  public static final String MESSAGE_TOPIC = "int_test_ssl_message_obj-topic";

  @Autowired private EmbeddedKafkaBroker broker;
  @Autowired private KafkaProperties kafkaProperties;

  @Autowired
  private ConcurrentKafkaListenerContainerFactory<String, String> defaultContainerFactory;

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
      final var payloadKey = "test_string-key";
      final var payload = "Sending with our own simple KafkaProducer";
      final var group = "test_string-group1";
      producer.send(new ProducerRecord<>(STRING_TOPIC, payloadKey, payload));
      final var consumer = getConsumer(defaultContainerFactory, STRING_TOPIC, group);
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
