## Local kafka broker and kafka-ui

### Start kafka broker without ssl

#### Prerequisites for kafka without ssl

1. docker and docker-compose
2. Add the following environment variables or update update the same in .env

```
KAFKA_HOST_NAME= kafka.broker.dev.local
```

#### To start kafka broker

```
cd infra/docker/kafka/
docker-compose up -d
```

### Start kafka broker with ssl

#### Prerequisites for kafka with ssl

1. docker and docker-compose

2. Add the following environment variables or update update the same in .env

```
KAFKA_HOST_NAME=kafka.broker.dev.local
KAFKA_TRUSTSTORE_PWD=your-truststore-password
KAFKA_KEYSTORE_PWD=your-keystore-password
```

3. If not exists, generate broker's keystore, truststore files and its password

For generating kafka broker keystore.p12 and truststore.p12 check keystore-truststore.md

4. Include keystore, truststore and password file in /infra/docker/kafka/docker_volume/kafka/ssl_secrets

files to include are broker.keystore.p12, broker.truststore.p12, keystore_pwd1.txt and truststore_pwd1.txt

#### To start kafka broker with ssl (connecting client doesn't need valid ssl cert to connect)

```
cd infra/docker/kafka/
docker-compose -f docker-compose-kafa-with-ssl.yml up
```

#### To start kafka broker with strict ssl (connecting client need valid ssl cert to connect)

```
cd infra/docker/
docker-compose -f docker-compose-kafa-with-strict-ssl.yml up
```

### Start kafka broker with ssl and use oauth-bearer authentication

#### Prerequisites for kafka with ssl

1. docker and docker-compose

2. Add the following environment variables or update update the same in .env

3. Ensure you have the identity provider(eg keycloak) started

```
KAFKA_HOST_NAME=kafka.broker.dev.local
KAFKA_TRUSTSTORE_PWD=your-truststore-password
KAFKA_KEYSTORE_PWD=your-keystore-password
IDP_JWKS_ENDPOINT_URL=your-idp-jwks-endpoint
IDP_JWT_EXPECTED_AUDIENCE=use the 'aud' value in jwt token
IDP_JWT_EXPECTED_ISSUER=use the 'iss' value in jwt token
```

3. If not exists, generate broker's keystore, truststore files and its password

For generating kafka broker keystore.p12 and truststore.p12 check keystore-truststore.md

4. Include keystore, truststore and password file in /infra/docker/kafka/docker_volume/kafka/ssl_secrets

files to include are broker.keystore.p12, broker.truststore.p12, keystore_pwd1.txt and truststore_pwd1.txt

#### To start kafka broker

```
cd infra/docker/kafka/
docker-compose -f docker-compose-kafka-with-ssl-and-bearer-auth.yml up
```

