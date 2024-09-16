package com.kcm.msp.dev.app2.development.prototype.kafka.consumer.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "spring.kafka")
@AllArgsConstructor(onConstructor = @__(@ConstructorBinding))
@Getter
public class KafkaProperty {
  private final String bootstrapServers;
}
