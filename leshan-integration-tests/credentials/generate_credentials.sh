#!/bin/bash

# Keystore parameters

CLIENT_STORE=clientKeyStore.jks
CLIENT_STORE_PWD=client
SERVER_STORE=serverKeyStore.jks
SERVER_STORE_PWD=server
TRUSTED_CA_STORE=trustedCaKeyStore.jks
TRUSTED_CA_STORE_PWD=trusted
UNKNOWN_CA_STORE=unknownCaKeyStore.jks
UNKNOWN_CA_STORE_PWD=unknown

VALIDITY=36500 #days
DEFAULT_STORE_TYPE=PKCS12 #PKCS12 is not supported by Java7, JKS can be used instead

# Color output stuff
red=`tput setaf 1`
green=`tput setaf 2`
blue=`tput setaf 4`
bold=`tput bold`
H1=${green}${bold} 
H2=${blue} 
RESET=`tput sgr0`

# Generation of the Trusted CA keystore needed for Leshan integration tests.
echo "${H1}Trusted CA Keystore : ${RESET}"
echo "${H1}======================${RESET}"
echo "${H2}Creating the trusted root CA key and certificate...${RESET}"
keytool -genkeypair -alias rootCA -keyalg EC -dname 'CN=Leshan root CA' \
        -validity $VALIDITY \
        -storetype $DEFAULT_STORE_TYPE \
        -ext BasicConstraints:critical=ca:true \
        -ext KeyUsage:critical=keyCertSign,cRLSign \
        -keypass $TRUSTED_CA_STORE_PWD -keystore $TRUSTED_CA_STORE -storepass $TRUSTED_CA_STORE_PWD
keytool -exportcert -alias rootCA -keystore $TRUSTED_CA_STORE -storepass $TRUSTED_CA_STORE_PWD -file rootCA.der
echo
echo "${H2}Creating the intermediate CA key and certificate...${RESET}"
keytool -genkeypair -alias intermediateCA -keyalg EC -dname 'CN=Leshan intermediate CA' \
        -validity $VALIDITY \
        -storetype $DEFAULT_STORE_TYPE \
        -ext BasicConstraints:critical=ca:true,pathlen:0 \
        -ext KeyUsage:critical=keyCertSign,cRLSign \
        -keypass $TRUSTED_CA_STORE_PWD -keystore $TRUSTED_CA_STORE -storepass $TRUSTED_CA_STORE_PWD
echo
keytool -certreq -alias intermediateCA -dname 'CN=Leshan intermediate CA' -keystore $TRUSTED_CA_STORE -storepass $TRUSTED_CA_STORE_PWD | \
  keytool -gencert -alias rootCA -keystore $TRUSTED_CA_STORE -storepass $TRUSTED_CA_STORE_PWD \
        -validity $VALIDITY \
        -ext BasicConstraints:critical=ca:true,pathlen:0 \
        -ext KeyUsage:critical=keyCertSign,cRLSign | \
  keytool -importcert -alias intermediateCA -keystore $TRUSTED_CA_STORE -storepass $TRUSTED_CA_STORE_PWD
keytool -exportcert -alias intermediateCA -keystore $TRUSTED_CA_STORE -storepass $TRUSTED_CA_STORE_PWD -file intermediateCA.der
echo
# Generation of the Unknown CA keystore needed for Leshan integration tests.
echo "${H1}Unknown CA Keystore : ${RESET}"
echo "${H1}======================${RESET}"
echo "${H2}Creating an untrusted root CA key and certificate...${RESET}"
keytool -genkeypair -alias untrustedRootCA -keyalg EC -dname 'CN=Leshan untrusted root CA' \
        -validity $VALIDITY \
        -storetype $DEFAULT_STORE_TYPE \
        -ext BasicConstraints:critical=ca:true \
        -ext KeyUsage:critical=keyCertSign,cRLSign \
        -keypass $UNKNOWN_CA_STORE_PWD -keystore $UNKNOWN_CA_STORE -storepass $UNKNOWN_CA_STORE_PWD
keytool -exportcert -alias untrustedRootCA -keystore $UNKNOWN_CA_STORE -storepass $UNKNOWN_CA_STORE_PWD -file untrustedRootCA.der
echo
# Generation of the keystore needed for Leshan integration tests.
echo "${H1}Server Keystore : ${RESET}"
echo "${H1}==================${RESET}"
echo "${H2}Creating server key and self-signed certificate ...${RESET}"
keytool -genkeypair -alias server -keyalg EC -dname 'CN=localhost' \
        -validity $VALIDITY \
        -storetype $DEFAULT_STORE_TYPE \
        -ext BasicConstraints=ca:false \
        -ext KeyUsage:critical=digitalSignature,keyAgreement \
        -ext ExtendedkeyUsage=serverAuth \
        -keypass $SERVER_STORE_PWD -keystore $SERVER_STORE -storepass $SERVER_STORE_PWD
keytool -exportcert -alias server -keystore $SERVER_STORE -storepass $SERVER_STORE_PWD | \
  keytool -importcert -alias server_self_signed -keystore $SERVER_STORE -storepass $SERVER_STORE_PWD -noprompt
