package com.kcm.msp.dev.app2.development.prototype.kafka.consumer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaConsumerService {

  @KafkaListener(
      topics = {"test-topic"},
      groupId = "test-group1")
  public void consumeString(String message) {
    log.info("String message: {}", message);
  }
}
