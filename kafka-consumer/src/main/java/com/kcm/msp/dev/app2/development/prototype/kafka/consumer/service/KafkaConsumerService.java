package com.kcm.msp.dev.app2.development.prototype.kafka.consumer.service;

import com.kcm.msp.dev.app2.development.prototype.kafka.consumer.models.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaConsumerService {

  @KafkaListener(
      topics = {"test-topic"},
      groupId = "test-group1")
  public void consumeString(final String message) {
    log.info("String message: {}", message);
  }

  /*
    @KafkaListener(
        topics = {"test-topic2"},
        groupId = "test-group1",
        containerFactory = "messageKafkaListenerContainerFactory")
    public void consumeMessage(final Message message) {
      log.info("Object message: {}", message);
    }
  */

  @KafkaListener(
      topics = {"test-topic3"},
      groupId = "test-group1",
      containerFactory = "jsonObjectKafkaListenerContainerFactory")
  public void consumeJson(final Message message) {
    log.info("JsonObject message: {}", message);
  }
}
