/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
package org.eclipse.leshan.server.demo.cluster.serialization;

import static org.junit.Assert.assertEquals;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.server.client.Client;
import org.junit.Test;

public class ClientSerDesTest {

    @Test
    public void ser_and_des_are_equals() throws Exception {
        LinkObject[] objs = new LinkObject[2];
        Map<String, Object> att = new HashMap<>();
        att.put("ts", new Integer(12));
        att.put("rt", "test");
        objs[0] = new LinkObject("/0/1024/2", att);
        objs[1] = new LinkObject("/0/2");

        Client.Builder builder = new Client.Builder("registrationId", "endpoint", Inet4Address.getByName("127.0.0.1"),
                1, new InetSocketAddress(212)).objectLinks(objs);

        builder.registrationDate(new Date(100L));
        builder.lastUpdate(new Date(101L));
        Client c = builder.build();

        byte[] ser = ClientSerDes.serialize(c);
        assertEquals(
                "{\"regDate\":100,\"address\":\"127.0.0.1\",\"port\":1,\"regAddr\":\"0.0.0.0\",\"regPort\":212,\"lt\":86400,\"ver\":\"1.0\",\"bnd\":\"U\",\"ep\":\"endpoint\",\"regId\":\"registrationId\",\"objLink\":[{\"url\":\"/0/1024/2\",\"at\":{\"rt\":\"test\",\"ts\":12}},{\"url\":\"/0/2\",\"at\":{}}],\"addAttr\":{},\"root\":\"/\",\"lastUp\":101}",
                new String(ser));
        Client c2 = ClientSerDes.deserialize(ser);

        assertEquals(c, c2);
    }
}
