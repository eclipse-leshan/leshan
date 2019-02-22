/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use ServerIdentity
 *     Achim Kraus (Bosch Software Innovations GmbH) - implement REPLACE/UPDATE
 *******************************************************************************/
package org.eclipse.leshan.client.util;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.LwM2mId;
import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.junit.Test;

public class ObjectEnablerTest {

    @Test
    public void check_callback_on_delete() throws InterruptedException {
        ObjectsInitializer initializer = new ObjectsInitializer();
        TestInstanceEnabler instanceEnabler = new TestInstanceEnabler();
        initializer.setInstancesForObject(LwM2mId.ACCESS_CONTROL, instanceEnabler);
        LwM2mObjectEnabler objectEnabler = initializer.create(LwM2mId.ACCESS_CONTROL);

        objectEnabler.delete(ServerIdentity.SYSTEM, new DeleteRequest(LwM2mId.ACCESS_CONTROL, instanceEnabler.getId()));
        assertTrue("callback delete should have been called", instanceEnabler.waitForDelete(2, TimeUnit.SECONDS));
    }

    public static class TestInstanceEnabler extends BaseInstanceEnabler {

        CountDownLatch onDelete = new CountDownLatch(1);

        @Override
        public void onDelete(ServerIdentity identity) {
            onDelete.countDown();
        }

        public boolean waitForDelete(long timeout, TimeUnit unit) throws InterruptedException {
            return onDelete.await(timeout, unit);
        }
    }
}
