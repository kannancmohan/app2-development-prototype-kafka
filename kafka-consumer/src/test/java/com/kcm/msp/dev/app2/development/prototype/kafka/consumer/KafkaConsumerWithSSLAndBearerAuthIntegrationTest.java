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

@ActiveProfiles({"test", "test-ssl-and-bearer-auth"})
@Tag("IntegrationTest")
@DisabledIf(expression = "#{environment['skip.integration.test'] == 'true'}")
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
    })
@EmbeddedKafka(
    partitions = 2,
    brokerProperties = {
      "listeners=PLAINTEXT://:"
          + KafkaConsumerWithSSLAndBearerAuthIntegrationTest.BROKER_PORT
          + ",SASL_SSL://:"
          + KafkaConsumerWithSSLAndBearerAuthIntegrationTest.BROKER_SSL_PORT,
      "listener.security.protocol.map = PLAINTEXT:PLAINTEXT,SASL_SSL:SASL_SSL",
      "ssl.keystore.location="
          + KafkaConsumerWithSSLAndBearerAuthIntegrationTest.BROKER_KEYSTORE_LOCATION,
      "ssl.keystore.password="
          + KafkaConsumerWithSSLAndBearerAuthIntegrationTest.BROKER_KEYSTORE_PWD,
      "ssl.truststore.location="
          + KafkaConsumerWithSSLAndBearerAuthIntegrationTest.BROKER_TRUSTSTORE_LOCATION,
      "ssl.truststore.password="
          + KafkaConsumerWithSSLAndBearerAuthIntegrationTest.BROKER_TRUSTSTORE_PWD,
      "ssl.keystore.type=pkcs12",
      "ssl.truststore.type=pkcs12",
      "ssl.endpoint.identification.algorithm=",
      "sasl.enabled.mechanisms=PLAIN, OAUTHBEARER",
      "sasl.mechanism.inter.broker.protocol=PLAIN",
      "listener.name.sasl_ssl.sasl.enabled.mechanisms=OAUTHBEARER",
      "listener.name.sasl_ssl.oauthbearer.sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule"
          + " required;",
      "listener.name.sasl_ssl.oauthbearer.sasl.server.callback.handler.class=org.apache.kafka.common.security.oauthbearer.OAuthBearerValidatorCallbackHandler",
      "sasl.oauthbearer.jwks.endpoint.url="
          + KafkaConsumerWithSSLAndBearerAuthIntegrationTest.BROKER_SASL_JWKS_ENDPOINT,
      "sasl.oauthbearer.expected.audience="
          + KafkaConsumerWithSSLAndBearerAuthIntegrationTest.BROKER_SASL_EXPECTED_AUD
    },
    topics = {
      KafkaConsumerWithSSLAndBearerAuthIntegrationTest.STRING_TOPIC,
      KafkaConsumerWithSSLAndBearerAuthIntegrationTest.MESSAGE_TOPIC
    })
public class KafkaConsumerWithSSLAndBearerAuthIntegrationTest {

  public static final String BROKER_PORT = "19881";
  public static final String BROKER_SSL_PORT = "19882";

  public static final String BROKER_KEYSTORE_LOCATION =
      "src/test/resources/self-signed-certs/server-keystore.p12";
  public static final String BROKER_KEYSTORE_PWD = "test@test.com";

  public static final String BROKER_TRUSTSTORE_LOCATION =
      "src/test/resources/self-signed-certs/server-truststore.p12";
  public static final String BROKER_TRUSTSTORE_PWD = "test@test.com";
  public static final String BROKER_SASL_JWKS_ENDPOINT =
      "http://192.168.0.30:8666/realms/homelab/protocol/openid-connect/certs";
  public static final String BROKER_SASL_EXPECTED_AUD = "account";

  public static final String STRING_TOPIC = "int_test_ssl_bearer_string-topic";
  public static final String MESSAGE_TOPIC = "int_test_ssl_bearer_message_obj-topic";

  public static final String CONSUMER_OAUTH_CLIENT_ID = "kafka-consumer2";
  public static final String CONSUMER_OAUTH_CLIENT_SECRET = "ZX10LkmMA8iESde9AjpL5hnJZbQPXzzm";
  public static final String CONSUMER_OAUTH_TOKEN_ENDPOINT =
      "http://192.168.0.30:8666/realms/homelab/protocol/openid-connect/token";
  public static final String CONSUMER_OAUTH_CLIENT_SCOPE = "openid profile";

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
