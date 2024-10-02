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

3. If not exists, generate broker's keystore, truststore files and its password

For generating kafka broker keystore.p12 and truststore.p12 check keystore-truststore.md

4. Include keystore, truststore and password file in /secrets

files to include are broker.keystore.p12, broker.truststore.p12, keystore_pwd1.txt and truststore_pwd1.txt

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

