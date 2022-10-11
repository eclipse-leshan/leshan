/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Zebra Technologies - initial API and implementation
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.leshan.client.LeshanClient;
import org.eclipse.leshan.client.LeshanClientBuilder;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpoint;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpointsProvider;
import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.client.object.LwM2mTestObject;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.send.ManualDataSender;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.DefaultLinkSerializer;
import org.eclipse.leshan.core.link.LinkParser;
import org.eclipse.leshan.core.link.LinkSerializer;
import org.eclipse.leshan.core.link.lwm2m.DefaultLwM2mLinkParser;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.request.argument.Arguments;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.util.TestLwM2mId;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumServerEndpoint;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumServerEndpointsProvider;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumServerEndpointsProvider.Builder;
import org.eclipse.leshan.server.californium.endpoint.coap.CoapOscoreServerEndpointFactory;
import org.eclipse.leshan.server.californium.endpoint.coap.CoapServerProtocolProvider;
import org.eclipse.leshan.server.californium.endpoint.coaps.CoapsServerProtocolProvider;
import org.eclipse.leshan.server.model.VersionedModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationServiceImpl;
import org.eclipse.leshan.server.security.DefaultAuthorizer;
import org.eclipse.leshan.server.security.InMemorySecurityStore;
import org.eclipse.leshan.server.security.SecurityStore;

/**
 * Helper for running a server and executing a client against it.
 *
 */
public class IntegrationTestHelper {
    public static final Random r = new Random();

    public static final String MODEL_NUMBER = "IT-TEST-123";
    public static final long LIFETIME = 2;

    public static final LinkParser linkParser = new DefaultLwM2mLinkParser();
    public static final LinkSerializer linkSerializer = new DefaultLinkSerializer();

    public LeshanServer server;
    public LeshanClient client;
    public AtomicReference<String> currentEndpointIdentifier = new AtomicReference<String>();

    private final SynchronousClientObserver clientObserver = new SynchronousClientObserver();
    private final SynchronousRegistrationListener registrationListener = new SynchronousRegistrationListener() {
        @Override
        public boolean accept(Registration registration) {
            return (registration != null && registration.getEndpoint().equals(currentEndpointIdentifier.get()));
        }
    };

    private boolean useOscore = false;

    public List<ObjectModel> createObjectModels() {
        // load default object from the spec
        List<ObjectModel> objectModels = TestObjectLoader.loadDefaultObject();

        return objectModels;
    }

    public void initialize() {
        currentEndpointIdentifier.set("leshan_integration_test_" + r.nextInt());
    }

    public void setCurrentEndpoint(String endpoint) {
        currentEndpointIdentifier.set(endpoint);
    }

    public String getCurrentEndpoint() {
        return currentEndpointIdentifier.get();
    }

    public void createClient() {
        createClient(null);
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

    protected ObjectsInitializer createObjectsInitializer() {
        return new TestObjectsInitializer(new StaticModel(createObjectModels()));
    }

    public void createClient(Map<String, String> additionalAttributes) {
        // Create objects Enabler
        ObjectsInitializer initializer = createObjectsInitializer();
        initializer.setInstancesForObject(LwM2mId.SECURITY,
                Security.noSec(server.getEndpoint(Protocol.COAP).getURI().toString(), 12345));
        initializer.setInstancesForObject(LwM2mId.SERVER, new Server(12345, LIFETIME));
        initializer.setInstancesForObject(LwM2mId.DEVICE, new TestDevice("Eclipse Leshan", MODEL_NUMBER, "12345"));
        initializer.setClassForObject(LwM2mId.ACCESS_CONTROL, DummyInstanceEnabler.class);
        initializer.setInstancesForObject(TestLwM2mId.TEST_OBJECT, new LwM2mTestObject());
        List<LwM2mObjectEnabler> objects = initializer.createAll();

        // Build Client
        LeshanClientBuilder builder = new LeshanClientBuilder(currentEndpointIdentifier.get());
        builder.setDecoder(new DefaultLwM2mDecoder(true));
        builder.setEncoder(new DefaultLwM2mEncoder(true));
        builder.setDataSenders(new ManualDataSender());
        builder.setAdditionalAttributes(additionalAttributes);
        builder.setObjects(objects);
        builder.setEndpointsProvider(new CaliforniumClientEndpointsProvider());
        client = builder.build();
        setupClientMonitoring();
    }

    public void createServer() {
        server = createServerBuilder().build();
        // monitor client registration
        setupServerMonitoring();
    }

    public void createOscoreServer() {
        // TODO support OSCORE
        useOscore = true;
        server = createServerBuilder().build();
        // monitor client registration
        setupServerMonitoring();
    }

    protected Builder createEndpointsProviderBuilder() {
        Builder endpointsBuilder = new CaliforniumServerEndpointsProvider.Builder(new CoapServerProtocolProvider(),
                new CoapsServerProtocolProvider());
        if (useOscore) {
            endpointsBuilder.addEndpoint(new CoapOscoreServerEndpointFactory(
                    EndpointUriUtil.createUri("coap", new InetSocketAddress(InetAddress.getLoopbackAddress(), 0))));
        } else {
            endpointsBuilder.addEndpoint(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), Protocol.COAP);
        }
        return endpointsBuilder;
    }