echo
echo "${H2}Importing Root CA certificate ...${RESET}"
keytool -importcert -alias rootCA -keystore $SERVER_STORE -storepass $SERVER_STORE_PWD -noprompt -file rootCA.der
echo
echo "${H2}Importing Intermediate CA certificate ...${RESET}"
keytool -importcert -alias intermediateCA -keystore $SERVER_STORE -storepass $SERVER_STORE_PWD -noprompt -file intermediateCA.der
echo
echo "${H2}Creating server certificate signed by root CA...${RESET}"
keytool -certreq -alias server -dname 'CN=localhost' -keystore $SERVER_STORE -storepass $SERVER_STORE_PWD | \
  keytool -gencert -alias rootCA -keystore $TRUSTED_CA_STORE -storepass $TRUSTED_CA_STORE_PWD \
          -validity $VALIDITY \
          -ext BasicConstraints=ca:false \
          -ext KeyUsage:critical=digitalSignature,keyAgreement \
          -ext ExtendedkeyUsage=serverAuth | \
    keytool -importcert -alias server_signed_by_root -keystore $SERVER_STORE -storepass $SERVER_STORE_PWD
echo
echo "${H2}Creating server certificate signed by intermediate CA...${RESET}"
keytool -certreq -alias server -dname 'CN=Server signed with Intermediate CA' -ext san=dns:localhost -keystore $SERVER_STORE -storepass $SERVER_STORE_PWD | \
  keytool -gencert -alias intermediateCA -keystore $TRUSTED_CA_STORE -storepass $TRUSTED_CA_STORE_PWD \
          -validity $VALIDITY \
          -ext BasicConstraints=ca:false \
          -ext KeyUsage:critical=digitalSignature,keyAgreement \
          -ext ExtendedkeyUsage=serverAuth \
          -ext san=dns:localhost | \
    keytool -importcert -alias server_signed_by_intermediate_ca -keystore $SERVER_STORE -storepass $SERVER_STORE_PWD

echo
echo "${H1}Client Keystore : ${RESET}"
echo "${H1}==================${RESET}"
echo "${H2}Creating client key and self-signed certificate with expected CN...${RESET}"
keytool -genkeypair -alias client -keyalg EC -dname 'CN=leshan_integration_test' \
        -validity $VALIDITY \
        -storetype $DEFAULT_STORE_TYPE \
        -ext BasicConstraints=ca:false \
        -ext KeyUsage:critical=digitalSignature,keyAgreement \
        -ext ExtendedkeyUsage=clientAuth \
        -keypass $CLIENT_STORE_PWD -keystore $CLIENT_STORE -storepass $CLIENT_STORE_PWD
keytool -exportcert -alias client -keystore $CLIENT_STORE -storepass $CLIENT_STORE_PWD | \
  keytool -importcert -alias client_self_signed -keystore $CLIENT_STORE -storepass $CLIENT_STORE_PWD -noprompt
echo
echo "${H2}Import root certificate just to be able to sign certificate ...${RESET}"
keytool -importcert -alias rootCA -keystore $CLIENT_STORE -storepass $CLIENT_STORE_PWD -noprompt -file rootCA.der
echo
echo "${H2}Creating client certificate signed by root CA with expected CN...${RESET}"
keytool -certreq -alias client -keystore $CLIENT_STORE -storepass $CLIENT_STORE_PWD | \
  keytool -gencert -alias rootCA -keystore $TRUSTED_CA_STORE -storepass $TRUSTED_CA_STORE_PWD \
          -validity $VALIDITY \
          -ext BasicConstraints=ca:false \
          -ext KeyUsage:critical=digitalSignature,keyAgreement \
          -ext ExtendedkeyUsage=clientAuth | \
    keytool -importcert -alias client_signed_by_root -keystore $CLIENT_STORE -storepass $CLIENT_STORE_PWD -noprompt
echo
echo "${H2}Creating client certificate signed by root CA with bad/unexpected CN...${RESET}"
keytool -certreq -alias client -dname 'CN=leshan_client_with_bad_cn' -keystore $CLIENT_STORE -storepass $CLIENT_STORE_PWD | \
  keytool -gencert -alias rootCA -keystore $TRUSTED_CA_STORE -storepass $TRUSTED_CA_STORE_PWD \
          -validity $VALIDITY \
          -ext BasicConstraints=ca:false \
          -ext KeyUsage:critical=digitalSignature,keyAgreement \
          -ext ExtendedkeyUsage=clientAuth | \
    keytool -importcert -alias client_bad_cn -keystore $CLIENT_STORE -storepass $CLIENT_STORE_PWD -noprompt
echo
echo "${H2}Creating client certificate signed by untrusted root CA with expected CN...${RESET}"
keytool -certreq -alias client -keystore $CLIENT_STORE -storepass $CLIENT_STORE_PWD | \
  keytool -gencert -alias untrustedRootCA -keystore $UNKNOWN_CA_STORE -storepass $UNKNOWN_CA_STORE_PWD \
          -validity $VALIDITY \
          -ext BasicConstraints=ca:false \
          -ext KeyUsage:critical=digitalSignature,keyAgreement \
          -ext ExtendedkeyUsage=clientAuth | \
    keytool -importcert -alias client_not_trusted -keystore $CLIENT_STORE -storepass $CLIENT_STORE_PWD -noprompt
