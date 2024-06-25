#!/bin/bash

# Keystore parameters
CERTIFICATE_STORE=certificates.jks
CERTIFICATE_STORE_PWD=secret

VALIDITY=36500 #days

# Color output stuff
red=`tput setaf 1`
green=`tput setaf 2`
blue=`tput setaf 4`
bold=`tput bold`
H1=${green}${bold} 
H2=${blue} 
RESET=`tput sgr0`

# Generation of the keystore needed for Leshan core tests.
echo "${H1}Certificate Store : ${RESET}"
echo "${H1}===================${RESET}"
echo "${H2}Creating the trusted root CA key and certificate...${RESET}"
keytool -genkeypair -alias rootCA -keyalg EC -dname 'CN=Leshan root CA' \
        -validity $VALIDITY \
        -ext BasicConstraints:critical=ca:true \
        -ext KeyUsage:critical=keyCertSign,cRLSign \
        -keypass $CERTIFICATE_STORE_PWD -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD
echo
echo "${H2}Creating server key and self-signed certificate ...${RESET}"
keytool -genkeypair -alias server -keyalg EC -dname 'CN=server.mydomain.com' \
        -ext BasicConstraints=ca:false \
        -ext KeyUsage:critical=digitalSignature,keyAgreement \
        -ext ExtendedkeyUsage=serverAuth \
        -validity $VALIDITY \
        -keypass $CERTIFICATE_STORE_PWD -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD
echo
echo "${H2}Creating server key and self-signed certificate with SAN...${RESET}"
keytool -genkeypair -alias server_with_san -keyalg EC -dname 'CN=Leshan server' \
        -ext BasicConstraints=ca:false \
        -ext KeyUsage:critical=digitalSignature,keyAgreement \
        -ext ExtendedkeyUsage=serverAuth \
        -ext SAN=dns:server.mydomain.com,ip:192.168.1.42 \
        -validity $VALIDITY \
        -keypass $CERTIFICATE_STORE_PWD -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD
echo
echo "${H2}Creating server key and self-signed certificate with IPv6 address...${RESET}"
keytool -genkeypair -alias server_with_ipv6 -keyalg EC -dname 'CN=Leshan server' \
        -ext BasicConstraints=ca:false \
        -ext KeyUsage:critical=digitalSignature,keyAgreement \
        -ext ExtendedkeyUsage=serverAuth \
        -ext SAN=dns:server.mydomain.com,ip:2001:0db8:85a3:0000:0000:8a2e:0370:7334 \
        -validity $VALIDITY \
        -keypass $CERTIFICATE_STORE_PWD -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD
echo
echo "${H2}Creating server key and self-signed certificate for localhost address...${RESET}"
keytool -genkeypair -alias localhost -keyalg EC -dname 'CN=Leshan server' \
        -ext BasicConstraints=ca:false \
        -ext KeyUsage:critical=digitalSignature,keyAgreement \
        -ext ExtendedkeyUsage=serverAuth \
        -ext SAN=dns:localhost,ip:127.0.0.1,ip:::1 \
        -validity $VALIDITY \
        -keypass $CERTIFICATE_STORE_PWD -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD
# Note: Disabled for now as this fails with:
# keytool error: java.lang.RuntimeException: java.io.IOException: DNSName components must begin with a letter
# one would need newer jdk that is with forbiddive license: https://bugs.openjdk.java.net/browse/JDK-8213952
#echo
#echo "${H2}Creating server key and self-signed wildcard certificate...${RESET}"
#keytool -genkeypair -alias wildcard -keyalg EC -dname 'CN=My Domain Servers' \
#        -ext BasicConstraints=ca:false \
#        -ext KeyUsage:critical=digitalSignature,keyAgreement \
#        -ext ExtendedkeyUsage=serverAuth \
#        -ext SAN=dns:*.mydomain.com \
#        -validity $VALIDITY \
#        -keypass $CERTIFICATE_STORE_PWD -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD
echo
echo "${H2}Creating server certificate signed by root CA...${RESET}"
keytool -certreq -alias server \
        -keypass $CERTIFICATE_STORE_PWD -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD | \
  keytool -gencert -alias rootCA -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD \
          -validity $VALIDITY \
          -ext BasicConstraints=ca:false \
          -ext KeyUsage:critical=digitalSignature,keyAgreement \
          -ext ExtendedkeyUsage=serverAuth | \
    keytool -importcert -alias server -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD
