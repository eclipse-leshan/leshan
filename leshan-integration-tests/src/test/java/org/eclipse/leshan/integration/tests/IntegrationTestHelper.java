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

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.LwM2mId;
import org.eclipse.leshan.client.LwM2mClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Operations;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.client.Registration;
import org.eclipse.leshan.server.client.RegistrationListener;
import org.eclipse.leshan.server.client.RegistrationUpdate;
import org.eclipse.leshan.server.impl.InMemorySecurityStore;
import org.eclipse.leshan.server.impl.RegistrationServiceImpl;
import org.eclipse.leshan.server.model.StaticModelProvider;

/**
 * Helper for running a server and executing a client against it.
 * 
 */
public class IntegrationTestHelper {
    public static final Random r = new Random();
    
    static final String MODEL_NUMBER = "IT-TEST-123";
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

    LeshanServer server;

    LwM2mClient client;
    String currentEndpointIdentifier;

    CountDownLatch registerLatch;
    Registration last_registration;
    CountDownLatch deregisterLatch;
    CountDownLatch updateLatch;

    protected List<ObjectModel> createObjectModels() {
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
        ResourceModel objlnkfield = new ResourceModel(OBJLNK_MULTI_INSTANCE_RESOURCE_ID, "objlnk", Operations.RW, true, false, Type.OBJLNK, 
                null, null, null);
        ResourceModel objlnkSinglefield = new ResourceModel(OBJLNK_SINGLE_INSTANCE_RESOURCE_ID, "objlnk", Operations.RW, false, false, Type.OBJLNK,
                null, null, null);
        objectModels.add(new ObjectModel(TEST_OBJECT_ID, "testobject", null, false, false, stringfield, booleanfield,
                integerfield, floatfield, timefield, opaquefield, objlnkfield, objlnkSinglefield));

        return objectModels;
    }

    public void initialize() {
        currentEndpointIdentifier = "leshan_integration_test_" + r.nextInt();
    }

    public String getCurrentEndpoint() {
        return currentEndpointIdentifier;
    }

    public void createClient() {
        // Create objects Enabler
        ObjectsInitializer initializer = new ObjectsInitializer(new LwM2mModel(createObjectModels()));
        initializer.setInstancesForObject(LwM2mId.SECURITY, Security.noSec(
                "coap://" + server.getNonSecureAddress().getHostString() + ":" + server.getNonSecureAddress().getPort(),
                12345));
        initializer.setInstancesForObject(LwM2mId.SERVER, new Server(12345, LIFETIME, BindingMode.U, false));
        initializer.setInstancesForObject(LwM2mId.DEVICE, new Device("Eclipse Leshan", MODEL_NUMBER, "12345", "U") {
            @Override
            public ExecuteResponse execute(int resourceid, String params) {
                if (resourceid == 4) {
                    return ExecuteResponse.success();
                } else {
                    return super.execute(resourceid, params);
                }
            }
        });
        List<LwM2mObjectEnabler> objects = initializer.createMandatory();
        objects.addAll(initializer.create(2, 2000));

        // Build Client
        LeshanClientBuilder builder = new LeshanClientBuilder(currentEndpointIdentifier);
        builder.setObjects(objects);
        client = builder.build();
    }

    public void createServer() {
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setObjectModelProvider(new StaticModelProvider(createObjectModels()));
        builder.setLocalAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        builder.setLocalSecureAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        builder.setSecurityStore(new InMemorySecurityStore());
        server = builder.build();
        // monitor client registration
        resetLatch();
        server.getRegistrationService().addListener(new RegistrationListener() {
            @Override
            public void updated(RegistrationUpdate update, Registration updatedRegistration) {
                if (updatedRegistration.getEndpoint().equals(currentEndpointIdentifier)) {
                    updateLatch.countDown();
                }
            }

            @Override
            public void unregistered(Registration registration) {
                if (registration.getEndpoint().equals(currentEndpointIdentifier)) {
                    deregisterLatch.countDown();
                }
            }

            @Override
            public void registered(Registration registration) {
                if (registration.getEndpoint().equals(currentEndpointIdentifier)) {
                    last_registration = registration;
                    registerLatch.countDown();
                }
            }
        });
    }

    public void resetLatch() {
        registerLatch = new CountDownLatch(1);
        deregisterLatch = new CountDownLatch(1);
        updateLatch = new CountDownLatch(1);
    }

    public boolean waitForRegistration(long timeInSeconds) {
        try {
            return registerLatch.await(timeInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean waitForUpdate(long timeInSeconds) {
        try {
            return updateLatch.await(timeInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean waitForDeregistration(long timeInSeconds) {
        try {
            return deregisterLatch.await(timeInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Registration getCurrentRegistration() {
        return server.getRegistrationService().getByEndpoint(currentEndpointIdentifier);
    }

    public void deregisterClient() {
        Registration r = getCurrentRegistration();
        if (r != null)
            ((RegistrationServiceImpl) server.getRegistrationService()).deregisterClient(r.getId());
    }

    public void dispose() {
        deregisterClient();
        currentEndpointIdentifier = null;
    }

    public void assertClientRegisterered() {
        assertNotNull(getCurrentRegistration());
    }

    public void assertClientNotRegisterered() {
        assertNull(getCurrentRegistration());
    }
}
