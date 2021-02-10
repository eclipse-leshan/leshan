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
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;

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
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.resource.SimpleInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Operations;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
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

    public static final int TEST_OBJECT_ID = 2000;
    public static final int STRING_RESOURCE_ID = 0;
    public static final int BOOLEAN_RESOURCE_ID = 1;
    public static final int INTEGER_RESOURCE_ID = 2;
    public static final int FLOAT_RESOURCE_ID = 3;
    public static final int TIME_RESOURCE_ID = 4;
    public static final int OPAQUE_RESOURCE_ID = 5;
    public static final int OBJLNK_MULTI_INSTANCE_RESOURCE_ID = 6;
    public static final int OBJLNK_SINGLE_INSTANCE_RESOURCE_ID = 7;
    public static final int INTEGER_MANDATORY_RESOURCE_ID = 8;
    public static final int STRING_MANDATORY_RESOURCE_ID = 9;
    public static final int STRING_RESOURCE_INSTANCE_ID = 10;
    public static final int UNSIGNED_INTEGER_RESOURCE_ID = 11;

    public static final String MULTI_INSTANCE = "multiinstance";

    public LeshanServer server;
    public LeshanClient client;
    public AtomicReference<String> currentEndpointIdentifier = new AtomicReference<String>();

    private SynchronousClientObserver clientObserver = new SynchronousClientObserver();
    private SynchronousRegistrationListener registrationListener = new SynchronousRegistrationListener() {
        @Override
        public boolean accept(Registration registration) {
            return (registration != null && registration.getEndpoint().equals(currentEndpointIdentifier.get()));
        }
    };

    public List<ObjectModel> createObjectModels() {
        // load default object from the spec
        List<ObjectModel> objectModels = ObjectLoader.loadDefault();
        // define custom model for testing purpose
        ResourceModel stringfield = new ResourceModel(STRING_RESOURCE_ID, "stringres", Operations.RW, false, false,
                Type.STRING, null, null, null);
        ResourceModel booleanfield = new ResourceModel(BOOLEAN_RESOURCE_ID, "booleanres", Operations.RW, false, false,
                Type.BOOLEAN, null, null, null);
        ResourceModel integerfield = new ResourceModel(INTEGER_RESOURCE_ID, "integerres", Operations.RW, false, false,
                Type.INTEGER, null, null, null);
        ResourceModel floatfield = new ResourceModel(FLOAT_RESOURCE_ID, "floatres", Operations.RW, false, false,
                Type.FLOAT, null, null, null);
        ResourceModel timefield = new ResourceModel(TIME_RESOURCE_ID, "timeres", Operations.RW, false, false, Type.TIME,
                null, null, null);
        ResourceModel opaquefield = new ResourceModel(OPAQUE_RESOURCE_ID, "opaque", Operations.RW, false, false,
                Type.OPAQUE, null, null, null);
        ResourceModel objlnkfield = new ResourceModel(OBJLNK_MULTI_INSTANCE_RESOURCE_ID, "objlnk", Operations.RW, true,
                false, Type.OBJLNK, null, null, null);
        ResourceModel objlnkSinglefield = new ResourceModel(OBJLNK_SINGLE_INSTANCE_RESOURCE_ID, "objlnk", Operations.RW,
                false, false, Type.OBJLNK, null, null, null);
        ResourceModel integermandatoryfield = new ResourceModel(INTEGER_MANDATORY_RESOURCE_ID, "integermandatory",
                Operations.RW, false, true, Type.INTEGER, null, null, null);
        ResourceModel stringmandatoryfield = new ResourceModel(STRING_MANDATORY_RESOURCE_ID, "stringmandatory",
                Operations.RW, false, true, Type.STRING, null, null, null);
        ResourceModel multiInstance = new ResourceModel(STRING_RESOURCE_INSTANCE_ID, MULTI_INSTANCE, Operations.RW,
                true, false, Type.STRING, null, null, null);
        ResourceModel unsignedintegerfield = new ResourceModel(UNSIGNED_INTEGER_RESOURCE_ID, "unsigned", Operations.RW,
                false, false, Type.UNSIGNED_INTEGER, null, null, null);

        objectModels.add(new ObjectModel(TEST_OBJECT_ID, "testobject", null, ObjectModel.DEFAULT_VERSION, true, false,
                stringfield, booleanfield, integerfield, floatfield, timefield, opaquefield, objlnkfield,
                objlnkSinglefield, integermandatoryfield, stringmandatoryfield, multiInstance, unsignedintegerfield));

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
        public ExecuteResponse execute(ServerIdentity identity, int resourceid, String params) {
            if (resourceid == 4) {
                return ExecuteResponse.success();
            } else {
                return super.execute(identity, resourceid, params);
            }
        }
    }

    protected ObjectsInitializer createObjectsInitializer() {
        return new TestObjectsInitializer(new StaticModel(createObjectModels()));
    }

    public void createClient(Map<String, String> additionalAttributes) {
        // Create objects Enabler
        ObjectsInitializer initializer = createObjectsInitializer();
        initializer.setInstancesForObject(LwM2mId.SECURITY, Security.noSec(
                "coap://" + server.getUnsecuredAddress().getHostString() + ":" + server.getUnsecuredAddress().getPort(),
                12345));
        initializer.setInstancesForObject(LwM2mId.SERVER, new Server(12345, LIFETIME));
        initializer.setInstancesForObject(LwM2mId.DEVICE, new TestDevice("Eclipse Leshan", MODEL_NUMBER, "12345"));
        initializer.setClassForObject(LwM2mId.ACCESS_CONTROL, DummyInstanceEnabler.class);
        initializer.setInstancesForObject(TEST_OBJECT_ID, new DummyInstanceEnabler(0),
                new SimpleInstanceEnabler(1, OPAQUE_RESOURCE_ID, new byte[0]));
        List<LwM2mObjectEnabler> objects = initializer.createAll();

        // Build Client
        LeshanClientBuilder builder = new LeshanClientBuilder(currentEndpointIdentifier.get());
        builder.setDecoder(new DefaultLwM2mNodeDecoder(true));
        builder.setEncoder(new DefaultLwM2mNodeEncoder(true));
        builder.setAdditionalAttributes(additionalAttributes);
        builder.setObjects(objects);
        client = builder.build();
        setupClientMonitoring();
    }

    public void createServer() {
        server = createServerBuilder().build();
        // monitor client registration
        setupServerMonitoring();
    }

    protected LeshanServerBuilder createServerBuilder() {
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setDecoder(new DefaultLwM2mNodeDecoder(true));
        builder.setEncoder(new DefaultLwM2mNodeEncoder(true));
        builder.setObjectModelProvider(new VersionedModelProvider(createObjectModels()));
        builder.setLocalAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        builder.setLocalSecureAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
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

    public void waitForBootstrapFinishedAtClientSide(long timeInSeconds) {
        try {
            assertTrue(clientObserver.waitForBootstrap(timeInSeconds, TimeUnit.SECONDS));
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
        CoapEndpoint endpoint = (CoapEndpoint) client.coap().getServer().getEndpoint(client.getAddress(server));
        return endpoint.getConnector();
    }
}
