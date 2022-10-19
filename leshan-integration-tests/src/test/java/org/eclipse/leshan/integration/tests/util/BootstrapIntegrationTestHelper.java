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

import static org.eclipse.leshan.client.object.Security.oscoreOnlyBootstrap;
import static org.eclipse.leshan.core.LwM2mId.OSCORE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.leshan.client.LeshanClientBuilder;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpointsProvider;
import org.eclipse.leshan.client.californium.endpoint.coap.CoapOscoreProtocolProvider;
import org.eclipse.leshan.client.californium.endpoint.coaps.CoapsClientProtocolProvider;
import org.eclipse.leshan.client.engine.DefaultRegistrationEngineFactory;
import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.client.object.Oscore;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.oscore.OscoreIdentity;
import org.eclipse.leshan.core.oscore.OscoreSetting;
import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ACLConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerSecurity;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigStoreTaskProvider;
import org.eclipse.leshan.server.bootstrap.BootstrapFailureCause;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.BootstrapTaskProvider;
import org.eclipse.leshan.server.bootstrap.DefaultBootstrapAuthorizer;
import org.eclipse.leshan.server.bootstrap.DefaultBootstrapSession;
import org.eclipse.leshan.server.bootstrap.DefaultBootstrapSessionManager;
import org.eclipse.leshan.server.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.bootstrap.LeshanBootstrapServerBuilder;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.CaliforniumBootstrapServerEndpointsProvider;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.CaliforniumBootstrapServerEndpointsProvider.Builder;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.coap.CoapBootstrapServerProtocolProvider;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.coap.CoapOscoreBootstrapServerEndpointFactory;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.coaps.CoapsBootstrapServerProtocolProvider;
import org.eclipse.leshan.server.model.StandardBootstrapModelProvider;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;

/**
 * Helper for running a server and executing a client against it.
 *
 */
public class BootstrapIntegrationTestHelper extends SecureIntegrationTestHelper {

    public static final byte[] OSCORE_BOOTSTRAP_MASTER_SECRET = Hex.decodeHex("BB1234567890".toCharArray());
    public static final byte[] OSCORE_BOOTSTRAP_MASTER_SALT = Hex.decodeHex("BB0987654321".toCharArray());
    public static final byte[] OSCORE_BOOTSTRAP_SENDER_ID = Hex.decodeHex("BBABCDEF".toCharArray());
    public static final byte[] OSCORE_BOOTSTRAP_RECIPIENT_ID = Hex.decodeHex("BBFEDCBA".toCharArray());

    public LeshanBootstrapServer bootstrapServer;
    public final PublicKey bootstrapServerPublicKey;
    public final PrivateKey bootstrapServerPrivateKey;
    public volatile LwM2mResponse lastCustomResponse;

    private final SynchronousBootstrapListener bootstrapListener = new SynchronousBootstrapListener();

    private class TestBootstrapSessionManager extends DefaultBootstrapSessionManager {

        public TestBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore,
                BootstrapTaskProvider tasksProvider) {
            super(tasksProvider, new StandardBootstrapModelProvider(), new DefaultBootstrapAuthorizer(bsSecurityStore));
        }

