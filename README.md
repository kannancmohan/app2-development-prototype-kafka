## app2-development-prototype-kafka

Kafka consumer and producer implementations using springboot and spring-kafka

## Project folder Structure

```bash

├── kafka-consumer/
│   ├── src
│   ├── pox.xml
│   └── README.md
├── scripts/ # contains shell scripts 
├── pom.xml
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

### Kafka topics and consumer naming

* Kafka topic: use dot seperator. format: {projectName}.{environment}.{topic-name}.v{version}
  eg: app.dev.sap.material.change.event.v1
* Kafka consumer group:use kebab-case naming format: {projectName}-{environmnet}-{serviceName}

## Project IDE initial setup

//TODO

## Project Setup and Configuring

### Kafka consumer project

[Check consumer README](kafka-consumer/README.md)

### Kafka producer project

[Check producer README](kafka-producer/README.md)

## Local kafka broker and kafka-ui

make sure to update the .env file

```
cd infra/docker
docker-compose up -d
```

## Manually testing kafka consumer and producer with kafka cli

1. download kafka binary from https://kafka.apache.org/downloads
2. extract the downloaded binary and execute the commands from kafka-binary/bin folder
3. if you are connecting to a secure kafka instance add a configuration file(kafka.properties) with connect details
   eg: Sample configuration file for connecting to kafka server using jaas authentication

   ```
   #bootstarp.servers=<kafka-server-host>:<9092>
   ssl.endpoint.identification.algorithm=https
   security.protocol=SASL_SSL
   sasl.mechanism=PLAIN
   sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="<USER-NAME>" password="<PASSWORD>";
   ```
4. command to list all topics in a kafka server

```
kafka-topics.sh --bootstrap-server <kafka-server-host>:<9092> --command-config kafka.properties --list --exclude-internal
```

5. command to view a topic configuration

```
kafka-topics.sh --bootstrap-server <kafka-server-host>:<9092> --command-config kafka.properties --describe --topic <topic-name>
```

6. command to send data to a topic

```
kafka-console-producer.sh --bootstrap-server <kafka-server-host>:<9092> --producer.config kafka.properties --topic <topic-name> 
```

7. command to consume data from a topic (from the begining)

```
kafka-console-consumer.sh --bootstrap-server <kafka-server-host>:<9092> --consumer.config kafka.properties --topic <topic-name> --from-beginning --group=<consumer-group-name> 
```

## Known child projects