echo
echo "${H2}Creating server certificate with SAN signed by root CA...${RESET}"
keytool -certreq -alias server_with_san \
        -ext SAN=dns:server.mydomain.com,ip:192.168.1.42 \
        -keypass $CERTIFICATE_STORE_PWD -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD | \
  keytool -gencert -alias rootCA -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD \
          -validity $VALIDITY \
          -ext BasicConstraints=ca:false \
          -ext KeyUsage:critical=digitalSignature,keyAgreement \
          -ext ExtendedkeyUsage=serverAuth \
          -ext SAN=dns:server.mydomain.com,ip:192.168.1.42 | \
    keytool -importcert -alias server_with_san -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD
echo
echo "${H2}Creating server certificate with SAN signed by root CA...${RESET}"
keytool -certreq -alias server_with_san \
        -ext SAN=dns:server.mydomain.com,ip:192.168.1.42 \
        -keypass $CERTIFICATE_STORE_PWD -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD | \
  keytool -gencert -alias rootCA -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD \
          -validity $VALIDITY \
          -ext BasicConstraints=ca:false \
          -ext KeyUsage:critical=digitalSignature,keyAgreement \
          -ext ExtendedkeyUsage=serverAuth \
          -ext SAN=dns:server.mydomain.com,ip:192.168.1.42 | \
    keytool -importcert -alias server_with_san -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD
echo
echo "${H2}Creating server certificate with IPv6 signed by root CA...${RESET}"
keytool -certreq -alias server_with_ipv6 \
        -ext SAN=dns:server.mydomain.com,ip:192.168.1.42 \
        -keypass $CERTIFICATE_STORE_PWD -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD | \
  keytool -gencert -alias rootCA -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD \
          -validity $VALIDITY \
          -ext BasicConstraints=ca:false \
          -ext KeyUsage:critical=digitalSignature,keyAgreement \
          -ext ExtendedkeyUsage=serverAuth \
          -ext SAN=dns:server.mydomain.com,ip:2001:0db8:85a3:0000:0000:8a2e:0370:7334 | \
    keytool -importcert -alias server_with_ipv6 -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD
echo
echo "${H2}Creating server certificate for localhost signed by root CA...${RESET}"
keytool -certreq -alias localhost \
        -ext SAN=dns:localhost,ip:127.0.0.1,ip:::1 \
        -keypass $CERTIFICATE_STORE_PWD -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD | \
  keytool -gencert -alias rootCA -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD \
          -validity $VALIDITY \
          -ext BasicConstraints=ca:false \
          -ext KeyUsage:critical=digitalSignature,keyAgreement \
          -ext ExtendedkeyUsage=serverAuth \
          -ext SAN=dns:localhost,ip:127.0.0.1,ip:::1 | \
    keytool -importcert -alias localhost -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD
# Note: Disabled due to keytool bug
#echo
#echo "${H2}Creating server certificate with wildcard signed by root CA...${RESET}"
#keytool -certreq -alias wildcard \
#        -ext SAN=dns:*.mydomain.com \
#        -keypass $CERTIFICATE_STORE_PWD -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD | \
#  keytool -gencert -alias rootCA -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD \
#          -validity $VALIDITY \
#          -ext BasicConstraints=ca:false \
#          -ext KeyUsage:critical=digitalSignature,keyAgreement \
#          -ext ExtendedkeyUsage=serverAuth \
#          -ext SAN=dns:*.mydomain.com | \
#    keytool -importcert -alias wildcard -keystore $CERTIFICATE_STORE -storepass $CERTIFICATE_STORE_PWD
