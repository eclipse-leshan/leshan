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

import static org.eclipse.leshan.core.LwM2mId.OSCORE;

import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.security.auth.x500.X500Principal;

import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.leshan.client.LeshanClientBuilder;
import org.eclipse.leshan.client.bootstrap.BootstrapConsistencyChecker;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpointsProvider;
import org.eclipse.leshan.client.californium.endpoint.ClientProtocolProvider;
import org.eclipse.leshan.client.californium.endpoint.coap.CoapClientProtocolProvider;
import org.eclipse.leshan.client.californium.endpoint.coap.CoapOscoreProtocolProvider;
import org.eclipse.leshan.client.californium.endpoint.coaps.CoapsClientProtocolProvider;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpointsProvider;
import org.eclipse.leshan.client.engine.DefaultRegistrationEngineFactory;
import org.eclipse.leshan.client.engine.RegistrationEngineFactory;
import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.client.object.LwM2mTestObject;
import org.eclipse.leshan.client.object.Oscore;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.resource.SimpleInstanceEnabler;
import org.eclipse.leshan.client.send.DataSender;
import org.eclipse.leshan.client.send.ManualDataSender;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.CertificateUsage;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.LinkSerializer;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeParser;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.core.oscore.OscoreSetting;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.argument.Arguments;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.util.TestLwM2mId;
import org.eclipse.leshan.core.util.X509CertUtil;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.bootstrap.endpoint.LwM2mBootstrapServerEndpoint;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpoint;

public class LeshanTestClientBuilder extends LeshanClientBuilder {

    private static final Random r = new Random();

    private String endpointName;
    private Protocol protocolToUse;
    private LeshanServer server;
    private LeshanBootstrapServer bootstrapServer;

    private Integer bootstrapServerId;

    private long lifetime = 300l; // use large lifetime by default
    private final ObjectsInitializer initializer;
    private final DefaultRegistrationEngineFactory engineFactory = new DefaultRegistrationEngineFactory();
    private ContentFormat[] supportedContentFormat;

    private String pskIdentity;
    private byte[] pskKey;
    private PrivateKey clientPrivateKey;
    private PublicKey clientPublicKey;
    private PublicKey serverPublicKey;
    private X509Certificate clientCertificate;
    private X509Certificate serverCertificate;
    private CertificateUsage certificageUsage;
    private OscoreSetting oscoreSetting;

    public LeshanTestClientBuilder(Protocol protocolToUse) {
        this();
        using(protocolToUse);
    }

    public LeshanTestClientBuilder() {
        super("leshan_test_client_" + r.nextInt());

        // Create objects Enabler
        initializer = new ObjectsInitializer(new StaticModel(TestObjectLoader.loadDefaultObject()));
        initializer.setInstancesForObject(LwM2mId.DEVICE, new TestDevice("Eclipse Leshan", "IT-TEST-123", "12345"));
        initializer.setClassForObject(LwM2mId.ACCESS_CONTROL, DummyInstanceEnabler.class);
        initializer.setInstancesForObject(TestLwM2mId.TEST_OBJECT, new LwM2mTestObject());

        setDataSenders(new ManualDataSender());
    }

