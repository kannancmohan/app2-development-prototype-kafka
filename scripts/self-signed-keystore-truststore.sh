#!/bin/bash

### Script to generate keystore and truststore for server and client

### Prerequisite
# add the openssl.cnf file


#### Usage:
# ./self-signed-keystore-truststore.sh -k keystore_name -t truststore_name -p test@test.com -q test@test.com -r test@test.com -s test@test.com



# Function to display help
show_help() {
  echo "Usage: $0 -k <keystore_name> -t <truststore_name> -p <server_keystore_password> -q <server_truststore_password> -r <client_keystore_password> -s <client_truststore_password>"
  echo
  echo "Options:"
  echo "  -k <keystore_name>                Name of the client and server keystore files (without .p12 extension)"
  echo "  -t <truststore_name>              Name of the client and server truststore files (without .p12 extension)"
  echo "  -p <server_keystore_password>     Server Password for keystore"
  echo "  -q <server_truststore_password>   Server Password for truststore"
  echo "  -r <client_keystore_password>     Client Password for keystore"
  echo "  -s <client_truststore_password>   Client Password for truststore"
  echo "  -h                                Show this help message"
  exit 1
}

# Get user inputs from command line arguments
while getopts "k:t:p:q:r:s:h" opt; do
  case "$opt" in
    k) keystore_name=$OPTARG ;;
    t) truststore_name=$OPTARG ;;
    p) server_keystore_password=$OPTARG ;;
    q) server_truststore_password=$OPTARG ;;
    r) client_keystore_password=$OPTARG ;;
    s) client_truststore_password=$OPTARG ;;
    h) show_help ;;
    *) show_help ;;
  esac
done

# Ensure mandatory inputs are provided
if [ -z "$keystore_name" ] || [ -z "$truststore_name" ] || [ -z "$server_keystore_password" ] || [ -z "$server_truststore_password" ] || [ -z "$client_keystore_password" ] || [ -z "$client_truststore_password" ]; then
  echo "Error: Missing required arguments.$client_truststore_password"
  show_help
fi

# Variables
CN="kafka.broker.dev.local"  # Common Name used consistently
config_file="./openssl.cnf"  # Assuming the openssl.cnf is in the same directory
server_keystore_file="server-${keystore_name}.p12"
server_truststore_file="server-${truststore_name}.p12"
client_keystore_file="client-${keystore_name}.p12"
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
validity_days=3650 #10 years

# Step 1: Generate the CA's private key and self-signed certificate
echo "Creating CA private key and self-signed certificate with CN=${CN}..."
openssl genrsa -out "$ca_key" 2048

openssl req -x509 -new -nodes -key "$ca_key" -sha256 -days "$validity_days" \
  -out "$ca_cert" -subj "/CN=${CN}/OU=RootCA/O=MyCompany/L=City/ST=State/C=US"

if [ $? -ne 0 ]; then
  echo "Error generating CA certificate."
  exit 1
fi

# Step 2: Generate server private key and CSR with SAN
echo "Creating server key and generating a certificate signing request (CSR) with SAN..."
openssl req -new -nodes -newkey rsa:2048 -keyout server-key.pem \
  -out "$server_csr" -subj "/CN=${CN}/OU=Server/O=MyCompany/L=City/ST=State/C=US" \
  -config "$config_file" -reqexts v3_req

if [ $? -ne 0 ]; then
  echo "Error generating server CSR."
  exit 1
fi

# Step 3: Sign the server's CSR using the CA certificate, and include SAN
echo "Signing the server certificate with SAN..."
openssl x509 -req -in "$server_csr" -CA "$ca_cert" -CAkey "$ca_key" -CAcreateserial \
  -out "$server_cert" -days "$validity_days" -sha256 -extfile "$config_file" -extensions v3_req

if [ $? -ne 0 ]; then
  echo "Error signing server certificate."
  exit 1
fi

# Step 4: Import the CA certificate and the signed server certificate into the server keystore
echo "Creating server PKCS12 keystore..."
openssl pkcs12 -export -out "$server_keystore_file" -inkey server-key.pem -in "$server_cert" \
  -certfile "$ca_cert" -passout pass:"$server_keystore_password" -name "$server_alias"

if [ $? -ne 0 ]; then
  echo "Error creating server keystore."
  exit 1
fi

# Step 5: Create the client keystore (similar process as server)
echo "Creating client keystore and generating a certificate signing request (CSR) with SAN..."
openssl req -new -nodes -newkey rsa:2048 -keyout client-key.pem \
  -out "$client_csr" -subj "/CN=${CN}/OU=Client/O=MyCompany/L=City/ST=State/C=US" \
  -config "$config_file" -reqexts v3_req

if [ $? -ne 0 ]; then
  echo "Error generating client CSR."
  exit 1
fi

# Step 6: Sign the client CSR using the CA certificate, and include SAN
echo "Signing the client certificate with SAN..."
openssl x509 -req -in "$client_csr" -CA "$ca_cert" -CAkey "$ca_key" -CAcreateserial \
  -out "$client_cert" -days "$validity_days" -sha256 -extfile "$config_file" -extensions v3_req

if [ $? -ne 0 ]; then
  echo "Error signing client certificate."
  exit 1
fi

# Step 7: Import the CA certificate and the signed client certificate into the client keystore
echo "Creating client PKCS12 keystore..."
openssl pkcs12 -export -out "$client_keystore_file" -inkey client-key.pem -in "$client_cert" \
  -certfile "$ca_cert" -passout pass:"$client_keystore_password" -name "$client_alias"

if [ $? -ne 0 ]; then
  echo "Error creating client keystore."
  exit 1
fi

# Step 8: Create the server truststore and import the CA certificate
echo "Creating server truststore and importing the CA certificate..."
keytool -importcert \
  -alias "$truststore_alias" \
  -file "$ca_cert" \
  -keystore "$server_truststore_file" \
  -storepass "$server_truststore_password" \
  -storetype PKCS12 \
  -noprompt

if [ $? -ne 0 ]; then
  echo "Error creating server truststore."
  exit 1
fi

# Step 9: Create the client truststore and import the CA certificate
echo "Creating client truststore and importing the CA certificate..."
keytool -importcert \
  -alias "$truststore_alias" \
  -file "$ca_cert" \
  -keystore "$client_truststore_file" \
  -storepass "$client_truststore_password" \
  -storetype PKCS12 \
  -noprompt

if [ $? -ne 0 ]; then
  echo "Error creating client truststore."
  exit 1
fi

# Step 10: Verify the keystores and truststores
echo "Verifying server keystore: $server_keystore_file"
openssl pkcs12 -info -in "$server_keystore_file" -passin pass:"$server_keystore_password" -nodes

echo "Verifying client keystore: $client_keystore_file"
openssl pkcs12 -info -in "$client_keystore_file" -passin pass:"$client_keystore_password" -nodes

echo "Verifying server truststore: $server_truststore_file"
keytool -list -keystore "$server_truststore_file" -storepass "$server_truststore_password" -v

echo "Verifying client truststore: $client_truststore_file"
keytool -list -keystore "$client_truststore_file" -storepass "$client_truststore_password" -v

# Step 11: Clean up temporary files
rm -f "$server_csr" "$server_cert" "$client_csr" "$client_cert" "$ca_key" "$ca_cert" ca-cert.srl server-key.pem client-key.pem

echo "Keystores and truststores created successfully!"
