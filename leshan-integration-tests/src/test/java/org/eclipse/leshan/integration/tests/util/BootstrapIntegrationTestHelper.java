/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.engine.DefaultRegistrationEngineFactory;
import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.BootstrapDiscoverResponse;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ACLConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerSecurity;
import org.eclipse.leshan.server.bootstrap.BootstrapConfiguration;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigurationStore;
import org.eclipse.leshan.server.bootstrap.BootstrapFailureCause;
import org.eclipse.leshan.server.bootstrap.BootstrapHandler;
import org.eclipse.leshan.server.bootstrap.BootstrapHandlerFactory;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager;
import org.eclipse.leshan.server.bootstrap.BootstrapUtil;
import org.eclipse.leshan.server.bootstrap.DefaultBootstrapHandler;
import org.eclipse.leshan.server.bootstrap.DefaultBootstrapSession;
import org.eclipse.leshan.server.bootstrap.DefaultBootstrapSessionManager;
import org.eclipse.leshan.server.bootstrap.LwM2mBootstrapRequestSender;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServerBuilder;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;

/**
 * Helper for running a server and executing a client against it.
 * 
 */
public class BootstrapIntegrationTestHelper extends SecureIntegrationTestHelper {

    public LeshanBootstrapServer bootstrapServer;
    public final PublicKey bootstrapServerPublicKey;
    public final PrivateKey bootstrapServerPrivateKey;
    public volatile DefaultBootstrapSession lastBootstrapSession;
    public volatile BootstrapDiscoverResponse lastDiscoverAnswer;

