## Local kafka broker and kafka-ui

### Prerequisites for kafka without ssl

1. docker and docker-compose
2. Add the necessary environment variables make sure to update the .env file or to set the necessary

```
HOST_NAME= kafka.kcmeu.duckdns.org
```

### Prerequisites for kafka with ssl

1. docker and docker-compose

2. Add the necessary environment variables make sure to update the .env file or to set the necessary

```
HOST_NAME=kafka.kcmeu.duckdns.org
SSL_TRUSTSTORE_PASSWORD=your-truststore-pwd
SSL_KEYSTORE_PASSWORD=your-keystore-pwd
```

3. keystore and truststore files and its passwords

check local-docker-setup.md for generating keystore and truststore files

### start kafka broker without ssl

```
cd infra/docker/
docker-compose up -d
```

### start kafka broker with ssl (connecting client doesn't need valid ssl cert to connect)

```
cd infra/docker/
docker-compose -f docker-compose-ssl.yml up
```

### start kafka broker with ssl (connecting client need valid ssl cert to connect)

```
cd infra/docker/
docker-compose -f docker-compose-ssl-strict.yml up
```

