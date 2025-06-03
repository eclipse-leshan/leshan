###############################
### create root certificate
###############################
# create root keys
openssl ecparam -out root_keys.pem -name prime256v1 -genkey
# create root certificate (in DER and PEM encoding) 
openssl req -x509 -new -key root_keys.pem -sha256 -days 36500 \
                       -subj '/CN=root' \
                       -addext "keyUsage = keyCertSign,cRLSign" \
                       -outform PEM -out root_cert.pem
openssl x509 -inform PEM -in root_cert.pem  -outform DER -out root_cert.der
                       
###############################
### create client certificate
###############################
# create client keys
openssl ecparam -out client_keys.pem -name prime256v1 -genkey
 
# create CSR
openssl req -new -key keys.pem \
                       -subj '/CN=leshan_client_test/C=FR' \
                       -addext "keyUsage = digitalSignature,keyAgreement" \
                       -addext "extendedKeyUsage = serverAuth, clientAuth" \
                       -out client_csr.pem
                       
# create client certificate (in DER and PEM encoding)
openssl x509 -req -in client_csr.pem -CA root_cert.pem -CAkey root_keys.pem -CAcreateserial -days 36500 \
                  -outform DER -out client_cert.der
openssl x509 -inform DER -in client_cert.der -out client_cert.pem

# Get private key 
openssl pkcs8 -topk8 -inform PEM -outform DER -in client_keys.pem -out client_prik.der -nocrypt

###############################
### create client certificate chain
###############################
# Cert chain in PEM encoding 
cat client_cert.pem root_cert.pem> client_chain.pem
# Cert chain in DER encoding
cat client_cert.der root_cert.der > client_chain.der

###############################
### create self-signed server certificate
###############################
openssl ecparam -out server_keys.pem -name prime256v1 -genkey 
openssl pkcs8 -topk8 -inform PEM -outform DER -in server_keys.pem -out server_prik.der -nocrypt
openssl req -x509 -new -key server_keys.pem -sha256 -days 36500 \
                       -subj '/CN=YOUR_COMMON_NAME/C=FR' \
                       -addext "keyUsage = digitalSignature,keyAgreement" \
                       -addext "extendedKeyUsage = serverAuth, clientAuth" \
                       -outform DER -out server_cert.der

################################################################
###             create crappy files with garbage data        ### 
################################################################

###############################
### create certificate with data just before
###############################
# PEM Cert with garbage data after
echo -n 'some more data' >> client_cert_with_garbage_before.pem
cat client_cert.pem >> client_cert_with_garbage_before.pem
# DER Cert with garbage data after
echo -n -e '\xde\xad\xbe\xefGARBAGE' >> client_cert_with_garbage_before.der
cat client_cert.der >> client_cert_with_garbage_before.der

###############################
### create certificate with data just after
###############################
# PEM Cert with garbage data after
cp client_cert.pem client_cert_with_garbage_after.pem
echo -n 'some more data' >> client_cert_with_garbage_after.pem
# DER Cert with garbage data after
cp client_cert.der client_cert_with_garbage_after.der
echo -n -e '\xde\xad\xbe\xefGARBAGE' >> client_cert_with_garbage_after.der

###############################
### create certificate chain with data just before
###############################
# Cert chain in PEM encoding with garbage data after
echo -n 'some more data' >> client_chain_with_garbage_before.pem
cat client_chain.pem >> client_chain_with_garbage_before.pem
# Cert chain in DER encoding with garbage data after
echo -n -e '\xde\xad\xbe\xefGARBAGE' >> client_chain_with_garbage_before.der
cat client_chain.der >> client_chain_with_garbage_before.der

###############################
### create certificate chain with data between 2 certificate
###############################
# Cert chain in PEM encoding with garbage data after
cat client_cert.pem >  client_chain_with_garbage_between.pem
echo -n 'some more data' >> client_chain_with_garbage_between.pem
cat root_cert.pem >>  client_chain_with_garbage_between.pem
# Cert chain in DER encoding with garbage data after
cat client_cert.der >  client_chain_with_garbage_between.der
echo -n -e '\xde\xad\xbe\xefGARBAGE' >> client_chain_with_garbage_between.der
cat root_cert.der >>  client_chain_with_garbage_between.der

###############################
### create certificate chain with data just after
###############################
# Cert chain in PEM encoding with garbage data after
cp client_chain.pem client_chain_with_garbage_after.pem
echo -n 'some more data' >> client_chain_with_garbage_after.pem
# Cert chain in DER encoding with garbage data after
cp client_chain.der client_chain_with_garbage_after.der
echo -n -e '\xde\xad\xbe\xefGARBAGE' >> client_chain_with_garbage_after.der