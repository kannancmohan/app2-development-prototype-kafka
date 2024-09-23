## app2-development-prototype-kafka

Kafka consumer and producer implementations using springboot and spring-kafka

## Project folder Structure

```bash

└── kafka-consumer/
       ├── src
       ├── pox.xml
       └── README.md

```

## App implemented and tested with

        java 21 -(Corretto-21.0.3.9.1)
        maven 3.5.4+ -(3.9.7)
        spring-boot 3.3.2

## Project convention

### Git commit message

Git commit should follow the [conventionalcommits](https://www.conventionalcommits.org/en/v1.0.0/#summary) convention
There is a git pre-commit hook(commit-msg) configured to enforce this convention

### Code style:

The project uses spotless-maven-plugin to enforce style check on the following
* Java : Uses google java coding style
* POM :  enforce style check on pom.xml
* Markdown(*.md) : enforce style check on *.md files

Execute './mvnw spotless:check' to view code style violations and use './mvnw spotless:apply' to  manually apply coding style

## Project IDE initial setup

//TODO

## Project Setup and Configuring

#### Build application

```
../mvnw clean install

../mvnw clean install -Dspotless.skip=true # [To skip spotless check]

./mvnw clean install -Dskip.integration.test=true # [To skip integration test]
```

#### Code Format

```
../mvnw spotless:check [To view check style violations]
../mvnw spotless:apply [To apply check style fix to files]
```

#### Run application

```
../mvnw spring-boot:run
# [Remote debugging]
../mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

### Generate and Push Container Image

```
../mvnw clean install -U -Pjkube-build-push -Djkube.docker.username=<your-dockerhub-username> -Djkube.docker.password=<your-dockerhub-password>
```

eg:

```
../mvnw clean install -U -Pjkube-build-push -Djkube.docker.username=kannan2024 -Djkube.docker.password=password
```

To manually pull and run container-image using docker

```
docker pull kannan2024/app2-development-prototype-kafka-consumer
docker run -d -p 8881:8881 kannan2024/app2-development-prototype-kafka-consumer:latest
```

## Additional Configuring

### Configuring consumer for manual acknowledgement of message

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

### Configuring a non-blocking consumer

* Option1: Non blocking using custom async logic
* Option2: Non blocking using RetryTopicConfiguration or @RetryableTopic
  RetryTopicConfiguration provides ootb support for retrying failed Kafka message processing using dedicated retry topics

#### Option1: Non blocking using custom async logic

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

#### Option2: Non blocking using RetryTopicConfiguration or @RetryableTopic

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