    @Override
    public LeshanTestClient build() {
        try {
            // connect to LWM2M Server
            if (server != null) {
                LwM2mServerEndpoint endpoint = server.getEndpoint(protocolToUse);
                URI uri = endpoint.getURI();
                int serverID = 12345;

                if (pskIdentity != null && pskKey != null) {
                    initializer.setInstancesForObject(LwM2mId.SECURITY,
                            Security.psk(uri.toString(), serverID, pskIdentity.getBytes(), pskKey));
                } else if (clientPublicKey != null && clientPrivateKey != null) {
                    initializer.setInstancesForObject(LwM2mId.SECURITY, Security.rpk(uri.toString(), serverID,
                            clientPublicKey.getEncoded(), clientPrivateKey.getEncoded(), serverPublicKey.getEncoded()));
                } else if (clientCertificate != null && clientPrivateKey != null) {
                    initializer.setInstancesForObject(LwM2mId.SECURITY,
                            Security.x509(uri.toString(), serverID, clientCertificate.getEncoded(),
                                    clientPrivateKey.getEncoded(), serverCertificate.getEncoded(),
                                    certificageUsage.code));
                } else {
                    if (oscoreSetting != null) {
                        Oscore oscoreObject = new Oscore(111, oscoreSetting);
                        initializer.setInstancesForObject(OSCORE, oscoreObject);
                        initializer.setInstancesForObject(LwM2mId.SECURITY,
                                Security.oscoreOnly(uri.toString(), serverID, oscoreObject.getId()));
                    } else {
                        initializer.setInstancesForObject(LwM2mId.SECURITY, Security.noSec(uri.toString(), serverID));
                    }
                }
                initializer.setInstancesForObject(LwM2mId.SERVER, new Server(serverID, lifetime));
            }
            // connect to LWM2M Bootstrap Server
            else if (bootstrapServer != null) {
                LwM2mBootstrapServerEndpoint endpoint = bootstrapServer.getEndpoint(protocolToUse);
                URI uri = endpoint.getURI();
                Security securityEnabler = null;

                if (pskIdentity != null && pskKey != null) {
                    securityEnabler = Security.pskBootstrap(uri.toString(), pskIdentity.getBytes(), pskKey);
                } else if (clientPublicKey != null && clientPrivateKey != null) {
                    securityEnabler = Security.rpkBootstrap(uri.toString(), clientPublicKey.getEncoded(),
                            clientPrivateKey.getEncoded(), serverPublicKey.getEncoded());
                } else if (clientCertificate != null && clientPrivateKey != null) {
                    securityEnabler = Security.x509Bootstrap(uri.toString(), clientCertificate.getEncoded(),
                            clientPrivateKey.getEncoded(), serverCertificate.getEncoded(), certificageUsage.code);
                } else {
                    if (oscoreSetting != null) {
                        Oscore oscoreObject = new Oscore(12345, oscoreSetting);
                        initializer.setInstancesForObject(OSCORE, oscoreObject);
                        securityEnabler = Security.oscoreOnlyBootstrap(uri.toString(), oscoreObject.getId());
                    } else {
                        securityEnabler = Security.noSecBootstrap(uri.toString());
                    }
                }
                if (bootstrapServerId != null) {
                    securityEnabler.setId(bootstrapServerId);
                }
                initializer.setInstancesForObject(LwM2mId.SECURITY, securityEnabler);
                initializer.setClassForObject(LwM2mId.SERVER, Server.class);
            }
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException(e);
        }

        List<LwM2mObjectEnabler> objects = initializer.createAll();
        setObjects(objects);
        setRegistrationEngineFactory(engineFactory.setRequestTimeoutInMs(800));

        // custom encoder/decoder with limited supported content format.
        if (supportedContentFormat != null && supportedContentFormat.length > 0) {
            final List<ContentFormat> supportedFormat = Arrays.asList(supportedContentFormat);
            setDecoder(new DefaultLwM2mDecoder(true) {
                @Override
                public boolean isSupported(ContentFormat format) {
                    return supportedFormat.contains(format);
                }
            });
            setEncoder(new DefaultLwM2mEncoder(true) {
                @Override
                public boolean isSupported(ContentFormat format) {
                    return supportedFormat.contains(format);
                }
            });
        } else {
            setDecoder(new DefaultLwM2mDecoder(true));
            setEncoder(new DefaultLwM2mEncoder(true));
        }

        return (LeshanTestClient) super.build();
    }

    @Override
    protected LeshanTestClient createLeshanClient(String endpoint, List<? extends LwM2mObjectEnabler> objectEnablers,
            List<DataSender> dataSenders, List<Certificate> trustStore, RegistrationEngineFactory engineFactory,
            BootstrapConsistencyChecker checker, Map<String, String> additionalAttributes,
            Map<String, String> bsAdditionalAttributes, LwM2mEncoder encoder, LwM2mDecoder decoder,
            ScheduledExecutorService sharedExecutor, LinkSerializer linkSerializer,
            LwM2mAttributeParser attributeParser, LwM2mClientEndpointsProvider endpointsProvider) {
        String endpointName;
        if (this.endpointName != null) {
            endpointName = this.endpointName;
        } else if (clientCertificate != null) {
            X500Principal subjectDN = clientCertificate.getSubjectX500Principal();
            endpointName = X509CertUtil.getPrincipalField(subjectDN, "CN");
        } else {
            endpointName = endpoint;
        }

        return new LeshanTestClient(endpointName, objectEnablers, dataSenders, trustStore, engineFactory, checker,
                additionalAttributes, bsAdditionalAttributes, encoder, decoder, sharedExecutor, linkSerializer,
                attributeParser, endpointsProvider);
    }

    public static LeshanTestClientBuilder givenClientUsing(Protocol protocol) {
        return new LeshanTestClientBuilder(protocol);
    }

    public static LeshanTestClientBuilder givenClient() {
        return new LeshanTestClientBuilder();
    }

    public LeshanTestClientBuilder with(LwM2mClientEndpointsProvider endpointsProvider) {
        setEndpointsProvider(endpointsProvider);
        return this;
    }

    public LeshanTestClientBuilder with(String endpointProvider) {
        if (endpointProvider.equals("Californium")) {
            setEndpointsProvider(
                    new CaliforniumClientEndpointsProvider.Builder(getCaliforniumProtocolProvider(protocolToUse))
                            .build());
        } else if (endpointProvider.equals("Californium-OSCORE")) {
            setEndpointsProvider(new CaliforniumClientEndpointsProvider.Builder(
                    getCaliforniumProtocolProviderSupportingOscore(protocolToUse)).build());
        }
        return this;
    }

    private ClientProtocolProvider getCaliforniumProtocolProvider(Protocol protocol) {
        if (protocolToUse.equals(Protocol.COAP)) {
            return new CoapClientProtocolProvider();
        } else if (protocolToUse.equals(Protocol.COAPS)) {
            return new CoapsClientProtocolProvider() {
                @Override
                public void applyDefaultValue(Configuration configuration) {
                    super.applyDefaultValue(configuration);
                    configuration.setAsList(DtlsConfig.DTLS_PRESELECTED_CIPHER_SUITES,
                            CipherSuite.TLS_PSK_WITH_AES_128_CCM_8, CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8);
                }
            };
        }
        throw new IllegalStateException(String.format("No Californium Protocol Provider for protocol %s", protocol));
    }

