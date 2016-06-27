/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import org.eclipse.leshan.LwM2mId;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerSecurity;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager;
import org.eclipse.leshan.server.bootstrap.BootstrapStore;
import org.eclipse.leshan.server.bootstrap.SecurityMode;
import org.eclipse.leshan.server.californium.impl.LwM2mBootstrapServerImpl;
import org.eclipse.leshan.server.impl.BootstrapSessionManagerImpl;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.util.Charsets;
import org.eclipse.leshan.util.Hex;

/**
 * Helper for running a server and executing a client against it.
 * 
 */
public class BootstrapIntegrationTestHelper extends IntegrationTestHelper {

    // public static final String SECURE_ENDPOINT_IDENTIFIER = "secureclient";
    public static final String SECURE_PSK_ID = "Client_identity";
    public static final byte[] SECURE_PSK_KEY = Hex.decodeHex("73656372657450534b".toCharArray());

    LwM2mBootstrapServerImpl bootstrapServer;

    public void createBootstrapServer(BootstrapSecurityStore securityStore) {

        BootstrapStore bsStore = new BootstrapStore() {
            @Override
            public BootstrapConfig getBootstrap(String endpoint) {

                BootstrapConfig bsConfig = new BootstrapConfig();

                // security for BS server
                ServerSecurity bsSecurity = new ServerSecurity();
                bsSecurity.serverId = 1111;
                bsSecurity.bootstrapServer = true;
                bsSecurity.uri = "coap://" + bootstrapServer.getNonSecureAddress().getHostString() + ":"
                        + bootstrapServer.getNonSecureAddress().getPort();
                bsSecurity.securityMode = SecurityMode.NO_SEC;
                bsConfig.security.put(0, bsSecurity);

                // security for DM server
                ServerSecurity dmSecurity = new ServerSecurity();
                dmSecurity.uri = "coap://" + server.getNonSecureAddress().getHostString() + ":"
                        + server.getNonSecureAddress().getPort();
                dmSecurity.serverId = 2222;
                dmSecurity.securityMode = SecurityMode.NO_SEC;
                bsConfig.security.put(1, dmSecurity);

                // DM server
                ServerConfig dmConfig = new ServerConfig();
                dmConfig.shortId = 2222;
                bsConfig.servers.put(0, dmConfig);

                return bsConfig;
            }
        };

        if (securityStore == null) {
            securityStore = dummyBsSecurityStore();
        }

        BootstrapSessionManager bsSessionManager = new BootstrapSessionManagerImpl(securityStore);

        bootstrapServer = new LwM2mBootstrapServerImpl(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0),
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), bsStore, securityStore, bsSessionManager);
    }

    public void createClient() {
        ObjectsInitializer initializer = new ObjectsInitializer();

        // set only the bootstrap server instance (security)
        String bsUrl = "coap://" + bootstrapServer.getNonSecureAddress().getHostString() + ":"
                + bootstrapServer.getNonSecureAddress().getPort();

        Security security = new Security(bsUrl, true, 3, new byte[0], new byte[0], new byte[0], 12345);

        initializer.setInstancesForObject(LwM2mId.SECURITY, security);

        initializer.setInstancesForObject(LwM2mId.DEVICE,
                new Device("Eclipse Leshan", IntegrationTestHelper.MODEL_NUMBER, "12345", "U"));
        List<LwM2mObjectEnabler> objects = initializer.createMandatory();
        objects.add(initializer.create(2));

        LeshanClientBuilder builder = new LeshanClientBuilder(IntegrationTestHelper.ENDPOINT_IDENTIFIER);
        builder.setObjects(objects);
        client = builder.build();
    }

    public void createPSKClient(String pskIdentity, byte[] pskKey) {
        ObjectsInitializer initializer = new ObjectsInitializer();

        String bsUrl = "coaps://" + bootstrapServer.getSecureAddress().getHostString() + ":"
                + bootstrapServer.getSecureAddress().getPort();

        byte[] pskId = pskIdentity.getBytes(Charsets.UTF_8);

        Security security = Security.pskBootstrap(bsUrl, pskId, pskKey);

        initializer.setInstancesForObject(LwM2mId.SECURITY, security);

        initializer.setInstancesForObject(LwM2mId.DEVICE,
                new Device("Eclipse Leshan", IntegrationTestHelper.MODEL_NUMBER, "12345", "U"));
        List<LwM2mObjectEnabler> objects = initializer.createMandatory();
        objects.add(initializer.create(2));

        LeshanClientBuilder builder = new LeshanClientBuilder(ENDPOINT_IDENTIFIER);
        builder.setObjects(objects);
        client = builder.build();
    }

    public static BootstrapSecurityStore bsSecurityStore() {

        return new BootstrapSecurityStore() {
            @Override
            public SecurityInfo getByIdentity(String identity) {
                if (BootstrapIntegrationTestHelper.SECURE_PSK_ID.equals(identity)) {
                    return pskSecurityInfo();
                } else {
                    return null;
                }
            }

            @Override
            public List<SecurityInfo> getAllByEndpoint(String endpoint) {
                if (IntegrationTestHelper.ENDPOINT_IDENTIFIER.equals(endpoint)) {
                    SecurityInfo info = pskSecurityInfo();
                    return Arrays.asList(info);
                } else {
                    return Arrays.asList();
                }

            }

        };
    }

    public static SecurityInfo pskSecurityInfo() {
        SecurityInfo info = SecurityInfo.newPreSharedKeyInfo(BootstrapIntegrationTestHelper.ENDPOINT_IDENTIFIER,
                BootstrapIntegrationTestHelper.SECURE_PSK_ID, BootstrapIntegrationTestHelper.SECURE_PSK_KEY);
        return info;
    }

    private BootstrapSecurityStore dummyBsSecurityStore() {
        return new BootstrapSecurityStore() {

            @Override
            public SecurityInfo getByIdentity(String identity) {
                return null;
            }

            @Override
            public List<SecurityInfo> getAllByEndpoint(String endpoint) {
                return null;
            }
        };
    }

}