    protected LeshanServerBuilder createServerBuilder() {
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setDecoder(new DefaultLwM2mDecoder(true));
        builder.setEncoder(new DefaultLwM2mEncoder(true));
        builder.setObjectModelProvider(new VersionedModelProvider(createObjectModels()));

        builder.setEndpointsProvider(createEndpointsProviderBuilder().build());
        SecurityStore securityStore = createSecurityStore();
        builder.setSecurityStore(securityStore);
        builder.setAuthorizer(new DefaultAuthorizer(securityStore) {
            @Override
            public Registration isAuthorized(UplinkRequest<?> request, Registration registration,
                    Identity senderIdentity) {
                assertThat(request.getCoapRequest(), instanceOf(Request.class));
                return super.isAuthorized(request, registration, senderIdentity);
            }
        });
        return builder;
    }

    protected SecurityStore createSecurityStore() {
        return new InMemorySecurityStore();
    }

    protected void setupServerMonitoring() {
        server.getRegistrationService().addListener(registrationListener);
    }

    protected void setupClientMonitoring() {
        client.addObserver(clientObserver);
    }

    public void waitForRegistrationAtClientSide(long timeInSeconds) {
        try {
            assertTrue(clientObserver.waitForRegistration(timeInSeconds, TimeUnit.SECONDS));
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void waitForRegistrationAtServerSide(long timeInSeconds) {
        try {
            registrationListener.waitForRegister(timeInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void ensureNoRegistration(long timeInSeconds) {
        try {
            registrationListener.waitForRegister(timeInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            // timeou means no registration
            return;
        }
        fail("No registration expected");
    }

    public void waitForUpdateAtClientSide(long timeInSeconds) {
        try {
            assertTrue(clientObserver.waitForUpdate(timeInSeconds, TimeUnit.SECONDS));
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void waitForUpdateFailureAtClientSide(long timeInSeconds) {
        try {
            assertFalse(clientObserver.waitForUpdate(timeInSeconds, TimeUnit.SECONDS));
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void waitForBootstrapFinishedAtClientSide(long timeInSeconds) {
        try {
            assertTrue(clientObserver.waitForBootstrap(timeInSeconds, TimeUnit.SECONDS));
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void waitForInconsistentStateAtClientSide(long timeInSeconds) {
        try {
            assertTrue(clientObserver.waitForInconsistenState(timeInSeconds, TimeUnit.SECONDS));
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void ensureNoUpdate(long timeInSeconds) {
        try {
            registrationListener.waitForUpdate(timeInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            // timeout means no registration
            return;
        }
        fail("No update registration expected");
    }

    public void waitForDeregistrationAtServerSide(long timeInSeconds) {
        try {
            registrationListener.waitForDeregister(timeInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void waitForDeregistrationAtClientSide(long timeInSeconds) {
        try {
            assertTrue(clientObserver.waitForDeregistration(timeInSeconds, TimeUnit.SECONDS));
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void ensureNoDeregistration(long timeInSeconds) {
        try {
            registrationListener.waitForDeregister(timeInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            // timeout means no registration
            return;
        }
        fail("No de-registration expected");
    }

    public Registration getCurrentRegistration() {
        return server.getRegistrationService().getByEndpoint(currentEndpointIdentifier.get());
    }

    public ServerIdentity getCurrentRegisteredServer() {
        Map<String, ServerIdentity> registeredServers = client.getRegisteredServers();
        if (registeredServers != null && !registeredServers.isEmpty())
            return registeredServers.values().iterator().next();
        return null;
    }

    public void deregisterClient() {
        Registration r = getCurrentRegistration();
        if (r != null)
            ((RegistrationServiceImpl) server.getRegistrationService()).getStore().removeRegistration(r.getId());
    }

    public void dispose() {
        deregisterClient();
        currentEndpointIdentifier.set(null);
    }

    public void assertClientRegisterered() {
        assertNotNull(getCurrentRegistration());
    }

    public void assertClientNotRegisterered() {
        assertNull(getCurrentRegistration());
    }

    public Registration getLastRegistration() {
        return registrationListener.getLastRegistration();
    }

    public Connector getClientConnector(ServerIdentity server) {
        CaliforniumClientEndpoint endpoint = (CaliforniumClientEndpoint) client.getEndpoint(server);
        return ((CoapEndpoint) endpoint.getCoapEndpoint()).getConnector();
    }

    public DTLSConnector getServerDTLSConnector() {
        CaliforniumServerEndpoint endpoint = (CaliforniumServerEndpoint) server.getEndpoint(Protocol.COAPS);
        return (DTLSConnector) endpoint.getCoapEndpoint().getConnector();
    }
}
