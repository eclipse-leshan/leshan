/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.leshan.client.californium.X509Util;
import org.eclipse.leshan.core.oscore.AeadAlgorithm;
import org.eclipse.leshan.core.oscore.HkdfAlgorithm;
import org.eclipse.leshan.core.util.Hex;

public class Credentials {

    public static final String GOOD_PSK_ID = "Good_Client_identity";
    public static final byte[] GOOD_PSK_KEY = Hex.decodeHex("73656372657450534b".toCharArray());
    public static final String GOOD_ENDPOINT = "good_endpoint";
    public static final String BAD_PSK_ID = "Bad_Client_identity";
    public static final byte[] BAD_PSK_KEY = Hex.decodeHex("010101010101010101".toCharArray());
    public static final String BAD_ENDPOINT = "bad_endpoint";

    public static final byte[] OSCORE_MASTER_SECRET = Hex.decodeHex("1234567890".toCharArray());
    public static final byte[] OSCORE_MASTER_SALT = Hex.decodeHex("0987654321".toCharArray());
    public static final byte[] OSCORE_SENDER_ID = Hex.decodeHex("ABCDEF".toCharArray());
    public static final byte[] OSCORE_RECIPIENT_ID = Hex.decodeHex("FEDCBA".toCharArray());

    public static final byte[] OSCORE_BOOTSTRAP_MASTER_SECRET = Hex.decodeHex("BB1234567890".toCharArray());
    public static final byte[] OSCORE_BOOTSTRAP_MASTER_SALT = Hex.decodeHex("BB0987654321".toCharArray());
    public static final byte[] OSCORE_BOOTSTRAP_SENDER_ID = Hex.decodeHex("BBABCDEF".toCharArray());
    public static final byte[] OSCORE_BOOTSTRAP_RECIPIENT_ID = Hex.decodeHex("BBFEDCBA".toCharArray());

    public static final AeadAlgorithm OSCORE_AEAD_ALGORITHM = AeadAlgorithm.AES_CCM_16_64_128;
    public static final HkdfAlgorithm OSCORE_HKDF_ALGORITHM = HkdfAlgorithm.HKDF_HMAC_SHA_256;

    public static final PublicKey clientPublicKey; // client public key used for RPK
    public static final PrivateKey clientPrivateKey; // client private key used for RPK
    public static final PublicKey serverPublicKey; // server public key used for RPK
    public static final PrivateKey serverPrivateKey; // server private key used for RPK
    public static final PublicKey anotherServerPublicKey; // server public key used for RPK
    public static final PrivateKey anotherServerPrivateKey; // server private key used for RPK

    // client private key used for X509
    public static final PrivateKey clientPrivateKeyFromCert;
    // client certificate signed by rootCA with a good CN (CN start by leshan_integration_test)
    public static final X509Certificate clientX509CertSignedByRoot;
    // client certificate signed by rootCA but with bad CN (CN does not start by leshan_integration_test)
    public static final X509Certificate clientX509CertWithBadCN;
    // client certificate self-signed with a good CN (CN start by leshan_integration_test)
    public static final X509Certificate clientX509CertSelfSigned;
    // client certificate signed by another CA (not rootCA) with a good CN (CN start by leshan_integration_test)
    public static final X509Certificate clientX509CertNotTrusted;

    // server private key used for X509
    public static final PrivateKey serverPrivateKeyFromCert;
    // server certificate signed by rootCA
    public static final X509Certificate serverX509CertSignedByRoot;
    // server certificate signed by intermediateCA
    public static final X509Certificate[] serverX509CertChainWithIntermediateCa;
    // self-signed server certificate
    public static final X509Certificate serverX509CertSelfSigned;

    // rootCA used by the server
    public static final X509Certificate rootCAX509Cert;

    // certificates trusted by the server (should contain rootCA)
    public static final X509Certificate[] trustedCertificatesByServer = new X509Certificate[1];
    // client's initial trust store
    public static final X509Certificate[] clientTrustStore;
    // client's initial empty trust store
    public static final List<X509Certificate> clientEmptyTrustStore = new ArrayList<>();

