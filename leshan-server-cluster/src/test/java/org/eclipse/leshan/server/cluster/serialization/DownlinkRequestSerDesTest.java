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
package org.eclipse.leshan.server.cluster.serialization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.eclipse.leshan.ObserveSpec;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.junit.Test;

import com.eclipsesource.json.JsonObject;

public class DownlinkRequestSerDesTest {

    @Test
    public void ser_and_des_read_request() throws Exception {
        ser_and_des_are_equals(new ReadRequest(ContentFormat.TLV, 3, 0, 1));
    }

    @Test
    public void ser_and_des_execute_request() throws Exception {
        ser_and_des_are_equals(new ExecuteRequest(3, 0, 1, "params"));
    }

    @Test
    public void ser_and_des_delete_request() throws Exception {
        ser_and_des_are_equals(new DeleteRequest(3, 0));
    }

    @Test
    public void ser_and_des_discover_request() throws Exception {
        ser_and_des_are_equals(new DiscoverRequest(3, 0, 1));
    }

    @Test
    public void ser_and_des_observe_request() throws Exception {
        ser_and_des_are_equals(new ObserveRequest(ContentFormat.TLV, 3, 0, 1));
    }

    @Test
    public void ser_and_des_write_request() throws Exception {
        ser_and_des_are_equals(new WriteRequest(Mode.REPLACE, ContentFormat.TLV, 3, 0,
                new LwM2mResource[] { LwM2mSingleResource.newStringResource(1, "value") }));
    }

    @Test
    public void ser_and_des_create_request() throws Exception {
        ser_and_des_are_equals(new CreateRequest(ContentFormat.TLV, 3,
                new LwM2mResource[] { LwM2mSingleResource.newStringResource(1, "value") }));
    }

    @Test
    public void ser_and_des_write_attributes_request() throws Exception {
        ObserveSpec os = new ObserveSpec.Builder().minPeriod(10).maxPeriod(60).build();
        ser_and_des_are_equals(new WriteAttributesRequest(3, 0, 1, os));
    }

    public void ser_and_des_are_equals(DownlinkRequest<?> request) throws Exception {
        JsonObject ser = DownlinkRequestSerDes.jSerialize(request);
        DownlinkRequest<?> r2 = DownlinkRequestSerDes.deserialize(ser);
        assertEquals(request, r2);
    }

    @Test
    public void ser_and_des_read_request_then_compare_to_observe_request() throws Exception {
        ReadRequest readRequest = new ReadRequest(ContentFormat.TLV, 3, 0, 1);

        JsonObject ser = DownlinkRequestSerDes.jSerialize(readRequest);
        DownlinkRequest<?> r2 = DownlinkRequestSerDes.deserialize(ser);
        assertNotEquals(r2, new ObserveRequest(ContentFormat.TLV, 3, 0, 1));
    }

}
