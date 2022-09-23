/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
package org.eclipse.leshan.server.californium;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.math.BigInteger;
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

import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumServerEndpointsProvider;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumServerEndpointsProvider.Builder;
import org.eclipse.leshan.server.californium.endpoint.coap.CoapServerProtocolProvider;
import org.eclipse.leshan.server.californium.endpoint.coaps.CoapsServerProtocolProvider;
import org.eclipse.leshan.server.security.InMemorySecurityStore;
import org.junit.Before;
import org.junit.Test;

public class LeshanServerBuilderTest {

    private LeshanServerBuilder builder;
    private LeshanServer server;
    private PublicKey publicKey;
    private PrivateKey privateKey;

    public LeshanServerBuilderTest() {
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
            KeySpec publicKeySpec = new ECPublicKeySpec(
                    new ECPoint(new BigInteger(1, publicX), new BigInteger(1, publicY)), parameterSpec);
            KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(1, privateS), parameterSpec);

            // Get keys
            publicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            privateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidParameterSpecException e) {
            throw new IllegalStateException("Unable to create private/public keys for tests");
        }
    }

    @Before
    public void start() {
        builder = new LeshanServerBuilder();
    }

    @Test
    public void create_server_with_default_californiumEndpointsProvider() {
        builder.setEndpointsProvider(new CaliforniumServerEndpointsProvider());
        server = builder.build();

        assertEquals(1, server.getEndpoints().size());
        assertEquals(Protocol.COAP, server.getEndpoints().get(0).getProtocol());
    }

    @Test
    public void create_server_without_securityStore() {
        Builder endpointsBuilder = new CaliforniumServerEndpointsProvider.Builder(new CoapServerProtocolProvider(),
                new CoapsServerProtocolProvider());
        builder.setEndpointsProvider(endpointsBuilder.build());
        server = builder.build();

        assertEquals(1, server.getEndpoints().size());
        assertEquals(Protocol.COAP, server.getEndpoints().get(0).getProtocol());
        assertNull(server.getSecurityStore());
    }

    @Test
    public void create_server_with_securityStore() {
        Builder endpointsBuilder = new CaliforniumServerEndpointsProvider.Builder(new CoapServerProtocolProvider(),
                new CoapsServerProtocolProvider());
        builder.setEndpointsProvider(endpointsBuilder.build());
        builder.setSecurityStore(new InMemorySecurityStore());
        server = builder.build();

        assertEquals(2, server.getEndpoints().size());
        assertEquals(Protocol.COAP, server.getEndpoints().get(0).getProtocol());
        assertEquals(Protocol.COAPS, server.getEndpoints().get(1).getProtocol());
        assertNotNull(server.getSecurityStore());
    }

    @Test
    public void create_server_with_coaps_only() {
        Builder endpointsBuilder = new CaliforniumServerEndpointsProvider.Builder(new CoapsServerProtocolProvider());
        builder.setEndpointsProvider(endpointsBuilder.build());
        builder.setSecurityStore(new InMemorySecurityStore());
        server = builder.build();

        assertEquals(1, server.getEndpoints().size());
        assertEquals(Protocol.COAPS, server.getEndpoints().get(0).getProtocol());
        assertNotNull(server.getSecurityStore());
    }

    @Test
    public void create_server_without_psk_cipher() {
        Builder endpointsBuilder = new CaliforniumServerEndpointsProvider.Builder(new CoapsServerProtocolProvider());

        Configuration coapConfiguration = endpointsBuilder.createDefaultConfiguration();
        coapConfiguration.setAsList(DtlsConfig.DTLS_CIPHER_SUITES, CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8);
        endpointsBuilder.setConfiguration(coapConfiguration);

        builder.setPrivateKey(privateKey);
        builder.setPublicKey(publicKey);
        builder.setSecurityStore(new InMemorySecurityStore());
        builder.setEndpointsProvider(endpointsBuilder.build());

        server = builder.build();

        assertEquals(1, server.getEndpoints().size());
        assertEquals(Protocol.COAPS, server.getEndpoints().get(0).getProtocol());
    }
}
