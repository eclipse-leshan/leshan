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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
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

import javax.xml.bind.DatatypeConverter;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoAPEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.pskstore.StaticPskStore;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.impl.SecurityRegistryImpl;

public class SecureIntegrationTestHelper extends IntegrationTestHelper {

    public final String pskIdentity = "Client_identity";
    public final byte[] pskKey = DatatypeConverter.parseHexBinary("73656372657450534b");
    public final PublicKey clientPublicKey;
    public final PrivateKey clientPrivateKey;
    public final PublicKey serverPublicKey;
    public final PrivateKey serverPrivateKey;

    public final X509Certificate clientX509Cert, clientCAX509Cert, serverX509Cert, serverCAX509Cert;

    public SecureIntegrationTestHelper() {
        // create client credentials
        try {
            // Get point values
            byte[] publicX = DatatypeConverter
                    .parseHexBinary("89c048261979208666f2bfb188be1968fc9021c416ce12828c06f4e314c167b5");
            byte[] publicY = DatatypeConverter
                    .parseHexBinary("cbf1eb7587f08e01688d9ada4be859137ca49f79394bad9179326b3090967b68");
            byte[] privateS = DatatypeConverter
                    .parseHexBinary("e67b68d2aaeb6550f19d98cade3ad62b39532e02e6b422e1f7ea189dabaea5d2");

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

            // Get certificates
            // TODO handmade credentials -> automatically generated creds to delete after test
            clientCAX509Cert = createCertificate("credentials/clientCA.crt");
            clientX509Cert = createCertificate("credentials/client.crt");

        } catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidParameterSpecException
                | FileNotFoundException | CertificateException e) {
            throw new RuntimeException(e);
        }

        // create server credentials
        try {
            // Get point values
            byte[] publicX = DatatypeConverter
                    .parseHexBinary("fcc28728c123b155be410fc1c0651da374fc6ebe7f96606e90d927d188894a73");
            byte[] publicY = DatatypeConverter
                    .parseHexBinary("d2ffaa73957d76984633fc1cc54d0b763ca0559a9dff9706e9f4557dacc3f52a");
            byte[] privateS = DatatypeConverter
                    .parseHexBinary("1dae121ba406802ef07c193c1ee4df91115aabd79c1ed7f4c0ef7ef6a5449400");

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

            // Get certificates
            // TODO handmade credentials -> automatically generated creds to delete after test
            serverCAX509Cert = createCertificate("credentials/serverCA.crt");
            serverX509Cert = createCertificate("credentials/server.crt");

        } catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidParameterSpecException
                | FileNotFoundException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    private X509Certificate createCertificate(String certFilename) throws FileNotFoundException, CertificateException {
        FileInputStream fileInStream = new FileInputStream(certFilename);
        BufferedInputStream bufInStream = new BufferedInputStream(fileInStream);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        return (X509Certificate) cf.generateCertificate(bufInStream);
    }

    // TODO we need better API for secure client, maybe we need a builder like leshanServer.
    public void createPSKClient() {
        ObjectsInitializer initializer = new ObjectsInitializer();
        List<ObjectEnabler> objects = initializer.create(2, 3);

        InetSocketAddress clientAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        DtlsConnectorConfig.Builder config = new DtlsConnectorConfig.Builder(clientAddress);
        config.setPskStore(new StaticPskStore(pskIdentity, pskKey));

        CoapServer coapServer = new CoapServer();
        coapServer.addEndpoint(new CoAPEndpoint(new DTLSConnector(config.build()), NetworkConfig.getStandard()));

        client = new LeshanClient(clientAddress, server.getSecureAddress(), coapServer,
                new ArrayList<LwM2mObjectEnabler>(objects));
    }

    // TODO we need better API for secure client, maybe we need a builder like leshanServer.
    public void createRPKClient() {
        ObjectsInitializer initializer = new ObjectsInitializer();
        List<ObjectEnabler> objects = initializer.create(2, 3);

        InetSocketAddress clientAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        DtlsConnectorConfig.Builder config = new DtlsConnectorConfig.Builder(clientAddress);
        config.setIdentity(clientPrivateKey, clientPublicKey);

        CoapServer coapServer = new CoapServer();
        coapServer.addEndpoint(new CoAPEndpoint(new DTLSConnector(config.build()), NetworkConfig.getStandard()));
        client = new LeshanClient(clientAddress, server.getSecureAddress(), coapServer,
                new ArrayList<LwM2mObjectEnabler>(objects));
    }