    static {
        // create client credentials
        try {
            // Create RPK
            KeyPair clientKeyPair = createEllipticCurveKeyPair( //
                    "89c048261979208666f2bfb188be1968fc9021c416ce12828c06f4e314c167b5", //
                    "cbf1eb7587f08e01688d9ada4be859137ca49f79394bad9179326b3090967b68", //
                    "e67b68d2aaeb6550f19d98cade3ad62b39532e02e6b422e1f7ea189dabaea5d2");
            clientPublicKey = clientKeyPair.getPublic();
            clientPrivateKey = clientKeyPair.getPrivate();

            // Get certificates from key store
            char[] clientKeyStorePwd = "client".toCharArray();
            KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (FileInputStream clientKeyStoreFile = new FileInputStream("./credentials/clientKeyStore.jks")) {
                clientKeyStore.load(clientKeyStoreFile, clientKeyStorePwd);
            }

            clientPrivateKeyFromCert = (PrivateKey) clientKeyStore.getKey("client", clientKeyStorePwd);
            clientX509CertSignedByRoot = (X509Certificate) clientKeyStore.getCertificate("client_signed_by_root");
            clientX509CertWithBadCN = (X509Certificate) clientKeyStore.getCertificate("client_bad_cn");
            clientX509CertSelfSigned = (X509Certificate) clientKeyStore.getCertificate("client_self_signed");
            clientX509CertNotTrusted = (X509Certificate) clientKeyStore.getCertificate("client_not_trusted");
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        // create server credentials
        try {
            // Create RPKs
            KeyPair serverKeyPair = createEllipticCurveKeyPair( //
                    "fcc28728c123b155be410fc1c0651da374fc6ebe7f96606e90d927d188894a73", //
                    "d2ffaa73957d76984633fc1cc54d0b763ca0559a9dff9706e9f4557dacc3f52a", //
                    "1dae121ba406802ef07c193c1ee4df91115aabd79c1ed7f4c0ef7ef6a5449400");
            serverPublicKey = serverKeyPair.getPublic();
            serverPrivateKey = serverKeyPair.getPrivate();

            serverKeyPair = createEllipticCurveKeyPair( //
                    "01b93a61485284414fd2750e4c1da584214f7a76eaf90498d6a13463b88b07cb", //
                    "482bfc089f8223a5d14206b12dbf69380eae376e5cd2ff212f393a8fec6384eb", //
                    "c97c734a03aa25b638360cc7f1c4fe3df454c050a28cd5d14d08cddec9e4ece0");
            anotherServerPublicKey = serverKeyPair.getPublic();
            anotherServerPrivateKey = serverKeyPair.getPrivate();

            // Get certificates from key store
            char[] serverKeyStorePwd = "server".toCharArray();
            KeyStore serverKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (FileInputStream serverKeyStoreFile = new FileInputStream("./credentials/serverKeyStore.jks")) {
                serverKeyStore.load(serverKeyStoreFile, serverKeyStorePwd);
            }

            serverPrivateKeyFromCert = (PrivateKey) serverKeyStore.getKey("server", serverKeyStorePwd);
            rootCAX509Cert = (X509Certificate) serverKeyStore.getCertificate("rootCA");
            serverX509CertSignedByRoot = (X509Certificate) serverKeyStore.getCertificate("server_signed_by_root");
            serverX509CertSelfSigned = (X509Certificate) serverKeyStore.getCertificate("server_self_signed");
            serverX509CertChainWithIntermediateCa = X509Util.asX509Certificates( //
                    serverKeyStore.getCertificate("server_signed_by_intermediate_ca"), //
                    serverKeyStore.getCertificate("intermediateCA"));
            trustedCertificatesByServer[0] = rootCAX509Cert;
            clientTrustStore = new X509Certificate[] { rootCAX509Cert };
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static KeyPair createEllipticCurveKeyPair(String pointX, String pointY, String pointS)
            throws NoSuchAlgorithmException, InvalidParameterSpecException, InvalidKeySpecException {
        // Get point values
        byte[] publicX = Hex.decodeHex(pointX.toCharArray());
        byte[] publicY = Hex.decodeHex(pointY.toCharArray());
        byte[] privateS = Hex.decodeHex(pointS.toCharArray());

        // Get Elliptic Curve Parameter spec for secp256r1
        AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
        algoParameters.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);

        // Create key specs
        KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(1, publicX), new BigInteger(1, publicY)),
                parameterSpec);
        KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(1, privateS), parameterSpec);

        // Get keys
        return new KeyPair(KeyFactory.getInstance("EC").generatePublic(publicKeySpec),
                KeyFactory.getInstance("EC").generatePrivate(privateKeySpec));
    }
}
