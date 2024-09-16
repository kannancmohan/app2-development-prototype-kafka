package com.kcm.msp.dev.app2.development.prototype.kafka.consumer;

import com.kcm.msp.dev.app2.development.prototype.kafka.consumer.properties.KafkaProperty;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(KafkaProperty.class)
@SpringBootApplication
public class KafkaConsumerApplication {

  public static void main(String[] args) {
    SpringApplication.run(KafkaConsumerApplication.class, args);
  }
}
