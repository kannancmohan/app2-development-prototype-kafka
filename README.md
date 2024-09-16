## app2-development-prototype-kafka

parent kafka project

## supported versions

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

### Build and Start the application

#### Build application

```
./mvnw clean install

./mvnw clean install -Dspotless.skip=true # [To skip spotless check]

./mvnw clean install -Dskip.integration.test=true # [To skip integration test]
```

#### Code Format

```
./mvnw spotless:check [To view check style violations]
./mvnw spotless:apply [To apply check style fix to files]
```

#### Run application

```
./mvnw spring-boot:run
# [Remote debugging]
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

### Generate and Push Container Image

```
./mvnw clean install -U -Pjkube-build-push -Djkube.docker.username=<your-dockerhub-username> -Djkube.docker.password=<your-dockerhub-password>
```

eg: ./mvnw clean install -U -Pjkube-build-push -Djkube.docker.username=kannan2024 -Djkube.docker.password=password

To manually pull and run container-image using docker

```
docker pull kannan2024/app2-development-prototype-microservice
docker run -d -p 8881:8881 kannan2024/app2-development-prototype-microservice:latest 
```

## Testing kafka consumer and producer with kafka cli
    * download kafka binary from https://kafka.apache.org/downloads
    * extract the downloaded binary and exceute the commands from kafka-binary/bin folder
    * if you are connecting to a secure kafka instance add a configuration file(kafka.properties) with connect details
    eg: This is an example configuration file for connecting to kafka server with jaas authentication
    ```
    #bootstarp.servers=<kafka-server-host>:<9092>
    ssl.endpoint.identification.algorithm=https
    security.protocol=SASL_SSL
    sasl.mechanism=PLAIN
    sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="<USER-NAME>" password="<PASSWORD>";
    ```
    ##To list all topics in a kafka server
    kafka-topics.sh --bootstrap-server <kafka-server-host>:<9092> --command-config kafka.properties --list --exclude-internal
    
    ##To view a topic configuration
    kafka-topics.sh --bootstrap-server <kafka-server-host>:<9092> --command-config kafka.properties --describe --topic <topic-name>

    ##To send data to a topic
    kafka-console-producer.sh --bootstrap-server <kafka-server-host>:<9092> --producer.config kafka.properties --topic <topic-name> 
    
    ##To consume data from a topic (from the begining)
    kafka-console-consumer.sh --bootstrap-server <kafka-server-host>:<9092> --consumer.config kafka.properties --topic <topic-name> --from-beginning --group=<consumer-group-name> 

## Known child projects