    // TODO we need better API for secure client, maybe we need a builder like leshanServer.
    public void createX509CertClient() {
        try {
            // TODO configurable pwd and path to keystore
            char[] keyStorePwd = "client".toCharArray();
            X509Certificate[] clientX509CertChain = { clientX509Cert, clientCAX509Cert };
            X509Certificate[] trustedCertificates = { clientCAX509Cert, serverCAX509Cert };

            FileInputStream fileInStream = new FileInputStream("./credentials/clientKeyStore.jks");

            KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            clientKeyStore.load(fileInStream, keyStorePwd);

            PrivateKey clientPrivKey = (PrivateKey) clientKeyStore.getKey("client", keyStorePwd);

            ObjectsInitializer initializer = new ObjectsInitializer();
            List<ObjectEnabler> objects = initializer.create(2, 3);

            InetSocketAddress clientAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
            DtlsConnectorConfig.Builder config = new DtlsConnectorConfig.Builder(clientAddress);
            config.setIdentity(clientPrivKey, clientX509CertChain, false);
            config.setTrustStore(trustedCertificates);

            CoapServer coapServer = new CoapServer();
            coapServer.addEndpoint(new CoAPEndpoint(new DTLSConnector(config.build()), NetworkConfig.getStandard()));
            client = new LeshanClient(clientAddress, server.getSecureAddress(), coapServer,
                    new ArrayList<LwM2mObjectEnabler>(objects));
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException
                | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

    // TODO client with (psk + rpk + cert) and (psk + cert)?

    public void createPSKandRPKClient() {
        ObjectsInitializer initializer = new ObjectsInitializer();
        List<ObjectEnabler> objects = initializer.create(2, 3);

        InetSocketAddress clientAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        DtlsConnectorConfig.Builder config = new DtlsConnectorConfig.Builder(clientAddress);
        config.setPskStore(new StaticPskStore(pskIdentity, pskKey));
        config.setIdentity(clientPrivateKey, clientPublicKey);

        CoapServer coapServer = new CoapServer();
        coapServer.addEndpoint(new CoAPEndpoint(new DTLSConnector(config.build()), NetworkConfig.getStandard()));

        client = new LeshanClient(clientAddress, server.getSecureAddress(), coapServer,
                new ArrayList<LwM2mObjectEnabler>(objects));
    }

    public void createServerWithRPK() {
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        builder.setLocalAddressSecure(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        builder.setSecurityRegistry(new SecurityRegistryImpl(serverPrivateKey, serverPublicKey, null) {
            // TODO we should separate SecurityRegistryImpl in 2 registries :
            // InMemorySecurityRegistry and PersistentSecurityRegistry
            @Override
            protected void loadFromFile() {
                // do not load From File
            }

            @Override
            protected void saveToFile() {
                // do not save to file
            }
        });

        server = builder.build();
    }

    public void createServerWithX509Cert() {
        try {
            // TODO configurable pwd and path to keystore
            char[] keyStorePwd = "server".toCharArray();
            X509Certificate[] serverX509CertChain = { serverX509Cert, serverCAX509Cert };
            X509Certificate[] trustedCertificates = { serverCAX509Cert, clientCAX509Cert };

            FileInputStream fileInStream = new FileInputStream("./credentials/serverKeyStore.jks");

            KeyStore serverKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            serverKeyStore.load(fileInStream, keyStorePwd);

            PublicKey serverPubKey = serverKeyStore.getCertificate("server").getPublicKey();
            PrivateKey serverPrivKey = (PrivateKey) serverKeyStore.getKey("server", keyStorePwd);

            LeshanServerBuilder builder = new LeshanServerBuilder();
            builder.setLocalAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            builder.setLocalAddressSecure(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));

            SecurityRegistryImpl securityRegistry = new SecurityRegistryImpl(serverPrivKey, serverPubKey,
                    serverX509CertChain) {
                // TODO we should separate SecurityRegistryImpl in 2 registries :
                // InMemorySecurityRegistry and PersistentSecurityRegistry
                @Override
                protected void loadFromFile() {
                    // do not load From File
                }

                @Override
                protected void saveToFile() {
                    // do not save to file
                }
            };
            securityRegistry.setTrustedCertificates(trustedCertificates);
            builder.setSecurityRegistry(securityRegistry);

            server = builder.build();
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException
                | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }
}
