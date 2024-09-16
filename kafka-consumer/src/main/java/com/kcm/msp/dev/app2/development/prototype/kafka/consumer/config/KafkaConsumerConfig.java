package com.kcm.msp.dev.app2.development.prototype.kafka.consumer.config;

import com.kcm.msp.dev.app2.development.prototype.kafka.consumer.models.Message;
import com.kcm.msp.dev.app2.development.prototype.kafka.consumer.properties.KafkaProperty;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

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
    ConcurrentKafkaListenerContainerFactory<String, Message> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(messageConsumerFactory());
    return factory;
  }

  @Bean // for consuming Message object
  public ConcurrentKafkaListenerContainerFactory<String, Object>
      jsonObjectKafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(jsonObjectConsumerFactory());
    return factory;
  }

  private ConsumerFactory<String, String> consumerFactory() {
    final Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperty.getBootstrapServers());
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    // props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    // props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, "20971520");
    // props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, "20971520");
    return new DefaultKafkaConsumerFactory<>(props);
  }

  private ConsumerFactory<String, Message> messageConsumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperty.getBootstrapServers());
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(
        JsonDeserializer.TRUSTED_PACKAGES,
        "*"); // whitelist of package names that the deserializer is allowed to deserialize objects
    // from
    props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Message.class);
    return new DefaultKafkaConsumerFactory<>(props);
  }

  private ConsumerFactory<String, Object> jsonObjectConsumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperty.getBootstrapServers());
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(
        JsonDeserializer.TRUSTED_PACKAGES,
        "*"); // whitelist of package names that the deserializer is allowed to deserialize objects
    // from
    return new DefaultKafkaConsumerFactory<>(props);
  }
}