    public BootstrapIntegrationTestHelper() {
        super();

        // create bootstrap server credentials
        try {
            // Get point values
            byte[] publicX = Hex
                    .decodeHex("fb136894878a9696d45fdb04506b9eb49ddcfba71e4e1b4ce23d5c3ac382d6b4".toCharArray());
            byte[] publicY = Hex
                    .decodeHex("3deed825e808f8ed6a9a74ff6bd24e3d34b1c0c5fc253422f7febadbdc9cb9e6".toCharArray());
            byte[] privateS = Hex
                    .decodeHex("35a8303e67a7e99d06552a0f8f6c8f1bf91a174396f4fad6211ae227e890da11".toCharArray());

            // Get Elliptic Curve Parameter spec for secp256r1
            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
            algoParameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);

            // Create key specs
            KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                    parameterSpec);
            KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);

            // Get keys
            bootstrapServerPublicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            bootstrapServerPrivateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private LeshanBootstrapServerBuilder createBootstrapBuilder(BootstrapSecurityStore securityStore,
            BootstrapConfigurationStore bootstrapStore) {
        if (bootstrapStore == null) {
            bootstrapStore = unsecuredBootstrapStore();
        }

        if (securityStore == null) {
            securityStore = dummyBsSecurityStore();
        }

        LeshanBootstrapServerBuilder builder = new LeshanBootstrapServerBuilder();
        builder.setConfigStore(bootstrapStore);
        builder.setSecurityStore(securityStore);
        builder.setLocalAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        builder.setLocalSecureAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        builder.setPrivateKey(bootstrapServerPrivateKey);
        builder.setPublicKey(bootstrapServerPublicKey);
        builder.setSessionManager(new DefaultBootstrapSessionManager(securityStore) {

            @Override
            public BootstrapSession begin(BootstrapRequest request, Identity clientIdentity) {
                assertThat(request.getCoapRequest(), instanceOf(Request.class));
                return super.begin(request, clientIdentity);
            }

            @Override
            public void end(BootstrapSession bsSession) {
                lastBootstrapSession = (DefaultBootstrapSession) bsSession;
            }
        });

        return builder;
    }

    public void createBootstrapServer(BootstrapSecurityStore securityStore,
            BootstrapConfigurationStore bootstrapStore) {
        bootstrapServer = createBootstrapBuilder(securityStore, bootstrapStore).build();
    }

    public void createBootstrapServer(BootstrapSecurityStore securityStore, BootstrapConfigurationStore bootstrapStore,
            final BootstrapDiscoverRequest request) {
        LeshanBootstrapServerBuilder builder = createBootstrapBuilder(securityStore, bootstrapStore);

        // start bootstrap session by a bootstrap discover request
        builder.setBootstrapHandlerFactory(new BootstrapHandlerFactory() {

            @Override
            public BootstrapHandler create(BootstrapConfigurationStore store, LwM2mBootstrapRequestSender sender,
                    BootstrapSessionManager sessionManager) {
                return new DefaultBootstrapHandler(store, sender, sessionManager) {

                    @Override
                    protected void startBootstrap(final BootstrapSession session, final BootstrapConfiguration cfg) {
                        send(session, request, new SafeResponseCallback<BootstrapDiscoverResponse>(session) {

                            @Override
                            public void safeOnResponse(BootstrapDiscoverResponse response) {
                                lastDiscoverAnswer = response;
                                sendRequest(session, cfg, new ArrayList<>(cfg.getRequests()));
                            }
                        }, new SafeErrorCallback(session) {
                            @Override
                            public void safeOnError(Exception e) {
                                stopSession(session, BootstrapFailureCause.INTERNAL_SERVER_ERROR);
                            }
                        });
                    }
                };
            }
        });

        bootstrapServer = builder.build();
    }

    public void createBootstrapServer(BootstrapSecurityStore securityStore) {
        createBootstrapServer(securityStore, null);
    }

    public Security withoutSecurity() {
        // Create Security Object (with bootstrap server only)
        String bsUrl = "coap://" + bootstrapServer.getUnsecuredAddress().getHostString() + ":"
                + bootstrapServer.getUnsecuredAddress().getPort();
        return Security.noSecBootstap(bsUrl);
    }

    @Override
    public void createClient() {
        createClient(withoutSecurity(), null);
    }

    public void createClient(ContentFormat preferredContentFormat, final ContentFormat... supportedContentFormat) {
        createClient(withoutSecurity(), null, null, preferredContentFormat, supportedContentFormat);
    }

    public void createPSKClient(String pskIdentity, byte[] pskKey) {
        // Create Security Object (with bootstrap server only)
        String bsUrl = "coaps://" + bootstrapServer.getSecuredAddress().getHostString() + ":"
                + bootstrapServer.getSecuredAddress().getPort();
        byte[] pskId = pskIdentity.getBytes(StandardCharsets.UTF_8);
        Security security = Security.pskBootstrap(bsUrl, pskId, pskKey);

        createClient(security, null);
    }

    @Override
    public void createRPKClient() {
        String bsUrl = "coaps://" + bootstrapServer.getSecuredAddress().getHostString() + ":"
                + bootstrapServer.getSecuredAddress().getPort();
        Security security = Security.rpkBootstrap(bsUrl, clientPublicKey.getEncoded(), clientPrivateKey.getEncoded(),
                bootstrapServerPublicKey.getEncoded());

        createClient(security, null);
    }

    public void createClient(Security security, ObjectsInitializer initializer) {
        createClient(security, initializer, null, null);
    }

    @Override
    public void createClient(Map<String, String> additionalAttributes) {
        createClient(withoutSecurity(), null, additionalAttributes, null);
    }

    public void createClient(Security security, ObjectsInitializer initializer,
            Map<String, String> additionalAttributes, ContentFormat preferredContentFormat,
            final ContentFormat... supportedContentFormat) {
        if (initializer == null) {
            initializer = new TestObjectsInitializer();
        }

        // Initialize LWM2M Object Tree
        initializer.setInstancesForObject(LwM2mId.SECURITY, security);
        initializer.setInstancesForObject(LwM2mId.DEVICE,
                new Device("Eclipse Leshan", IntegrationTestHelper.MODEL_NUMBER, "12345"));
        initializer.setClassForObject(LwM2mId.SERVER, DummyInstanceEnabler.class);
        createClient(initializer, additionalAttributes, preferredContentFormat, supportedContentFormat);
    }

    public void createClient(ObjectsInitializer initializer, Map<String, String> additionalAttributes,
            ContentFormat preferredContentFormat, final ContentFormat... supportedContentFormat) {
        // Create Leshan Client
        LeshanClientBuilder builder = new LeshanClientBuilder(getCurrentEndpoint());
        builder.setObjects(initializer.createAll());
        builder.setBootstrapAdditionalAttributes(additionalAttributes);
        builder.setRegistrationEngineFactory(
                new DefaultRegistrationEngineFactory().setPreferredContentFormat(preferredContentFormat));

        // custom encoder/decoder with limited supported content format.
        if (supportedContentFormat != null && supportedContentFormat.length > 0) {
            final List<ContentFormat> supportedFormat = Arrays.asList(supportedContentFormat);
            builder.setDecoder(new DefaultLwM2mNodeDecoder() {
                @Override
                public boolean isSupported(ContentFormat format) {
                    return supportedFormat.contains(format);
                }
            });
            builder.setEncoder(new DefaultLwM2mNodeEncoder() {
                @Override
                public boolean isSupported(ContentFormat format) {
                    return supportedFormat.contains(format);
                }
            });
        }
        client = builder.build();
        setupClientMonitoring();
    }

    public BootstrapSecurityStore bsSecurityStore(final SecurityMode mode) {

        return new BootstrapSecurityStore() {
            @Override
            public SecurityInfo getByIdentity(String identity) {
                if (mode == SecurityMode.PSK) {
                    if (BootstrapIntegrationTestHelper.GOOD_PSK_ID.equals(identity)) {
                        return pskSecurityInfo();
                    }
                }
                return null;
            }

            @Override
            public Iterator<SecurityInfo> getAllByEndpoint(String endpoint) {
                if (getCurrentEndpoint().equals(endpoint)) {
                    SecurityInfo info;
                    if (mode == SecurityMode.PSK) {
                        info = pskSecurityInfo();
                        return Arrays.asList(info).iterator();
                    } else if (mode == SecurityMode.RPK) {
                        info = rpkSecurityInfo();
                        return Arrays.asList(info).iterator();
                    }
                }
                return null;
            }
        };
    }

    public SecurityInfo pskSecurityInfo() {
        SecurityInfo info = SecurityInfo.newPreSharedKeyInfo(getCurrentEndpoint(),
                BootstrapIntegrationTestHelper.GOOD_PSK_ID, BootstrapIntegrationTestHelper.GOOD_PSK_KEY);
        return info;
    }

    public SecurityInfo rpkSecurityInfo() {
        SecurityInfo info = SecurityInfo.newRawPublicKeyInfo(getCurrentEndpoint(), clientPublicKey);
        return info;
    }

    private BootstrapSecurityStore dummyBsSecurityStore() {
        return new BootstrapSecurityStore() {

            @Override
            public SecurityInfo getByIdentity(String identity) {
                return null;
            }

            @Override
            public Iterator<SecurityInfo> getAllByEndpoint(String endpoint) {
                return null;
            }
        };
    }

    public BootstrapConfigurationStore unsecuredBootstrapStore() {
        return new BootstrapConfigurationStore() {

            @Override
            public BootstrapConfiguration get(String endpoint, Identity deviceIdentity, BootstrapSession session) {

                BootstrapConfig bsConfig = new BootstrapConfig();

                // security for BS server
                ServerSecurity bsSecurity = new ServerSecurity();
                bsSecurity.bootstrapServer = true;
                bsSecurity.uri = "coap://" + bootstrapServer.getUnsecuredAddress().getHostString() + ":"
                        + bootstrapServer.getUnsecuredAddress().getPort();
                bsSecurity.securityMode = SecurityMode.NO_SEC;
                bsConfig.security.put(0, bsSecurity);

                // security for DM server
                ServerSecurity dmSecurity = new ServerSecurity();
                dmSecurity.uri = "coap://" + server.getUnsecuredAddress().getHostString() + ":"
                        + server.getUnsecuredAddress().getPort();
                dmSecurity.serverId = 2222;
                dmSecurity.securityMode = SecurityMode.NO_SEC;
                bsConfig.security.put(1, dmSecurity);

                // DM server
                ServerConfig dmConfig = new ServerConfig();
                dmConfig.shortId = 2222;
                bsConfig.servers.put(0, dmConfig);

                return new BootstrapConfiguration(BootstrapUtil.toRequests(bsConfig, session.getContentFormat()));
            }
        };
    }

    public BootstrapConfigurationStore deleteSecurityStore(Integer... objectToDelete) {
        String[] pathToDelete = new String[objectToDelete.length];
        for (int i = 0; i < pathToDelete.length; i++) {
            pathToDelete[i] = "/" + objectToDelete[i];

        }
        return deleteSecurityStore(pathToDelete);
    }

    public BootstrapConfigurationStore deleteSecurityStore(final String... pathToDelete) {
        return new BootstrapConfigurationStore() {

            @Override
            public BootstrapConfiguration get(String endpoint, Identity deviceIdentity, BootstrapSession session) {

                BootstrapConfig bsConfig = new BootstrapConfig();
                bsConfig.toDelete = Arrays.asList(pathToDelete);
                return new BootstrapConfiguration(BootstrapUtil.toRequests(bsConfig, session.getContentFormat()));
            }
        };
    }

    public BootstrapConfigurationStore unsecuredWithAclBootstrapStore() {
        return new BootstrapConfigurationStore() {

            @Override
            public BootstrapConfiguration get(String endpoint, Identity deviceIdentity, BootstrapSession session) {

                BootstrapConfig bsConfig = new BootstrapConfig();

                // security for BS server
                ServerSecurity bsSecurity = new ServerSecurity();
                bsSecurity.bootstrapServer = true;
                bsSecurity.uri = "coap://" + bootstrapServer.getUnsecuredAddress().getHostString() + ":"
                        + bootstrapServer.getUnsecuredAddress().getPort();
                bsSecurity.securityMode = SecurityMode.NO_SEC;
                bsConfig.security.put(0, bsSecurity);

                // security for DM server
                ServerSecurity dmSecurity = new ServerSecurity();
                dmSecurity.uri = "coap://" + server.getUnsecuredAddress().getHostString() + ":"
                        + server.getUnsecuredAddress().getPort();
                dmSecurity.serverId = 2222;
                dmSecurity.securityMode = SecurityMode.NO_SEC;
                bsConfig.security.put(1, dmSecurity);

                // DM server
                ServerConfig dmConfig = new ServerConfig();
                dmConfig.shortId = 2222;
                bsConfig.servers.put(0, dmConfig);

                // ACL
                ACLConfig aclConfig = new ACLConfig();
                aclConfig.objectId = 3;
                aclConfig.objectInstanceId = 0;
                HashMap<Integer, Long> acl = new HashMap<Integer, Long>();
                acl.put(3333, 1l); // server with short id 3333 has just read(1) right on device object (3/0)
                aclConfig.acls = acl;
                aclConfig.AccessControlOwner = 2222;
                bsConfig.acls.put(0, aclConfig);

                aclConfig = new ACLConfig();
                aclConfig.objectId = 4;
                aclConfig.objectInstanceId = 0;
                aclConfig.AccessControlOwner = 2222;
                bsConfig.acls.put(1, aclConfig);

                return new BootstrapConfiguration(BootstrapUtil.toRequests(bsConfig));
            }
        };
    }

    public BootstrapConfigurationStore pskBootstrapStore() {
        return new BootstrapConfigurationStore() {

            @Override
            public BootstrapConfiguration get(String endpoint, Identity deviceIdentity, BootstrapSession session) {

                BootstrapConfig bsConfig = new BootstrapConfig();

                // security for BS server
                ServerSecurity bsSecurity = new ServerSecurity();
                bsSecurity.bootstrapServer = true;
                bsSecurity.uri = "coap://" + bootstrapServer.getUnsecuredAddress().getHostString() + ":"
                        + bootstrapServer.getUnsecuredAddress().getPort();
                bsSecurity.securityMode = SecurityMode.NO_SEC;
                bsConfig.security.put(0, bsSecurity);

                // security for DM server
                ServerSecurity dmSecurity = new ServerSecurity();
                dmSecurity.uri = "coaps://" + server.getUnsecuredAddress().getHostString() + ":"
                        + server.getSecuredAddress().getPort();
                dmSecurity.serverId = 2222;
                dmSecurity.securityMode = SecurityMode.PSK;
                dmSecurity.publicKeyOrId = GOOD_PSK_ID.getBytes();
                dmSecurity.secretKey = GOOD_PSK_KEY;
                bsConfig.security.put(1, dmSecurity);

                // DM server
                ServerConfig dmConfig = new ServerConfig();
                dmConfig.shortId = 2222;
                bsConfig.servers.put(0, dmConfig);

                return new BootstrapConfiguration(BootstrapUtil.toRequests(bsConfig, session.getContentFormat()));
            }
        };
    }

    public BootstrapConfigurationStore rpkBootstrapStore() {
        return new BootstrapConfigurationStore() {

            @Override
            public BootstrapConfiguration get(String endpoint, Identity deviceIdentity, BootstrapSession session) {

                BootstrapConfig bsConfig = new BootstrapConfig();

                // security for BS server
                ServerSecurity bsSecurity = new ServerSecurity();
                bsSecurity.bootstrapServer = true;
                bsSecurity.uri = "coap://" + bootstrapServer.getUnsecuredAddress().getHostString() + ":"
                        + bootstrapServer.getUnsecuredAddress().getPort();
                bsSecurity.securityMode = SecurityMode.NO_SEC;
                bsConfig.security.put(0, bsSecurity);

                // security for DM server
                ServerSecurity dmSecurity = new ServerSecurity();
                dmSecurity.uri = "coaps://" + server.getUnsecuredAddress().getHostString() + ":"
                        + server.getSecuredAddress().getPort();
                dmSecurity.serverId = 2222;
                dmSecurity.securityMode = SecurityMode.RPK;
                dmSecurity.publicKeyOrId = clientPublicKey.getEncoded();
                dmSecurity.secretKey = clientPrivateKey.getEncoded();
                dmSecurity.serverPublicKey = serverPublicKey.getEncoded();
                bsConfig.security.put(1, dmSecurity);

                // DM server
                ServerConfig dmConfig = new ServerConfig();
                dmConfig.shortId = 2222;
                bsConfig.servers.put(0, dmConfig);

                return new BootstrapConfiguration(BootstrapUtil.toRequests(bsConfig, session.getContentFormat()));
            }
        };
    }

    @Override
    public void dispose() {
        super.dispose();
        ((EditableSecurityStore) server.getSecurityStore()).remove(getCurrentEndpoint(), false);
    }
}
