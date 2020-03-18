/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.client.californium;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectEnabler;
import org.eclipse.leshan.core.request.ContentFormat;
import org.junit.Test;

public class LeshanBuilderTest {

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_create_client_with_same_object_twice() {
        ObjectEnabler objectEnabler = new ObjectEnabler(1, null, new HashMap<Integer, LwM2mInstanceEnabler>(), null,
                ContentFormat.DEFAULT);
        ObjectEnabler objectEnabler2 = new ObjectEnabler(1, null, new HashMap<Integer, LwM2mInstanceEnabler>(), null,
                ContentFormat.DEFAULT);
        ArrayList<LwM2mObjectEnabler> objects = new ArrayList<>();
        objects.add(objectEnabler);
        objects.add(objectEnabler2);
        new LeshanClientBuilder("test").setObjects(objects).build();
    }
}
