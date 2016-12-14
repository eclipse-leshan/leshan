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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.server.client;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.eclipse.leshan.LinkObject;
import org.junit.Assert;
import org.junit.Test;

public class ClientSortObjectTest {

    @Test
    public void sort_link_object_on_get() throws UnknownHostException {
        LinkObject[] objs = new LinkObject[3];
        objs[0] = new LinkObject("/0/1024/2");
        objs[1] = new LinkObject("/0/2");
        objs[2] = null;

        Registration.Builder builder = new Registration.Builder("registrationId", "endpoint", Inet4Address.getByName("127.0.0.1"),
                1, new InetSocketAddress(212)).objectLinks(objs);

        Registration r = builder.build();

        LinkObject[] res = r.getSortedObjectLinks();
        Assert.assertEquals(3, res.length);
        Assert.assertNull(res[0]);
        Assert.assertEquals("/0/2", res[1].getUrl());
        Assert.assertEquals("/0/1024/2", res[2].getUrl());
    }
}
