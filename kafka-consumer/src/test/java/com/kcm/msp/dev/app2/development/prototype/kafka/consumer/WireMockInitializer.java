package com.kcm.msp.dev.app2.development.prototype.kafka.consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.http.HttpStatus.OK;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaZKBroker;
import org.springframework.test.util.TestSocketUtils;

public class WireMockInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private static final String BROKER_KEYSTORE_LOCATION =
      "src/test/resources/self-signed-certs/server-keystore.p12";
  private static final String BROKER_KEYSTORE_PWD = "test@test.com";

  private static final String BROKER_TRUSTSTORE_LOCATION =
      "src/test/resources/self-signed-certs/server-truststore.p12";
  private static final String BROKER_TRUSTSTORE_PWD = "test@test.com";
  private static final String BROKER_SASL_JWKS_ENDPOINT =
      "http://localhost:%d/mock-idp/protocol/openid-connect/certs";
  private static final String CONSUMER_OAUTH_TOKEN_ENDPOINT =
      "http://localhost:%d/mock-idp/protocol/openid-connect/token";
  private static final String CONSUMER_OAUTH_CLIENT_ID = "kafka-consumer2";

  private static final String CONSUMER_OAUTH_CLIENT_SECRET = "ZX10LkmMA8iESde9AjpL5hnJZbQPXzzm";

  private static final String CONSUMER_OAUTH_CLIENT_SCOPE = "openid profile";

  private static final String BROKER_SASL_EXPECTED_AUD = "account";

  @Override
  public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
    try {
      final var wireMockServer = generateWireMockServer();
      int wireMockPort = wireMockServer.port();
      applicationContext.addApplicationListener(
          event -> {
            if (event instanceof ContextClosedEvent) {
              wireMockServer.stop();
            }
          });
      final String tokenEndpoint = String.format(CONSUMER_OAUTH_TOKEN_ENDPOINT, wireMockPort);
      final String jwksEndpoint = String.format(BROKER_SASL_JWKS_ENDPOINT, wireMockPort);
      TestPropertyValues.of("OAUTH2_TOKEN_ENDPOINT:" + tokenEndpoint).applyTo(applicationContext);
      applicationContext.getBeanFactory().registerSingleton("wireMockServer", wireMockServer);

      final var embeddedKafka = generateKafkaBroker(jwksEndpoint);
      embeddedKafka.afterPropertiesSet(); // start kafka broker
      final String kafkaBrokers = embeddedKafka.getBrokersAsString();
      TestPropertyValues.of("spring.embedded.kafka.brokers:" + kafkaBrokers)
          .applyTo(applicationContext);
      applicationContext.getBeanFactory().registerSingleton("embeddedKafka", embeddedKafka);
      final AdminClient adminClient = adminClient(kafkaBrokers, tokenEndpoint);
      applicationContext.getBeanFactory().registerSingleton("kafkaAdminClient", adminClient);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static WireMockServer generateWireMockServer() {
    int wireMockPort = TestSocketUtils.findAvailableTcpPort();
    final KeyPair pair = JwtUtil.generateRsaKeyPair();
    final String jwksJson =
        JwtUtil.generateJwksJson(JwtUtil.generateJwk("key-id-123", pair.getPublic()));
    final String access_token =
        JwtUtil.generateJwt(
            pair.getPrivate(),
            CONSUMER_OAUTH_CLIENT_ID,
            Map.of("aud", BROKER_SASL_EXPECTED_AUD),
            300);
    final String access_token_json = "{ \"access_token\": \"" + access_token + "\" }";

    final WireMockServer wireMockServer =
        new WireMockServer(WireMockConfiguration.wireMockConfig().port(wireMockPort));
    wireMockServer.start();
    wireMockServer.stubFor(
        get(urlEqualTo("/mock-idp/protocol/openid-connect/certs"))
            .willReturn(
                aResponse()
                    .withStatus(OK.value())
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(jwksJson)));

    wireMockServer.stubFor(
        post(urlEqualTo("/mock-idp/protocol/openid-connect/token"))
            // .withRequestBody(containing("clientId=kafka-consumer2"))
            .willReturn(
                aResponse()
                    .withStatus(OK.value())
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(access_token_json)));
    return wireMockServer;
  }

  private static EmbeddedKafkaBroker generateKafkaBroker(final String jwksEndpoint) {
    int internalPort = TestSocketUtils.findAvailableTcpPort();
    int externalPort = TestSocketUtils.findAvailableTcpPort();
    final var listeners = "INTERNAL://:" + internalPort + ",EXTERNAL://:" + externalPort;
    final var brokerProperties = new HashMap<String, String>();
    brokerProperties.put("listeners", listeners);
    brokerProperties.put("listener.security.protocol.map", "INTERNAL:PLAINTEXT,EXTERNAL:SASL_SSL");
    brokerProperties.put("ssl.keystore.location", WireMockInitializer.BROKER_KEYSTORE_LOCATION);
    brokerProperties.put("ssl.keystore.password", WireMockInitializer.BROKER_KEYSTORE_PWD);
    brokerProperties.put("ssl.truststore.location", WireMockInitializer.BROKER_TRUSTSTORE_LOCATION);
    brokerProperties.put("ssl.truststore.password", WireMockInitializer.BROKER_TRUSTSTORE_PWD);
    brokerProperties.put("ssl.keystore.type", "pkcs12");
    brokerProperties.put("ssl.truststore.type", "pkcs12");
    brokerProperties.put("ssl.endpoint.identification.algorithm", "");
    brokerProperties.put("sasl.enabled.mechanisms", "PLAIN, OAUTHBEARER");
    brokerProperties.put("sasl.mechanism.inter.broker.protocol", "PLAIN");
    brokerProperties.put("listener.name.external.sasl.enabled.mechanisms", "OAUTHBEARER");
    brokerProperties.put("inter.broker.listener.name", "INTERNAL");
    brokerProperties.put(
        "listener.name.external.oauthbearer.sasl.jaas.config",
        "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;");
    brokerProperties.put(
        "listener.name.external.oauthbearer.sasl.server.callback.handler.class",
        "org.apache.kafka.common.security.oauthbearer.OAuthBearerValidatorCallbackHandler");
    brokerProperties.put("sasl.oauthbearer.jwks.endpoint.url", jwksEndpoint);
    brokerProperties.put(
        "sasl.oauthbearer.expected.audience", WireMockInitializer.BROKER_SASL_EXPECTED_AUD);

    final var broker = new EmbeddedKafkaZKBroker(1);
    // broker.zkPort(Integer.parseInt(WireMockInitializer.BROKER_ZK_PORT));
    broker.kafkaPorts(externalPort);
    broker.brokerProperties(brokerProperties);
    return broker;
  }

  private static AdminClient adminClient(final String kafkaBroker, final String tokenEndpoint) {
    final Map<String, Object> configs = new HashMap<>();
    configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBroker);
    configs.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
    configs.put("sasl.mechanism", "OAUTHBEARER");
    configs.put(
        "ssl.truststore.location", "src/test/resources/self-signed-certs/client-truststore.p12");
    configs.put("ssl.truststore.password", "test@test.com");
    configs.put(
        "sasl.jaas.config",
        "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required "
            + "clientId=\""
            + CONSUMER_OAUTH_CLIENT_ID
            + "\" clientSecret=\""
            + CONSUMER_OAUTH_CLIENT_SECRET
            + "\" "
            + "tokenEndpoint=\""
            + tokenEndpoint
            + "\" "
            + "scope=\""
            + CONSUMER_OAUTH_CLIENT_SCOPE
            + "\";");
    configs.put(
        "sasl.login.callback.handler.class",
        "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler");
    configs.put("sasl.oauthbearer.token.endpoint.url", tokenEndpoint);
    return KafkaAdminClient.create(configs);
  }
}
