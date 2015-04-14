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

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
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
import org.eclipse.leshan.util.Hex;

public class SecureIntegrationTestHelper extends IntegrationTestHelper {

    public final String pskIdentity = "Client_identity";
    public final byte[] pskKey = Hex.decodeHex("73656372657450534b".toCharArray());
    public final PublicKey clientPublicKey;
    public final PrivateKey clientPrivateKey;
    public final PublicKey serverPublicKey;
    public final PrivateKey serverPrivateKey;

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
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidParameterSpecException e) {
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
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidParameterSpecException e) {
            throw new RuntimeException(e);
        }
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

        client = new LeshanClient(clientAddress, getServerSecureAddress(), coapServer,
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
        client = new LeshanClient(clientAddress, getServerSecureAddress(), coapServer,
                new ArrayList<LwM2mObjectEnabler>(objects));
    }

    public void createPSKandRPKClient() {
        ObjectsInitializer initializer = new ObjectsInitializer();
        List<ObjectEnabler> objects = initializer.create(2, 3);

        InetSocketAddress clientAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        DtlsConnectorConfig.Builder config = new DtlsConnectorConfig.Builder(clientAddress);
        config.setPskStore(new StaticPskStore(pskIdentity, pskKey));
        config.setIdentity(clientPrivateKey, clientPublicKey);

        CoapServer coapServer = new CoapServer();
        coapServer.addEndpoint(new CoAPEndpoint(new DTLSConnector(config.build()), NetworkConfig.getStandard()));

        client = new LeshanClient(clientAddress, getServerSecureAddress(), coapServer,
                new ArrayList<LwM2mObjectEnabler>(objects));
    }

    public void createServerWithRPK() {
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        builder.setLocalAddressSecure(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        builder.setSecurityRegistry(new SecurityRegistryImpl(serverPrivateKey, serverPublicKey) {
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
}
