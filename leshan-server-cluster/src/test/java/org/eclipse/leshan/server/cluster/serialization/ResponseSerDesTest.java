/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
package org.eclipse.leshan.server.cluster.serialization;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.Link;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.junit.Test;

import com.eclipsesource.json.JsonObject;

public class ResponseSerDesTest {

    @Test
    public void ser_and_des_discover_response() throws Exception {
        Link[] objs = new Link[2];
        Map<String, Object> att = new HashMap<>();
        att.put("ts", 12);
        att.put("rt", "test");
        objs[0] = new Link("/0/1024/2", att);
        objs[1] = new Link("/0/2");

        DiscoverResponse dr = DiscoverResponse.success(objs);
        JsonObject obj = ResponseSerDes.jSerialize(dr);
        LwM2mResponse dr2 = ResponseSerDes.deserialize(obj);

        assertEquals(dr.toString(), dr2.toString());
    }
}
