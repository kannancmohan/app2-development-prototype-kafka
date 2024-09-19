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
      topics = {"test_string-topic"},
      groupId = "test_string-group1")
  public void consumeString(@Payload final String message) {
    log.info("String message: {}", message);
  }

  @KafkaListener(
      topics = {"test_string-topic"},
      groupId = "test_batch_string-group1",
      containerFactory = "batchContainerFactory")
  public void consumeBatchString(@Payload final List<String> messages) {
    for (final String message : messages) {
      log.info("[Batch]String message: {}", message);
    }
  }

  @KafkaListener(
      topics = {"test_message_obj-topic"},
      groupId = "test_message_obj-group1",
      containerFactory = "messageContainerFactory")
  public void consumeMessage(@Payload final Message message, final Acknowledgment acknowledgment) {
    CompletableFuture.runAsync(() -> log.info("Object message: {}", message))
        .thenRun(acknowledgment::acknowledge) // Acknowledge the message after successful processing
        .exceptionally(
            ex -> {
              // Handle failure (e.g., retry or send to DLQ)
              log.error("Error in processing message: {}", message, ex);
              return null;
            });
  }
}
