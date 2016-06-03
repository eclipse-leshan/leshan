Key Stores passwords
====================

### Client Key Store
* File: clientKeyStore.jks
* Password: client
* Contains: client keys and certificate, signed by client CA

### Server Key Store
* File: serverKeyStore.jks
* Password: server
* Contains: server keys and certificate, signed by server CA


The following instructions are from [Scandium documentation](https://github.com/eclipse/californium/blob/master/scandium-core/README.md) and describe the procedure to create the certificates and key stores in this credentials folder.

In the following "entity" can be replaced by "client" of "server" for the set of instructions.


Create a self-signed certificate
================================

The client CA and server CA certificates are created with this method.

```
openssl ecparam -name prime256v1 -genkey -out entityCA.key
openssl req -new -key entityCA.key -x509 -sha256 -days 365 -out entityCA.crt
```


Add root CAs to Java's trusted CAs
==================================

This is done for client CA and server CA.

```
keytool -importcert -alias californium -file entityCA.crt -keystore "$JAVA_HOME/jre/lib/security/cacerts"
```


Create client and server certificate and key store
==================================================

This is done for the LWM2M client and server.

```
keytool -genkeypair -alias entity -keyalg EC -keystore entityKeyStore.jks -sigalg SHA256withECDSA -validity 365
keytool -certreq -alias entity -keystore entityKeyStore.jks -file entity.csr
openssl x509 -req -in entity.csr -CA entityCA.crt -CAkey entityCA.key -out entity.crt -sha256 -days 365 -CAcreateserial
keytool -importcert -alias entityCA -file entityCA.crt -keystore entityKeyStore.jks -trustcacerts
keytool -importcert -alias entity -file entity.crt -keystore entityKeyStore.jks -trustcacerts
```
