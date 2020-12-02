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
package org.eclipse.leshan.integration.tests.write;

import static org.eclipse.leshan.integration.tests.util.IntegrationTestHelper.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class WriteOpaqueValueTest {
    protected IntegrationTestHelper helper = new IntegrationTestHelper();

    @Parameters(name = "{0}")
    public static Collection<?> contentFormats() {
        return Arrays.asList(new Object[][] { //
                                { ContentFormat.OPAQUE }, //
                                { ContentFormat.TEXT }, //
                                { ContentFormat.TLV }, //
                                { ContentFormat.CBOR }, //
                                { ContentFormat.fromCode(ContentFormat.OLD_TLV_CODE) }, //
                                { ContentFormat.JSON }, //
                                { ContentFormat.fromCode(ContentFormat.OLD_JSON_CODE) }, //
                                { ContentFormat.SENML_JSON }, //
                                { ContentFormat.SENML_CBOR } });
    }

    private ContentFormat contentFormat;

    public WriteOpaqueValueTest(ContentFormat contentFormat) {
        this.contentFormat = contentFormat;
    }

    @Before
    public void start() {
        helper.initialize();
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
    }

    @After
    public void stop() {
        helper.client.destroy(false);
        helper.server.destroy();
        helper.dispose();
    }

    @Test
    public void write_opaque_resource() throws InterruptedException {
        // write resource
        byte[] expectedvalue = new byte[] { 1, 2, 3 };
        WriteResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(contentFormat, TEST_OBJECT_ID, 0, OPAQUE_RESOURCE_ID, expectedvalue));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, TEST_OBJECT_ID, 0, OPAQUE_RESOURCE_ID));
        LwM2mResource resource = (LwM2mResource) readResponse.getContent();
        assertArrayEquals(expectedvalue, (byte[]) resource.getValue());
    }
}
