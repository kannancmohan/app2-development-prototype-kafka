package com.kcm.msp.dev.app2.development.prototype.kafka.consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.kcm.msp.dev.app2.development.prototype.kafka.consumer.KafkaConsumerWithSSLAndBearerAuthIntegrationTest.CONSUMER_OAUTH_TOKEN_ENDPOINT;
import static org.springframework.http.HttpStatus.OK;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.jackson.io.JacksonSerializer;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaZKBroker;

public class WireMockInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  private static final String BROKER_PORT = "19881";
  private static final String BROKER_SSL_PORT = "19882";

  private static final String BROKER_ZK_PORT = "19885";

  private static final String BROKER_KEYSTORE_LOCATION =
      "src/test/resources/self-signed-certs/server-keystore2.p12";
  private static final String BROKER_KEYSTORE_PWD = "test@test.com";

  private static final String BROKER_TRUSTSTORE_LOCATION =
      "src/test/resources/self-signed-certs/server-truststore2.p12";
  private static final String BROKER_TRUSTSTORE_PWD = "test@test.com";
  private static final String BROKER_SASL_JWKS_ENDPOINT =
      "http://localhost:19883/mock-idp/protocol/openid-connect/certs";
  public static final String CONSUMER_OAUTH_CLIENT_ID = "kafka-consumer2";

  public static final String CONSUMER_OAUTH_CLIENT_SECRET = "ZX10LkmMA8iESde9AjpL5hnJZbQPXzzm";

  public static final String CONSUMER_OAUTH_CLIENT_SCOPE = "openid profile";

  public static final String BROKER_SASL_EXPECTED_AUD = "account";
  public static final String MOCK_JWKS_RESPONSE =
      "{\n"
          + "  \"keys\": [\n"
          + "    {\n"
          + "      \"kty\": \"RSA\",\n"
          + "      \"kid\": \"test-key\",\n"
          + "      \"use\": \"sig\",\n"
          + "      \"alg\": \"RS256\",\n"
          + "      \"n\": \"your_modulus_here\",\n"
          + "      \"e\": \"AQAB\"\n"
          + "    }\n"
          + "  ]\n"
          + "}";

  @Override
  public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
    try {
      final var wireMockServer = generateWireMockServer();
      /*    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
          applicationContext, "wiremock.server.port=${wireMockServer.port()}"
      );*/
      applicationContext.getBeanFactory().registerSingleton("wireMockServer", wireMockServer);
      applicationContext.addApplicationListener(
          event -> {
            if (event instanceof ContextClosedEvent) {
              wireMockServer.stop();
            }
          });
      /*      TestPropertyValues
      .of("github.url:http://localhost:" + wireMockServer.port())
      .applyTo(applicationContext);*/
      final var embeddedKafka = generateKafkaBroker();
      //      embeddedKafka.afterPropertiesSet();
      applicationContext.getBeanFactory().registerSingleton("embeddedKafka", embeddedKafka);

      KafkaAdmin kafkaAdmin = kafkaAdmin(embeddedKafka);
      // applicationContext.getBeanFactory().registerSingleton("kafkaAdmin", kafkaAdmin);

      AdminClient adminClient = adminClient(kafkaAdmin);
      applicationContext.getBeanFactory().registerSingleton("kafkaAdminClient", adminClient);
      embeddedKafka.afterPropertiesSet();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static WireMockServer generateWireMockServer() throws Exception {

    final String kid = UUID.randomUUID().toString();
    final KeyPair keyPair = JwtUtil.generateEncodedKeyPair();
    final String jwks_json_old = JwtUtil.generateJwks(keyPair.getPublic(), kid);

    // TEST//
    KeyPair pair = Jwts.SIG.RS512.keyPair().build();
    RSAPublicKey pubKey = (RSAPublicKey) pair.getPublic();
    RSAPrivateKey privKey = (RSAPrivateKey) pair.getPrivate();

    /*RsaPublicJwk jwk = Jwks.builder().key(pubKey).idFromThumbprint().build();
    byte[] utf8Bytes = new JacksonSerializer().serialize(jwk);
    String jwks_json = new String(utf8Bytes, StandardCharsets.UTF_8);*/

    Jwk jwk = Jwks.builder().id("key-id-123").key(pubKey).algorithm("RS512").build();
    JwkSet set = Jwks.set().add(jwk).build();
    byte[] utf8Bytes = new JacksonSerializer().serialize(set);
    String jwks_json = new String(utf8Bytes, StandardCharsets.UTF_8);

    final String access_token =
        JwtUtil.generateRsaJwt(
            privKey, kid, CONSUMER_OAUTH_CLIENT_ID, Map.of("aud", BROKER_SASL_EXPECTED_AUD), 300);
    /*    final String access_token =
    JwtUtil.generateJwt(
        keyPair.getPrivate(),
        kid,
        CONSUMER_OAUTH_CLIENT_ID,
        Map.of("aud", BROKER_SASL_EXPECTED_AUD),
        300);*/
    final String access_token_json = "{ \"access_token\": \"" + access_token + "\" }";

    WireMockServer wireMockServer =
        new WireMockServer(WireMockConfiguration.wireMockConfig().port(19883));
    wireMockServer.start();
    // WireMock.configureFor("localhost", wireMockServer.port());
    wireMockServer.stubFor(
        get(urlEqualTo("/mock-idp/protocol/openid-connect/certs"))
            .willReturn(
                aResponse()
                    .withStatus(OK.value())
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(jwks_json)));

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

  private static String generateToken(final String username, final String audience)
      throws Exception {
    var claims = Map.of("aud", audience);
    final var now = Instant.now();
    return Jwts.builder()
        .subject(username)
        .claims(claims)
        .issuedAt(Date.from(now))
        .expiration(java.sql.Date.from(Instant.now().plusSeconds(300)))
        .signWith(getSignKey())
        .compact();
  }

  private static Key getSignKey() {
    final String SECRET = "357638792F423F4428472B4B6250655368566D597133743677397A2443264629";
    byte[] keyBytes = Decoders.BASE64.decode(SECRET);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  private static EmbeddedKafkaBroker generateKafkaBroker() throws Exception {
    final var listeners =
        "INTERNAL://:"
            + WireMockInitializer.BROKER_PORT
            + ",EXTERNAL://:"
            + WireMockInitializer.BROKER_SSL_PORT;
    final var advListeners =
        "INTERNAL://localhost:"
            + WireMockInitializer.BROKER_PORT
            + ",EXTERNAL://localhost:"
            + WireMockInitializer.BROKER_SSL_PORT;
    final var brokerProperties = new HashMap<String, String>();
    // brokerProperties.put("security.protocol","SASL_SSL");
    brokerProperties.put("listeners", listeners);
    brokerProperties.put("advertised.listeners", advListeners);
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
    // brokerProperties.put("controller.listener.names","PLAINTEXT");
    // brokerProperties.put("sasl.mechanism.controller.protocol","PLAIN");
    brokerProperties.put("listener.name.external.sasl.enabled.mechanisms", "OAUTHBEARER");
    brokerProperties.put("inter.broker.listener.name", "INTERNAL");
    // brokerProperties.put("listener.name.internal.sasl.enabled.mechanisms", "PLAIN");
    brokerProperties.put(
        "listener.name.external.oauthbearer.sasl.jaas.config",
        "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;");
    brokerProperties.put(
        "listener.name.external.oauthbearer.sasl.server.callback.handler.class",
        "org.apache.kafka.common.security.oauthbearer.OAuthBearerValidatorCallbackHandler");
    brokerProperties.put(
        "sasl.oauthbearer.jwks.endpoint.url", WireMockInitializer.BROKER_SASL_JWKS_ENDPOINT);
    brokerProperties.put(
        "sasl.oauthbearer.expected.audience", WireMockInitializer.BROKER_SASL_EXPECTED_AUD);

    /*    final var broker =
    new EmbeddedKafkaZKBroker(
        1,
        true,
        KafkaConsumerWithSSLAndBearerAuthIntegrationTest.STRING_TOPIC,
        KafkaConsumerWithSSLAndBearerAuthIntegrationTest.MESSAGE_TOPIC);*/
    final var broker = new EmbeddedKafkaZKBroker(1);
    broker.zkPort(Integer.parseInt(WireMockInitializer.BROKER_ZK_PORT));

    // https://github.com/spring-projects/spring-kafka/issues/2916
    /*        final var broker =
    new EmbeddedKafkaKraftBroker(
        1,1,
        KafkaConsumerWithSSLAndBearerAuthIntegrationTest.STRING_TOPIC,
        KafkaConsumerWithSSLAndBearerAuthIntegrationTest.MESSAGE_TOPIC);*/

    broker.kafkaPorts(Integer.parseInt(WireMockInitializer.BROKER_SSL_PORT));
    broker.brokerProperties(brokerProperties);
    // broker.afterPropertiesSet();
    return broker;
  }

  private static KafkaAdmin kafkaAdmin(EmbeddedKafkaBroker embeddedKafkaBroker) {
    Map<String, Object> configs = new HashMap<>();
    configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:" + BROKER_SSL_PORT);
    configs.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
    configs.put("sasl.mechanism", "OAUTHBEARER");
    configs.put(
        "ssl.truststore.location", "src/test/resources/self-signed-certs/client-truststore2.p12");
    configs.put("ssl.truststore.password", "test@test.com");
    //    configs.put(
    //        "ssl.keystore.location", "src/test/resources/self-signed-certs/client-keystore2.p12");
    //    configs.put("ssl.keystore.password", "test@test.com");
    configs.put(
        "sasl.jaas.config",
        "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required "
            + "clientId=\""
            + CONSUMER_OAUTH_CLIENT_ID
            + "\" clientSecret=\""
            + CONSUMER_OAUTH_CLIENT_SECRET
            + "\" "
            + "tokenEndpoint=\""
            + CONSUMER_OAUTH_TOKEN_ENDPOINT
            + "\" "
            + "scope=\""
            + CONSUMER_OAUTH_CLIENT_SCOPE
            + "\";");
    configs.put(
        "sasl.login.callback.handler.class",
        "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler");
    configs.put("sasl.oauthbearer.token.endpoint.url", CONSUMER_OAUTH_TOKEN_ENDPOINT);
    KafkaAdmin kafkaAdmin = new KafkaAdmin(configs);
    kafkaAdmin.setFatalIfBrokerNotAvailable(true);
    // kafkaAdmin.afterSingletonsInstantiated();
    return kafkaAdmin;
  }

  private static AdminClient adminClient(KafkaAdmin kafkaAdmin) {
    return KafkaAdminClient.create(kafkaAdmin.getConfigurationProperties());
  }
}
