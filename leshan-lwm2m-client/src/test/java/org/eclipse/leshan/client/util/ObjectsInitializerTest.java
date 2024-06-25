/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
package org.eclipse.leshan.client.util;

import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.LwM2mId;
import org.junit.jupiter.api.Test;

public class ObjectsInitializerTest {

    @Test()
    public void set_single_instance_with_not_0_id() {
        int bad_id = 10; // single instance should have a 0 id
        ObjectsInitializer objectsInitializer = new ObjectsInitializer();

        assertThrowsExactly(IllegalArgumentException.class, () -> {
            objectsInitializer.setInstancesForObject(LwM2mId.DEVICE, new BaseInstanceEnabler(bad_id));
        });
    }

    @Test
    public void set_instances_with_custom_id() {
        List<Integer> expectedIds = Arrays.asList(10, 20);

        LwM2mInstanceEnabler[] acls = new LwM2mInstanceEnabler[expectedIds.size()];
        for (int i = 0; i < acls.length; i++) {
            LwM2mInstanceEnabler dummyACL = new BaseInstanceEnabler(expectedIds.get(i));
            acls[i] = dummyACL;
        }

        ObjectsInitializer objectsInitializer = new ObjectsInitializer();
        objectsInitializer.setInstancesForObject(LwM2mId.ACCESS_CONTROL, acls);
        LwM2mObjectEnabler AclObject = objectsInitializer.create(LwM2mId.ACCESS_CONTROL);

        List<Integer> availableInstanceIds = AclObject.getAvailableInstanceIds();
        assertTrue(availableInstanceIds.containsAll(expectedIds), "Bad instance id");
    }
}
