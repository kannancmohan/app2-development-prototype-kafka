## Configuring consumer for manual acknowledgement of message

1. set auto-commit to false in consumerFactory

```
props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
```

2. set AckMode in ConcurrentKafkaListenerContainerFactory

```
factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
```

3. manually commit message using Acknowledgment.acknowledge()
   Example:

```
@KafkaListener(topics = "my-topic", groupId = "batch-group", containerFactory = "containerFactory")
public void listen(String message, Acknowledgment acknowledgment) {
    // Manually commit the offset after successfully processing message
    acknowledgment.acknowledge();
}
```

## Configuring a non-blocking consumer

* Option1: Non blocking using custom async logic
* Option2: Non blocking using RetryTopicConfiguration or @RetryableTopic
  RetryTopicConfiguration provides ootb support for retrying failed Kafka message processing using dedicated retry topics

### Option1: Non blocking using custom async logic

1. set Concurrency in ConcurrentKafkaListenerContainerFactory

```
factory.setConcurrency(3);
```

2. Add logic to process message asynchronously
   Example:

```
@KafkaListener(topics = "my-topic", groupId = "batch-group", containerFactory = "containerFactory")
public void consumeMessage(@Payload final Message message) {
  CompletableFuture.runAsync(() -> log.info("Object message: {}", message))
      .exceptionally(
          ex -> {
            // Handle failure (e.g., retry or send to DLQ)
            log.error("Error in processing message: {}", message, ex);
            return null;
          });
}
```

### Option2: Non blocking using RetryTopicConfiguration or @RetryableTopic

1. Using RetryTopicConfiguration

```
@Bean
public RetryTopicConfiguration retryTopicConfig(KafkaTemplate<String, String> template) {
    return RetryTopicConfigurationBuilder
        .newInstance()
        .fixedBackOff(2000L)        // Delay of 2 seconds between retries
        .maxAttempts(3)             // Retry up to 3 times
        .includeTopics("my-topic")  // Only apply to "my-topic"
        .retryTopicSuffix(".retry") // Custom suffix for retry topics
        .concurrency(2)
        .create(template);
}
```

2. Using @RetryableTopic annotation

```
@RetryableTopic(kafkaTemplate = "kafkaTemplate",attempts = "4", backoff = @Backoff(delay = 3000, multiplier = 1.5, maxDelay = 15000))
@KafkaListener(topics = "my-topic", groupId = "batch-group")
public void orderEventListener(@Payload final Message message, Acknowledgment ack) throws SocketException {
    log.info("Object message: {}", message)
}
```

