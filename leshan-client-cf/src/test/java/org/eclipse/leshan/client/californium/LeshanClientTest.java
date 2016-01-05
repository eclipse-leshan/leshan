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
 *     Zebra Technologies - initial API and implementation
 *     Kai Hudalla (Bosch Software Innovations GmbH) - refactor into separate test class
 *******************************************************************************/
package org.eclipse.leshan.client.californium;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectEnabler;
import org.junit.Test;

public class LeshanClientTest {

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_create_client_with_null() {
        new LeshanClient(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_create_client_with_same_object_twice() {
        List<LwM2mObjectEnabler> objects = new ArrayList<>();
        objects.add(new ObjectEnabler(1, null, new HashMap<Integer, LwM2mInstanceEnabler>(), null));
        objects.add(new ObjectEnabler(1, null, new HashMap<Integer, LwM2mInstanceEnabler>(), null));
        new LeshanClient(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), objects);
    }
}
