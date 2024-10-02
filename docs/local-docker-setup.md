## Local kafka broker and kafka-ui

### Start kafka broker without ssl

#### Prerequisites for kafka without ssl

1. docker and docker-compose
2. Add the following environment variables or update update the same in .env

```
HOST_NAME= kafka.kcmeu.duckdns.org
```

#### To start kafka broker

```
cd infra/docker/
docker-compose up -d
```

### Start kafka broker with ssl

#### Prerequisites for kafka with ssl

1. docker and docker-compose

2. Add the following environment variables or update update the same in .env

```
HOST_NAME=kafka.kcmeu.duckdns.org
```

3. keystore and truststore files and its passwords

For generating kafka broker keystore.p12 and truststore.p12 check keystore-truststore.md

#### To start kafka broker with ssl (connecting client doesn't need valid ssl cert to connect)

```
cd infra/docker/
docker-compose -f docker-compose-ssl.yml up
```

#### To start kafka broker with strict ssl (connecting client need valid ssl cert to connect)

```
cd infra/docker/
docker-compose -f docker-compose-ssl-strict.yml up
```