        public TestBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore, BootstrapConfigStore configStore) {
            super(bsSecurityStore, configStore);
        }

        @Override
        public BootstrapSession begin(BootstrapRequest request, Identity clientIdentity, URI endpointUsed) {
            assertThat(request.getCoapRequest(), instanceOf(Request.class));
            return super.begin(request, clientIdentity, endpointUsed);
        }
    }

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
            KeySpec publicKeySpec = new ECPublicKeySpec(
                    new ECPoint(new BigInteger(1, publicX), new BigInteger(1, publicY)), parameterSpec);
            KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(1, privateS), parameterSpec);

            // Get keys
            bootstrapServerPublicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            bootstrapServerPrivateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private LeshanBootstrapServerBuilder createBootstrapBuilder(BootstrapSecurityStore securityStore,
            BootstrapConfigStore configStore) {
        LeshanBootstrapServerBuilder builder = new LeshanBootstrapServerBuilder();

        Builder endpointsBuilder = new CaliforniumBootstrapServerEndpointsProvider.Builder(
                new CoapBootstrapServerProtocolProvider(), new CoapsBootstrapServerProtocolProvider());
        endpointsBuilder.addEndpoint(
                EndpointUriUtil.createUri("coap", new InetSocketAddress(InetAddress.getLoopbackAddress(), 0)));
        endpointsBuilder.addEndpoint(
                EndpointUriUtil.createUri("coaps", new InetSocketAddress(InetAddress.getLoopbackAddress(), 0)));
        builder.setEndpointsProvider(endpointsBuilder.build());

        builder.setPrivateKey(bootstrapServerPrivateKey);
        builder.setPublicKey(bootstrapServerPublicKey);
        builder.setSessionManager(new DefaultBootstrapSessionManager(securityStore, configStore) {

            @Override
            public BootstrapSession begin(BootstrapRequest request, Identity clientIdentity, URI endpointUsed) {
                assertThat(request.getCoapRequest(), instanceOf(Request.class));
                return super.begin(request, clientIdentity, endpointUsed);
            }

            @Override
            public void end(BootstrapSession bsSession) {
                super.end(bsSession);
            }
        });
        return builder;
    }

    public void createOscoreBootstrapServer(BootstrapSecurityStore securityStore, BootstrapConfigStore bootstrapStore) {
        LeshanBootstrapServerBuilder builder = createBootstrapBuilder(securityStore, bootstrapStore);
        Builder endpointsBuilder = new CaliforniumBootstrapServerEndpointsProvider.Builder(
                new CoapBootstrapServerProtocolProvider(), new CoapsBootstrapServerProtocolProvider());

        endpointsBuilder.addEndpoint(new CoapOscoreBootstrapServerEndpointFactory(
                EndpointUriUtil.createUri("coap", new InetSocketAddress(InetAddress.getLoopbackAddress(), 0))));
        endpointsBuilder.addEndpoint(
                EndpointUriUtil.createUri("coaps", new InetSocketAddress(InetAddress.getLoopbackAddress(), 0)));
        builder.setEndpointsProvider(endpointsBuilder.build());
        if (bootstrapStore == null) {
            bootstrapStore = unsecuredBootstrapStore();
        }

        if (securityStore == null) {
            securityStore = dummyBsSecurityStore();
        }
        builder.setSecurityStore(securityStore);
        builder.setSessionManager(new TestBootstrapSessionManager(securityStore, bootstrapStore));
        bootstrapServer = builder.build();
        setupBootstrapServerMonitoring();
    }

    public void createBootstrapServer(BootstrapSecurityStore securityStore, BootstrapConfigStore bootstrapStore) {
        LeshanBootstrapServerBuilder builder = createBootstrapBuilder(securityStore, bootstrapStore);
        if (bootstrapStore == null) {
            bootstrapStore = unsecuredBootstrapStore();
        }

        if (securityStore == null) {
            securityStore = dummyBsSecurityStore();
        }
        builder.setSecurityStore(securityStore);
        builder.setSessionManager(new TestBootstrapSessionManager(securityStore, bootstrapStore));
        bootstrapServer = builder.build();
        setupBootstrapServerMonitoring();
    }

    public void createBootstrapServer(BootstrapSecurityStore securityStore, BootstrapConfigStore bootstrapStore,
            final BootstrapDownlinkRequest<?> firstCustomRequest) {
        LeshanBootstrapServerBuilder builder = createBootstrapBuilder(securityStore, bootstrapStore);
        if (bootstrapStore == null) {
            bootstrapStore = unsecuredBootstrapStore();
        }
        if (securityStore == null) {
            securityStore = dummyBsSecurityStore();
        }
        // start bootstrap session by a bootstrap discover request
        BootstrapConfigStoreTaskProvider taskProvider = new BootstrapConfigStoreTaskProvider(bootstrapStore) {
            @Override
            public Tasks getTasks(BootstrapSession session, List<LwM2mResponse> previousResponses) {
                if (previousResponses == null) {
                    Tasks tasks = new Tasks();
                    tasks.requestsToSend = new ArrayList<>(1);
                    tasks.requestsToSend.add(firstCustomRequest);
                    tasks.last = false;
                    tasks.supportedObjects = new HashMap<>();
                    tasks.supportedObjects.put(0, "1.1");
                    tasks.supportedObjects.put(1, "1.1");
                    tasks.supportedObjects.put(2, "1.0");
                    return tasks;
                } else {
                    lastCustomResponse = previousResponses.get(0);
                    return super.getTasks(session, null);
                }
            }
        };
        builder.setSecurityStore(securityStore);
        builder.setSessionManager(new TestBootstrapSessionManager(securityStore, taskProvider));
        bootstrapServer = builder.build();
        setupBootstrapServerMonitoring();
    }

    public void createBootstrapServer(BootstrapSecurityStore securityStore) {
        createBootstrapServer(securityStore, null);
    }

    public Security withoutSecurity() {
        return withoutSecurityAndInstanceId(null);
    }

    public Security withoutSecurityAndInstanceId(Integer id) {
        // Create Security Object (with bootstrap server only)
        String bsUrl = bootstrapServer.getEndpoint(Protocol.COAP).getURI().toString();
        Security sec = Security.noSecBootstrap(bsUrl);
        if (id != null)
            sec.setId(id);
        return sec;
    }

    protected void setupBootstrapServerMonitoring() {
        bootstrapServer.addListener(bootstrapListener);
    }

    public void createOscoreOnlyBootstrapClient() {
        String bsServerUri = bootstrapServer.getEndpoint(Protocol.COAP).getURI().toString();

        Oscore oscoreObject = new Oscore(12345, getBootstrapClientOscoreSetting());
        ObjectsInitializer initializer = new TestObjectsInitializer();
        initializer.setInstancesForObject(OSCORE, oscoreObject);
        createClient(oscoreOnlyBootstrap(bsServerUri, oscoreObject.getId()), initializer);
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
        String bsUrl = bootstrapServer.getEndpoint(Protocol.COAPS).getURI().toString();
        byte[] pskId = pskIdentity.getBytes(StandardCharsets.UTF_8);
        Security security = Security.pskBootstrap(bsUrl, pskId, pskKey);

        createClient(security, null);
    }

    @Override
    public void createRPKClient() {
        String bsUrl = bootstrapServer.getEndpoint(Protocol.COAPS).getURI().toString();
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
        initializer.setClassForObject(LwM2mId.SERVER, Server.class);
        initializer.setClassForObject(LwM2mId.OSCORE, Oscore.class);
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
            builder.setDecoder(new DefaultLwM2mDecoder() {
                @Override
                public boolean isSupported(ContentFormat format) {
                    return supportedFormat.contains(format);
                }
            });
            builder.setEncoder(new DefaultLwM2mEncoder() {
                @Override
                public boolean isSupported(ContentFormat format) {
                    return supportedFormat.contains(format);
                }
            });
        }

        // create endpoint provider
        CaliforniumClientEndpointsProvider.Builder endpointProviderBuilder = new CaliforniumClientEndpointsProvider.Builder(
                new CoapOscoreProtocolProvider(), new CoapsClientProtocolProvider());
        endpointProviderBuilder.setClientAddress(InetAddress.getLoopbackAddress());
        builder.setEndpointsProvider(endpointProviderBuilder.build());
        client = builder.build();
        setupClientMonitoring();
    }

    public BootstrapSecurityStore bsOscoreSecurityStore() {
        return new BootstrapSecurityStore() {

            @Override
            public Iterator<SecurityInfo> getAllByEndpoint(String endpoint) {
                if (getCurrentEndpoint().equals(endpoint)) {
                    return Arrays.asList(SecurityInfo.newOscoreInfo(endpoint, getServerOscoreSetting())).iterator();
                }
                return null;
            }

            @Override
            public SecurityInfo getByIdentity(String pskIdentity) {
                return null;
            }

            @Override
            public SecurityInfo getByOscoreIdentity(OscoreIdentity oscoreIdentity) {
                if (oscoreIdentity.equals(getBootstrapServerOscoreSetting().getOscoreIdentity())) {
                    return oscoreSecurityInfo();
                }
                return null;
            }
        };
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

            @Override
            public SecurityInfo getByOscoreIdentity(OscoreIdentity oscoreIdentity) {
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

    public SecurityInfo oscoreSecurityInfo() {
        SecurityInfo info = SecurityInfo.newOscoreInfo(getCurrentEndpoint(), getBootstrapServerOscoreSetting());
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

            @Override
            public SecurityInfo getByOscoreIdentity(OscoreIdentity oscoreIdentity) {
                return null;
            }
        };
    }

    public BootstrapConfigStore unsecuredBootstrapStore() {
        return unsecuredBootstrapStore(0, false, null);
    }

    public BootstrapConfigStore unsecuredBootstrapStoreWithBsSecurityInstanceIdAt(Integer instanceId) {
        return unsecuredBootstrapStore(instanceId, false, null);
    }

    public BootstrapConfigStore unsecuredBootstrapWithAutoID() {
        return unsecuredBootstrapStore(0, true, null);
    }

    public BootstrapConfigStore unsecuredBootstrap(ContentFormat format) {
        return unsecuredBootstrapStore(0, true, format);
    }

    public BootstrapConfigStore unsecuredBootstrapStore(final Integer bsInstanceId, final boolean autoId,
            final ContentFormat format) {
        return new BootstrapConfigStore() {

            @Override
            public BootstrapConfig get(String endpoint, Identity deviceIdentity, BootstrapSession session) {

                BootstrapConfig bsConfig = new BootstrapConfig();

                bsConfig.autoIdForSecurityObject = autoId;
                bsConfig.contentFormat = format;

                // security for BS server
                ServerSecurity bsSecurity = new ServerSecurity();
                bsSecurity.bootstrapServer = true;
                bsSecurity.uri = bootstrapServer.getEndpoint(Protocol.COAP).getURI().toString();
                bsSecurity.securityMode = SecurityMode.NO_SEC;
                bsConfig.security.put(bsInstanceId, bsSecurity);

                // security for DM server
                ServerSecurity dmSecurity = new ServerSecurity();
                dmSecurity.uri = server.getEndpoint(Protocol.COAP).getURI().toString();
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
    }

    public BootstrapConfigStore deleteSecurityStore(Integer... objectToDelete) {
        String[] pathToDelete = new String[objectToDelete.length];
        for (int i = 0; i < pathToDelete.length; i++) {
            pathToDelete[i] = "/" + objectToDelete[i];

        }
        return deleteSecurityStore(pathToDelete);
    }

    public BootstrapConfigStore deleteSecurityStore(final String... pathToDelete) {
        return new BootstrapConfigStore() {

            @Override
            public BootstrapConfig get(String endpoint, Identity deviceIdentity, BootstrapSession session) {

                BootstrapConfig bsConfig = new BootstrapConfig();
                bsConfig.toDelete = Arrays.asList(pathToDelete);
                return bsConfig;
            }
        };
    }

    public BootstrapConfigStore unsecuredWithAclBootstrapStore() {
        return new BootstrapConfigStore() {

            @Override
            public BootstrapConfig get(String endpoint, Identity deviceIdentity, BootstrapSession session) {

                BootstrapConfig bsConfig = new BootstrapConfig();

                // security for BS server
                ServerSecurity bsSecurity = new ServerSecurity();
                bsSecurity.bootstrapServer = true;
                bsSecurity.uri = bootstrapServer.getEndpoint(Protocol.COAP).getURI().toString();
                bsSecurity.securityMode = SecurityMode.NO_SEC;
                bsConfig.security.put(0, bsSecurity);

                // security for DM server
                ServerSecurity dmSecurity = new ServerSecurity();
                dmSecurity.uri = server.getEndpoint(Protocol.COAP).getURI().toString();
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

                return bsConfig;
            }
        };
    }

    public BootstrapConfigStore pskBootstrapStore() {
        return new BootstrapConfigStore() {

            @Override
            public BootstrapConfig get(String endpoint, Identity deviceIdentity, BootstrapSession session) {

                BootstrapConfig bsConfig = new BootstrapConfig();

                // security for BS server
                ServerSecurity bsSecurity = new ServerSecurity();
                bsSecurity.bootstrapServer = true;
                bsSecurity.uri = bootstrapServer.getEndpoint(Protocol.COAP).getURI().toString();
                bsSecurity.securityMode = SecurityMode.NO_SEC;
                bsConfig.security.put(0, bsSecurity);

                // security for DM server
                ServerSecurity dmSecurity = new ServerSecurity();
                dmSecurity.uri = server.getEndpoint(Protocol.COAPS).getURI().toString();
                dmSecurity.serverId = 2222;
                dmSecurity.securityMode = SecurityMode.PSK;
                dmSecurity.publicKeyOrId = GOOD_PSK_ID.getBytes();
                dmSecurity.secretKey = GOOD_PSK_KEY;
                bsConfig.security.put(1, dmSecurity);

                // DM server
                ServerConfig dmConfig = new ServerConfig();
                dmConfig.shortId = 2222;
                bsConfig.servers.put(0, dmConfig);

                return bsConfig;
            }
        };
    }

    public BootstrapConfigStore rpkBootstrapStore() {
        return new BootstrapConfigStore() {

            @Override
            public BootstrapConfig get(String endpoint, Identity deviceIdentity, BootstrapSession session) {

                BootstrapConfig bsConfig = new BootstrapConfig();

                // security for BS server
                ServerSecurity bsSecurity = new ServerSecurity();
                bsSecurity.bootstrapServer = true;
                bsSecurity.uri = bootstrapServer.getEndpoint(Protocol.COAP).getURI().toString();
                bsSecurity.securityMode = SecurityMode.NO_SEC;
                bsConfig.security.put(0, bsSecurity);

                // security for DM server
                ServerSecurity dmSecurity = new ServerSecurity();
                dmSecurity.uri = server.getEndpoint(Protocol.COAPS).getURI().toString();
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

                return bsConfig;
            }
        };
    }

    public void waitForBootstrapSuccessAtServerSide(long timeInSeconds) {
        try {
            bootstrapListener.waitForSuccess(timeInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void waitForBootstrapFailureAtServerSide(long timeInSeconds) {
        try {
            bootstrapListener.waitForFailure(timeInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public DefaultBootstrapSession getLastBootstrapSession() {
        return (DefaultBootstrapSession) bootstrapListener.getLastSuccessfulSession();
    }

    public BootstrapFailureCause getLastCauseOfBootstrapFailure() {
        return bootstrapListener.getLastCauseOfFailure();
    }

    public BootstrapConfigStore unsecuredBootstrapStoreWithOscoreServer() {
        return new BootstrapConfigStore() {

            @Override
            public BootstrapConfig get(String endpoint, Identity deviceIdentity, BootstrapSession session) {

                BootstrapConfig bsConfig = new BootstrapConfig();

                // security for BS server
                ServerSecurity bsSecurity = new ServerSecurity();
                bsSecurity.bootstrapServer = true;
                bsSecurity.uri = bootstrapServer.getEndpoint(Protocol.COAP).getURI().toString();
                bsSecurity.securityMode = SecurityMode.NO_SEC;
                bsConfig.security.put(0, bsSecurity);

                // security for DM server
                ServerSecurity dmSecurity = new ServerSecurity();
                dmSecurity.uri = server.getEndpoint(Protocol.COAP).getURI().toString();
                dmSecurity.serverId = 2222;
                dmSecurity.securityMode = SecurityMode.NO_SEC;
                dmSecurity.oscoreSecurityMode = 1;
                bsConfig.security.put(1, dmSecurity);
                bsConfig.oscore.put(1, getOscoreBootstrapObject(false));

                // DM server
                ServerConfig dmConfig = new ServerConfig();
                dmConfig.shortId = 2222;
                bsConfig.servers.put(0, dmConfig);

                return bsConfig;
            }
        };
    }

    public BootstrapConfigStore oscoreBootstrapStoreWithOscoreServer() {

        return new BootstrapConfigStore() {

            @Override
            public BootstrapConfig get(String endpoint, Identity deviceIdentity, BootstrapSession session) {

                BootstrapConfig bsConfig = new BootstrapConfig();

                // security for BS server
                ServerSecurity bsSecurity = new ServerSecurity();
                bsSecurity.bootstrapServer = true;
                bsSecurity.uri = bootstrapServer.getEndpoint(Protocol.COAP).getURI().toString();
                bsSecurity.securityMode = SecurityMode.NO_SEC;
                bsSecurity.oscoreSecurityMode = 1;
                bsConfig.security.put(0, bsSecurity);
                bsConfig.oscore.put(0, getOscoreBootstrapObject(true));

                // security for DM server
                ServerSecurity dmSecurity = new ServerSecurity();
                dmSecurity.uri = server.getEndpoint(Protocol.COAP).getURI().toString();
                dmSecurity.serverId = 2222;
                dmSecurity.securityMode = SecurityMode.NO_SEC;
                dmSecurity.oscoreSecurityMode = 1;
                bsConfig.security.put(1, dmSecurity);
                bsConfig.oscore.put(1, getOscoreBootstrapObject(false));

                // DM server
                ServerConfig dmConfig = new ServerConfig();
                dmConfig.shortId = 2222;
                bsConfig.servers.put(0, dmConfig);

                return bsConfig;
            }
        };
    }

    public BootstrapConfigStore oscoreBootstrapStoreWithUnsecuredServer() {

        return new BootstrapConfigStore() {

            @Override
            public BootstrapConfig get(String endpoint, Identity deviceIdentity, BootstrapSession session) {

                BootstrapConfig bsConfig = new BootstrapConfig();

                // security for BS server
                ServerSecurity bsSecurity = new ServerSecurity();
                bsSecurity.bootstrapServer = true;
                bsSecurity.uri = bootstrapServer.getEndpoint(Protocol.COAP).getURI().toString();
                bsSecurity.securityMode = SecurityMode.NO_SEC;
                bsSecurity.oscoreSecurityMode = 1;
                bsConfig.security.put(0, bsSecurity);
                bsConfig.oscore.put(0, getOscoreBootstrapObject(true));

                // security for DM server
                ServerSecurity dmSecurity = new ServerSecurity();
                dmSecurity.uri = server.getEndpoint(Protocol.COAP).getURI().toString();
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
    }

    public static OscoreSetting getBootstrapServerOscoreSetting() {
        return new OscoreSetting(OSCORE_BOOTSTRAP_RECIPIENT_ID, OSCORE_BOOTSTRAP_SENDER_ID,
                OSCORE_BOOTSTRAP_MASTER_SECRET, OSCORE_AEAD_ALGORITHM, OSCORE_HKDF_ALGORITHM,
                OSCORE_BOOTSTRAP_MASTER_SALT);
    }

    protected static OscoreSetting getBootstrapClientOscoreSetting() {
        return new OscoreSetting(OSCORE_BOOTSTRAP_SENDER_ID, OSCORE_BOOTSTRAP_RECIPIENT_ID,
                OSCORE_BOOTSTRAP_MASTER_SECRET, OSCORE_AEAD_ALGORITHM, OSCORE_HKDF_ALGORITHM,
                OSCORE_BOOTSTRAP_MASTER_SALT);
    }

    protected static BootstrapConfig.OscoreObject getOscoreBootstrapObject(boolean bootstrap) {
        BootstrapConfig.OscoreObject oscoreObject = new BootstrapConfig.OscoreObject();

        oscoreObject.oscoreMasterSecret = bootstrap ? OSCORE_BOOTSTRAP_MASTER_SECRET : OSCORE_MASTER_SECRET;
        oscoreObject.oscoreSenderId = bootstrap ? OSCORE_BOOTSTRAP_SENDER_ID : OSCORE_SENDER_ID;
        oscoreObject.oscoreRecipientId = bootstrap ? OSCORE_BOOTSTRAP_RECIPIENT_ID : OSCORE_RECIPIENT_ID;
        oscoreObject.oscoreAeadAlgorithm = OSCORE_AEAD_ALGORITHM.getValue();
        oscoreObject.oscoreHmacAlgorithm = OSCORE_HKDF_ALGORITHM.getValue();
        oscoreObject.oscoreMasterSalt = bootstrap ? OSCORE_BOOTSTRAP_MASTER_SALT : OSCORE_MASTER_SALT;

        return oscoreObject;
    }

    @Override
    public void dispose() {
        super.dispose();
        ((EditableSecurityStore) server.getSecurityStore()).remove(getCurrentEndpoint(), false);
    }
}
