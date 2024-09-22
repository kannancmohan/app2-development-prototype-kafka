package com.kcm.msp.dev.app2.development.prototype.kafka.consumer.service;

import com.kcm.msp.dev.app2.development.prototype.kafka.consumer.models.Message;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaConsumerService {

  @KafkaListener(
      topics = {"${consumers.string_consumer.topics}"},
      groupId = "${consumers.string_consumer.groupId}")
  public void consumeString(@Payload final String message) {
    log.info("String message: {}", message);
  }

  @KafkaListener(
      topics = {"${consumers.batch_string_consumer.topics}"},
      groupId = "${consumers.batch_string_consumer.groupId}",
      containerFactory = "${consumers.batch_string_consumer.containerFactory}")
  public void consumeBatchString(@Payload final List<String> messages) {
    for (final String message : messages) {
      log.info("[Batch]String message: {}", message);
    }
  }

  @KafkaListener(
      topics = {"${consumers.message_consumer.topics}"},
      groupId = "${consumers.message_consumer.groupId}",
      containerFactory = "${consumers.message_consumer.containerFactory}")
  public void consumeMessage(@Payload final Message message, final Acknowledgment acknowledgment) {
    CompletableFuture.runAsync(() -> log.info("Object message: {}", message))
        .thenRun(acknowledgment::acknowledge) // Acknowledge the message after successful processing
        .exceptionally(
            ex -> {
              log.error("Error in processing message: {}", message, ex);
              return null;
            });
  }
}
