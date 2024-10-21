package com.kcm.msp.dev.app2.development.prototype.kafka.consumer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Duration;
import java.util.List;
import java.util.stream.StreamSupport;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.DisabledIf;

@ActiveProfiles({"test-ssl-and-bearer-auth"})
@Tag("IntegrationTest")
@DisabledIf(expression = "#{environment['skip.integration.test'] == 'true'}")
@ContextConfiguration(initializers = {WireMockInitializer.class})
@SpringBootTest(
    properties = {
      "OAUTH2_CLIENT_ID="
          + KafkaConsumerWithSSLAndBearerAuthIntegrationTest.CONSUMER_OAUTH_CLIENT_ID,
      "OAUTH2_CLIENT_PWD="
          + KafkaConsumerWithSSLAndBearerAuthIntegrationTest.CONSUMER_OAUTH_CLIENT_SECRET,
      "OAUTH2_TOKEN_ENDPOINT="
          + KafkaConsumerWithSSLAndBearerAuthIntegrationTest.CONSUMER_OAUTH_TOKEN_ENDPOINT,
      "OAUTH2_CLIENT_SCOPE="
          + KafkaConsumerWithSSLAndBearerAuthIntegrationTest.CONSUMER_OAUTH_CLIENT_SCOPE,
      "spring.kafka.bootstrap-servers=localhost:"
          + KafkaConsumerWithSSLAndBearerAuthIntegrationTest.BROKER_SSL_PORT
      // "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
    })
public class KafkaConsumerWithSSLAndBearerAuthIntegrationTest {
  public static final String BROKER_SSL_PORT = "19882";

  public static final String STRING_TOPIC = "int_test_ssl_bearer_string-topic";
  public static final String MESSAGE_TOPIC = "int_test_ssl_bearer_message_obj-topic";
  public static final String CONSUMER_OAUTH_CLIENT_ID = "kafka-consumer2";
  public static final String CONSUMER_OAUTH_CLIENT_SECRET = "ZX10LkmMA8iESde9AjpL5hnJZbQPXzzm";
  public static final String CONSUMER_OAUTH_TOKEN_ENDPOINT =
      "http://localhost:19883/mock-idp/protocol/openid-connect/token";
  public static final String CONSUMER_OAUTH_CLIENT_SCOPE = "openid profile";

  @Autowired private EmbeddedKafkaBroker embeddedKafka;
  @Autowired private KafkaProperties kafkaProperties;

  @Autowired private AdminClient adminClient;

  @Autowired
  private ConcurrentKafkaListenerContainerFactory<String, String> defaultContainerFactory;

  @Autowired WireMockServer wireMockServer;

  @Nested
  // @TestInstance(PER_CLASS)
  class TestStingPayload {

    private Producer<String, String> producer;

    /*    @BeforeAll
    void beforeAll() {
      //final var producerProperties = kafkaProperties.buildProducerProperties(null);
       final var producerProperties = KafkaTestUtils.producerProps(embeddedKafka);
      producer = new KafkaProducer<>(producerProperties);
    }*/

    @Test
    void stringMessageShouldInvokeKafkaConsumer() {
      final var payloadKey = "test_string-key";
      final var payload = "Sending with our own simple KafkaProducer";
      final var group = "test_string-group1";
      final var producerProperties = kafkaProperties.buildProducerProperties(null);
      producer = new KafkaProducer<>(producerProperties);
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
    consumer.subscribe(List.of(topicId));
    // embeddedKafka.consumeFromAnEmbeddedTopic(consumer, topicId);
    return (Consumer<K, V>) consumer;
  }
}
