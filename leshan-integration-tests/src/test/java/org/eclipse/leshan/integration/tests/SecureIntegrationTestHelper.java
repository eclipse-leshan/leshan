/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.KeySpec;
import java.util.List;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.LwM2mId;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.impl.InMemorySecurityStore;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.util.Charsets;
import org.eclipse.leshan.util.Hex;

public class SecureIntegrationTestHelper extends IntegrationTestHelper {

    public static final String GOOD_PSK_ID = "Good_Client_identity";
    public static final byte[] GOOD_PSK_KEY = Hex.decodeHex("73656372657450534b".toCharArray());
    public static final String BAD_PSK_ID = "Bad_Client_identity";
    public static final byte[] BAD_PSK_KEY = Hex.decodeHex("010101010101010101".toCharArray());
    public static final String BAD_ENDPOINT = "bad_endpoint";

    public final PublicKey clientPublicKey;
    public final PrivateKey clientPrivateKey;
    public final PublicKey serverPublicKey;
    public final PrivateKey serverPrivateKey;

    public final PrivateKey clientPrivateKeyFromCert;
    public final PrivateKey serverPrivateKeyFromCert;
    public final X509Certificate[] clientX509CertChain = new X509Certificate[2];
    public final X509Certificate[] serverX509CertChain = new X509Certificate[2];
    public final Certificate[] trustedCertificates = new Certificate[2];
    public final X509Certificate clientCAX509Cert;
    public final X509Certificate serverCAX509Cert;

