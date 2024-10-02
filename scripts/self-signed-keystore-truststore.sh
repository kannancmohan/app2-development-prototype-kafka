#!/bin/bash

## Usage
# ./self-signed-keystore-truststore.sh -s server-keystore -c client-keystore -t truststore -p test@test.com

# Function to display help
show_help() {
  echo "Usage: $0 -s <server_keystore_name> -c <client_keystore_name> -t <truststore_name> -p <password>"
  echo
  echo "Options:"
  echo "  -s <server_keystore_name>  Name of the server keystore (without .p12 extension)"
  echo "  -c <client_keystore_name>  Name of the client keystore (without .p12 extension)"
  echo "  -t <truststore_name>       Name of the truststore (without .p12 extension)"
  echo "  -p <password>              Password for keystores and truststores"
  echo "  -h                         Show this help message"
  exit 1
}

# Get user inputs from command line arguments
while getopts "s:c:t:p:h" opt; do
  case "$opt" in
    s) server_keystore_name=$OPTARG ;;
    c) client_keystore_name=$OPTARG ;;
    t) truststore_name=$OPTARG ;;
    p) password=$OPTARG ;;
    h) show_help ;;
    *) show_help ;;
  esac
done

# Ensure mandatory inputs are provided
if [ -z "$server_keystore_name" ] || [ -z "$client_keystore_name" ] || [ -z "$truststore_name" ] || [ -z "$password" ]; then
  echo "Error: Missing required arguments."
  show_help
fi

# Variables
CN="E5"  # Common Name used consistently
server_keystore_file="${server_keystore_name}.p12"
client_keystore_file="${client_keystore_name}.p12"
server_truststore_file="server-${truststore_name}.p12"
client_truststore_file="client-${truststore_name}.p12"
ca_key="ca-key.pem"
ca_cert="ca-cert.pem"
server_csr="server-csr.pem"
server_cert="server-cert.pem"
client_csr="client-csr.pem"
client_cert="client-cert.pem"
server_alias="serverkey"
client_alias="clientkey"
truststore_alias="ca-cert"
dname_server="CN=${CN}, OU=Server, O=MyCompany, L=City, ST=State, C=US"
dname_client="CN=${CN}, OU=Client, O=MyCompany, L=City, ST=State, C=US"
validity_days=365

# Step 1: Generate the CA's private key and self-signed certificate
echo "Creating CA private key and self-signed certificate with CN=${CN}..."
openssl genrsa -out "$ca_key" 2048

openssl req -x509 -new -nodes -key "$ca_key" -sha256 -days "$validity_days" \
  -out "$ca_cert" -subj "/CN=${CN}/OU=RootCA/O=MyCompany/L=City/ST=State/C=US"

if [ $? -ne 0 ]; then
  echo "Error generating CA certificate."
  exit 1
fi

# Step 2: Create the server keystore and generate a certificate signing request (CSR)
echo "Creating server keystore and generating a certificate signing request (CSR) with CN=${CN}..."
keytool -genkeypair \
  -alias "$server_alias" \
  -keyalg RSA \
  -keysize 2048 \
  -validity "$validity_days" \
  -keystore "$server_keystore_file" \
  -storetype PKCS12 \
  -storepass "$password" \
  -keypass "$password" \
  -dname "$dname_server" \
  -ext SAN=dns:localhost

# Generate CSR from the server keystore
keytool -certreq \
  -alias "$server_alias" \
  -keystore "$server_keystore_file" \
  -storepass "$password" \
  -file "$server_csr"

if [ $? -ne 0 ]; then
  echo "Error generating CSR from server keystore."
  exit 1
fi

# Step 3: Sign the server's CSR using the CA certificate
echo "Signing the server certificate with the CA certificate..."
openssl x509 -req -in "$server_csr" -CA "$ca_cert" -CAkey "$ca_key" -CAcreateserial \
  -out "$server_cert" -days "$validity_days" -sha256

if [ $? -ne 0 ]; then
  echo "Error signing server certificate."
  exit 1
fi

