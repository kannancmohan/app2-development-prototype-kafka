## Steps for generating keystore and truststore files for kafka broker and clients

### Steps to generate keystore and truststore self-signed certificate files(p12) for server and client

   ```
   cd scripts/
   ./self-signed-keystore-truststore.sh -k keystore_name -t truststore_name -p test@test.com -q test@test.com -r test@test.com -s test@test.com
   ```

### Steps for generating keystore and truststore (p12) files for kafka broker
Here the assumption is that you already have your your-domain.crt, your-domain.key and CA.crt files
1. Download the CA.crt if not available

      1.1 optional: To check which ca.cert is been used by domain

      ```
      openssl s_client -connect vault.kcmeu.duckdns.org:443 -showcerts
      ```

      1.2 Download CA.cert( letsencrypt in this case)
      go to https://letsencrypt.org/certificates/ Right-click on the desired certificate (e.g., ISRG Root X1 or E5) and choose to save the certificate. Save it with a .crt or .pem extension
   
      ```
      wget https://letsencrypt.org/certs/2024/e5-cross.pem -O ca.crt
      ```
   
      1.3 optional: Verify the Downloaded CA Certificates
   
      ```
      openssl x509 -in ca.crt -text -noout
      ```
2.  Generate keystore p12 file for broker

      2.1 command to generate keystore 

      ```
      openssl pkcs12 -export -in your-domain.crt  -inkey your-domain.key -certfile ca.crt -name broker.keystore -out broker.keystore.p12 -passout pass:your-keystore-pwd
      ```

      2.2 optional: verify broker.keystore.p12 file

      ```
      openssl pkcs12 -info -in broker.keystore.p12
      ```

3. Generate truststore p12 file for broker

      3.1 command to generate truststore

      ```
      keytool -importcert -alias rootCA -file ca.crt -keystore broker.truststore.p12 -storetype PKCS12  -storepass your-truststore-pwd
      ```

      3.2 optional: verify broker.truststore.p12 file

      ```
      openssl pkcs12 -info -in  broker.truststore.p12
      ```

      3.3 optional: verifying a CA Certificate in Keystore/Truststore file

      ```
      openssl pkcs12 -info -in broker.keystore.p12 -nokeys
      openssl pkcs12 -info -in broker.truststore.p12 -nokeys
      ```

### Steps for generating keystore and truststore (p12) files for client

use the same steps mentioned in broker for generating keystore and truststore for client
1. Generate keystore p12 file for client

    1.1 command to generate keystore

      ```
      openssl pkcs12 -export -in your-domain.crt  -inkey your-domain.key -certfile ca.crt -name client.keystore -out client.keystore.p12 -passout pass:your-keystore-pwd
      ```

    1.2 optional: verify client.keystore.p12 file

      ```
      openssl pkcs12 -info -in client.keystore.p12
      ```

2. Generate truststore p12 file for client

   2.1 command to generate truststore

      ```
      keytool -importcert -alias rootCA -file ca.crt -keystore client.truststore.p12 -storetype PKCS12  -storepass your-truststore-pwd
      ```

   2.2 optional: verify client.truststore.p12 file

      ```
      openssl pkcs12 -info -in  client.truststore.p12
      ```

   2.3 optional: verifying a CA Certificate in Keystore/Truststore file

      ```
      openssl pkcs12 -info -in client.keystore.p12 -nokeys
      openssl pkcs12 -info -in client.truststore.p12 -nokeys
      ```

