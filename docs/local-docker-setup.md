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
3. keystore and truststore certificates