# Step 4: Import the CA certificate and the signed server certificate into the server keystore
echo "Importing the CA certificate and signed server certificate into the server keystore..."
keytool -importcert \
  -alias "ca" \
  -file "$ca_cert" \
  -keystore "$server_keystore_file" \
  -storepass "$password" \
  -noprompt

keytool -importcert \
  -alias "$server_alias" \
  -file "$server_cert" \
  -keystore "$server_keystore_file" \
  -storepass "$password" \
  -noprompt

if [ $? -ne 0 ]; then
  echo "Error importing certificates into the server keystore."
  exit 1
fi

# Step 5: Create the client keystore and generate a certificate signing request (CSR)
echo "Creating client keystore and generating a certificate signing request (CSR) with CN=${CN}..."
keytool -genkeypair \
  -alias "$client_alias" \
  -keyalg RSA \
  -keysize 2048 \
  -validity "$validity_days" \
  -keystore "$client_keystore_file" \
  -storetype PKCS12 \
  -storepass "$password" \
  -keypass "$password" \
  -dname "$dname_client" \
  -ext SAN=dns:localhost

# Generate CSR from the client keystore
keytool -certreq \
  -alias "$client_alias" \
  -keystore "$client_keystore_file" \
  -storepass "$password" \
  -file "$client_csr"

if [ $? -ne 0 ]; then
  echo "Error generating CSR from client keystore."
  exit 1
fi

# Step 6: Sign the client's CSR using the CA certificate
echo "Signing the client certificate with the CA certificate..."
openssl x509 -req -in "$client_csr" -CA "$ca_cert" -CAkey "$ca_key" -CAcreateserial \
  -out "$client_cert" -days "$validity_days" -sha256

if [ $? -ne 0 ]; then
  echo "Error signing client certificate."
  exit 1
fi

# Step 7: Import the CA certificate and the signed client certificate into the client keystore
echo "Importing the CA certificate and signed client certificate into the client keystore..."
keytool -importcert \
  -alias "ca" \
  -file "$ca_cert" \
  -keystore "$client_keystore_file" \
  -storepass "$password" \
  -noprompt

keytool -importcert \
  -alias "$client_alias" \
  -file "$client_cert" \
  -keystore "$client_keystore_file" \
  -storepass "$password" \
  -noprompt

if [ $? -ne 0 ]; then
  echo "Error importing certificates into the client keystore."
  exit 1
fi

# Step 8: Create the server truststore and import the CA certificate (so the server trusts the client)
echo "Creating server truststore and importing the CA certificate..."
keytool -importcert \
  -alias "$truststore_alias" \
  -file "$ca_cert" \
  -keystore "$server_truststore_file" \
  -storetype PKCS12 \
  -storepass "$password" \
  -noprompt

if [ $? -ne 0 ]; then
  echo "Error creating server truststore."
  exit 1
fi

# Step 9: Create the client truststore and import the CA certificate (so the client trusts the server)
echo "Creating client truststore and importing the CA certificate..."
keytool -importcert \
  -alias "$truststore_alias" \
  -file "$ca_cert" \
  -keystore "$client_truststore_file" \
  -storetype PKCS12 \
  -storepass "$password" \
  -noprompt

if [ $? -ne 0 ]; then
  echo "Error creating client truststore."
  exit 1
fi

# Step 10: Verify the keystores and truststores
echo "Verifying server keystore: $server_keystore_file"
keytool -list -keystore "$server_keystore_file" -storepass "$password" -storetype PKCS12 -v

echo "Verifying client keystore: $client_keystore_file"
keytool -list -keystore "$client_keystore_file" -storepass "$password" -storetype PKCS12 -v

echo "Verifying server truststore: $server_truststore_file"
keytool -list -keystore "$server_truststore_file" -storepass "$password" -storetype PKCS12 -v

echo "Verifying client truststore: $client_truststore_file"
keytool -list -keystore "$client_truststore_file" -storepass "$password" -storetype PKCS12 -v

# Step 11: Clean up temporary files
#rm -f "$server_csr" "$server_cert" "$client_csr" "$client_cert"
rm -f "$server_csr" "$server_cert" "$client_csr" "$client_cert" "$ca_key" "$ca_cert" "ca-cert.srl"


echo "Server and client keystores and truststores created successfully with CN=${CN}!"