    public SecureIntegrationTestHelper() {
        // create client credentials
        try {
            // Get point values
            byte[] publicX = Hex
                    .decodeHex("89c048261979208666f2bfb188be1968fc9021c416ce12828c06f4e314c167b5".toCharArray());
            byte[] publicY = Hex
                    .decodeHex("cbf1eb7587f08e01688d9ada4be859137ca49f79394bad9179326b3090967b68".toCharArray());
            byte[] privateS = Hex
                    .decodeHex("e67b68d2aaeb6550f19d98cade3ad62b39532e02e6b422e1f7ea189dabaea5d2".toCharArray());

            // Get Elliptic Curve Parameter spec for secp256r1
            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
            algoParameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);

            // Create key specs
            KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                    parameterSpec);
            KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);

            // Get keys
            clientPublicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            clientPrivateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);

            // Get certificates from key store
            char[] clientKeyStorePwd = "client".toCharArray();
            FileInputStream clientKeyStoreFile = new FileInputStream("./credentials/clientKeyStore.jks");
            KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            clientKeyStore.load(clientKeyStoreFile, clientKeyStorePwd);

            clientPrivateKeyFromCert = (PrivateKey) clientKeyStore.getKey("client", clientKeyStorePwd);
            clientCAX509Cert = (X509Certificate) clientKeyStore.getCertificate("clientCA");
            clientX509CertChain[0] = (X509Certificate) clientKeyStore.getCertificate("client");
            clientX509CertChain[1] = clientCAX509Cert;
            trustedCertificates[0] = clientCAX509Cert;

        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        // create server credentials
        try {
            // Get point values
            byte[] publicX = Hex
                    .decodeHex("fcc28728c123b155be410fc1c0651da374fc6ebe7f96606e90d927d188894a73".toCharArray());
            byte[] publicY = Hex
                    .decodeHex("d2ffaa73957d76984633fc1cc54d0b763ca0559a9dff9706e9f4557dacc3f52a".toCharArray());
            byte[] privateS = Hex
                    .decodeHex("1dae121ba406802ef07c193c1ee4df91115aabd79c1ed7f4c0ef7ef6a5449400".toCharArray());

            // Get Elliptic Curve Parameter spec for secp256r1
            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
            algoParameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);

            // Create key specs
            KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                    parameterSpec);
            KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);

            // Get keys
            serverPublicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            serverPrivateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);

            // Get certificates from key store
            char[] serverKeyStorePwd = "server".toCharArray();
            FileInputStream serverKeyStoreFile = new FileInputStream("./credentials/serverKeyStore.jks");
            KeyStore serverKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            serverKeyStore.load(serverKeyStoreFile, serverKeyStorePwd);

            serverPrivateKeyFromCert = (PrivateKey) serverKeyStore.getKey("server", serverKeyStorePwd);
            serverCAX509Cert = (X509Certificate) serverKeyStore.getCertificate("serverCA");
            serverX509CertChain[0] = (X509Certificate) serverKeyStore.getCertificate("server");
            serverX509CertChain[1] = serverCAX509Cert;
            trustedCertificates[1] = serverCAX509Cert;

        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void createPSKClient() {
        ObjectsInitializer initializer = new ObjectsInitializer();
        initializer.setInstancesForObject(LwM2mId.SECURITY,
                Security.psk(
                        "coaps://" + server.getSecureAddress().getHostString() + ":"
                                + server.getSecureAddress().getPort(),
                        12345, GOOD_PSK_ID.getBytes(Charsets.UTF_8), GOOD_PSK_KEY));
        initializer.setInstancesForObject(LwM2mId.SERVER, new Server(12345, LIFETIME, BindingMode.U, false));
        initializer.setInstancesForObject(LwM2mId.DEVICE, new Device("Eclipse Leshan", MODEL_NUMBER, "12345", "U"));
        List<LwM2mObjectEnabler> objects = initializer.createMandatory();
        objects.add(initializer.create(2));

        InetSocketAddress clientAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        LeshanClientBuilder builder = new LeshanClientBuilder(getCurrentEndpoint());
        builder.setLocalAddress(clientAddress.getHostString(), clientAddress.getPort());
        builder.setObjects(objects);
        client = builder.build();
    }

    // TODO implement RPK support for client
    public void createRPKClient() {
        ObjectsInitializer initializer = new ObjectsInitializer();
        initializer.setInstancesForObject(LwM2mId.SECURITY,
                Security.rpk(
                        "coaps://" + server.getSecureAddress().getHostString() + ":"
                                + server.getSecureAddress().getPort(),
                        12345, clientPublicKey.getEncoded(), clientPrivateKey.getEncoded(),
                        serverPublicKey.getEncoded()));
        initializer.setInstancesForObject(LwM2mId.SERVER, new Server(12345, LIFETIME, BindingMode.U, false));
        initializer.setInstancesForObject(LwM2mId.DEVICE, new Device("Eclipse Leshan", MODEL_NUMBER, "12345", "U"));
        List<LwM2mObjectEnabler> objects = initializer.createMandatory();
        objects.add(initializer.create(2));

        InetSocketAddress clientAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        DtlsConnectorConfig.Builder config = new DtlsConnectorConfig.Builder(clientAddress);
        // TODO we should read the config from the security object
        // TODO no way to provide a dynamic config with the current scandium API
        config.setIdentity(clientPrivateKey, clientPublicKey);

        CoapServer coapServer = new CoapServer();
        coapServer.addEndpoint(new CoapEndpoint(new DTLSConnector(config.build()), NetworkConfig.getStandard()));

        LeshanClientBuilder builder = new LeshanClientBuilder(getCurrentEndpoint());
        builder.setLocalAddress(clientAddress.getHostString(), clientAddress.getPort());
        builder.setObjects(objects);
        client = builder.build();
    }

    // TODO implement X509 support for client
    public void createX509CertClient(PrivateKey privatekey, Certificate[] trustedCertificates) {
        ObjectsInitializer initializer = new ObjectsInitializer();
        // TODO security instance with certificate info
        initializer.setInstancesForObject(LwM2mId.SECURITY, Security.noSec(
                "coaps://" + server.getSecureAddress().getHostString() + ":" + server.getSecureAddress().getPort(),
                12345));
        initializer.setInstancesForObject(LwM2mId.SERVER, new Server(12345, LIFETIME, BindingMode.U, false));
        initializer.setInstancesForObject(LwM2mId.DEVICE, new Device("Eclipse Leshan", MODEL_NUMBER, "12345", "U"));
        List<LwM2mObjectEnabler> objects = initializer.createMandatory();
        objects.add(initializer.create(2));

        InetSocketAddress clientAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        DtlsConnectorConfig.Builder config = new DtlsConnectorConfig.Builder(clientAddress);
        // TODO we should read the config from the security object
        config.setIdentity(privatekey, clientX509CertChain, false);
        config.setTrustStore(trustedCertificates);

        CoapServer coapServer = new CoapServer();
        coapServer.addEndpoint(new CoapEndpoint(new DTLSConnector(config.build()), NetworkConfig.getStandard()));

        LeshanClientBuilder builder = new LeshanClientBuilder(getCurrentEndpoint());
        builder.setLocalAddress(clientAddress.getHostString(), clientAddress.getPort());
        builder.setObjects(objects);
        client = builder.build();
    }

    public void createServerWithRPK() {
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        builder.setLocalSecureAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        builder.setPublicKey(serverPublicKey);
        builder.setPrivateKey(serverPrivateKey);
        builder.setSecurityStore(new InMemorySecurityStore());

        server = builder.build();
    }

    public void createServerWithX509Cert(Certificate[] trustedCertificates) {
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        builder.setLocalSecureAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        builder.setPrivateKey(serverPrivateKeyFromCert);
        builder.setCertificateChain(serverX509CertChain);
        builder.setTrustedCertificates(trustedCertificates);
        builder.setSecurityStore(new InMemorySecurityStore());

        server = builder.build();
    }

    public PublicKey getServerPublicKey() {
        return serverPublicKey;
    }

    public EditableSecurityStore getSecurityStore() {
        return (EditableSecurityStore) server.getSecurityStore();
    }

    @Override
    public void dispose() {
        super.dispose();
        getSecurityStore().remove(getCurrentEndpoint());
        getSecurityStore().remove(BAD_ENDPOINT);
    }
}