    private ClientProtocolProvider getCaliforniumProtocolProviderSupportingOscore(Protocol protocol) {
        if (protocolToUse.equals(Protocol.COAP)) {
            return new CoapOscoreProtocolProvider();
        }
        throw new IllegalStateException(
                String.format("No Californium Protocol Provider supporting OSCORE for protocol %s", protocol));
    }

    public LeshanTestClientBuilder withAdditiontalAttributes(Map<String, String> attrs) {
        setAdditionalAttributes(attrs);
        return this;
    }

    public LeshanTestClientBuilder named(String endpointName) {
        this.endpointName = endpointName;
        return this;
    }

    public LeshanTestClientBuilder using(Protocol protocol) {
        this.protocolToUse = protocol;
        return this;
    }

    public LeshanTestClientBuilder usingQueueMode() {
        engineFactory.setQueueMode(true);
        return this;
    }

    public LeshanTestClientBuilder usingLifeTimeOf(long lifetime, TimeUnit unit) {
        this.lifetime = unit.toSeconds(lifetime);
        return this;
    }

    public LeshanTestClientBuilder usingPsk(String identity, byte[] key) {
        this.pskIdentity = identity;
        this.pskKey = key;
        return this;
    }

    public LeshanTestClientBuilder connectingTo(LeshanServer server) {
        this.server = server;
        return this;
    }

    public LeshanTestClientBuilder usingBootstrapServerId(int bootstrapServerId) {
        this.bootstrapServerId = bootstrapServerId;
        return this;
    }

    public LeshanTestClientBuilder connectingTo(LeshanBootstrapServer server) {
        this.bootstrapServer = server;
        return this;
    }

    public LeshanTestClientBuilder using(PublicKey clientPublicKey, PrivateKey clientPrivateKey) {
        this.clientPrivateKey = clientPrivateKey;
        this.clientPublicKey = clientPublicKey;
        return this;
    }

    public LeshanTestClientBuilder trusting(PublicKey serverPublicKey) {
        this.serverPublicKey = serverPublicKey;
        return this;
    }

    public LeshanTestClientBuilder trusting(X509Certificate serverCertificate) {
        this.serverCertificate = serverCertificate;
        this.certificageUsage = CertificateUsage.DOMAIN_ISSUER_CERTIFICATE;
        return this;
    }

    public LeshanTestClientBuilder trusting(X509Certificate serverCertificate, CertificateUsage usage,
            X509Certificate... trustedStore) {
        this.serverCertificate = serverCertificate;
        this.certificageUsage = usage;
        this.setTrustStore(Arrays.asList(trustedStore));
        return this;
    }

    public LeshanTestClientBuilder trusting(X509Certificate serverCertificate, CertificateUsage usage) {
        this.serverCertificate = serverCertificate;
        this.certificageUsage = usage;
        return this;
    }

    public LeshanTestClientBuilder using(X509Certificate clientCertificate, PrivateKey clientPrivateKey) {
        this.clientPrivateKey = clientPrivateKey;
        this.clientCertificate = clientCertificate;
        return this;
    }

    public LeshanTestClientBuilder using(OscoreSetting oscoreSetting) {
        this.oscoreSetting = oscoreSetting;
        return this;
    }

    public LeshanTestClientBuilder preferring(ContentFormat contentFormat) {
        this.engineFactory.setPreferredContentFormat(contentFormat);
        return this;
    }

    public LeshanTestClientBuilder supporting(ContentFormat... supportedContentFormat) {
        this.supportedContentFormat = supportedContentFormat;
        return this;
    }

    public LeshanTestClientBuilder withBootstrap(Map<String, String> additionalAttributes) {
        setBootstrapAdditionalAttributes(additionalAttributes);
        return this;
    }

    public LeshanTestClientBuilder withOneSimpleInstancesForObjects(int... objectIds) {
        for (int id : objectIds) {
            initializer.setInstancesForObject(id, new SimpleInstanceEnabler());
        }
        return this;
    }

    public LeshanTestClientBuilder withInstancesForObject(int objectId, LwM2mInstanceEnabler... instances) {
        initializer.setInstancesForObject(objectId, instances);
        return this;
    }

    public LeshanTestClientBuilder enabling(int objectId, Class<? extends LwM2mInstanceEnabler> clazz) {
        initializer.setClassForObject(objectId, clazz);
        return this;
    }

    public static class TestDevice extends Device {

        public TestDevice() {
            super();
        }

        public TestDevice(String manufacturer, String modelNumber, String serialNumber) {
            super(manufacturer, modelNumber, serialNumber);
        }

        @Override
        public ExecuteResponse execute(ServerIdentity identity, int resourceid, Arguments arguments) {
            if (resourceid == 4) {
                return ExecuteResponse.success();
            } else {
                return super.execute(identity, resourceid, arguments);
            }
        }
    }
}
