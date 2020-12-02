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
package org.eclipse.leshan.integration.tests.read;

import static org.eclipse.leshan.core.ResponseCode.CONTENT;
import static org.eclipse.leshan.integration.tests.util.IntegrationTestHelper.*;
import static org.eclipse.leshan.integration.tests.util.TestUtil.assertContentFormat;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ReadOpaqueValueTest {
    protected IntegrationTestHelper helper = new IntegrationTestHelper();

    @Parameters(name = "{0}")
    public static Collection<?> contentFormats() {
        return Arrays.asList(new Object[][] { //
                                { ContentFormat.OPAQUE }, //
                                { ContentFormat.TEXT }, //
                                { ContentFormat.TLV }, //
                                { ContentFormat.CBOR }, //
                                { ContentFormat.JSON }, //
                                { ContentFormat.SENML_JSON }, //
                                { ContentFormat.SENML_CBOR } });
    }

    private ContentFormat contentFormat;

    public ReadOpaqueValueTest(ContentFormat contentFormat) {
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
    public void can_read_empty_opaque_resource() throws InterruptedException {
        // read device model number
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, TEST_OBJECT_ID, 1, OPAQUE_RESOURCE_ID));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertContentFormat(contentFormat, response);

        LwM2mResource resource = (LwM2mResource) response.getContent();
        assertEquals(Type.OPAQUE, resource.getType());
        assertEquals(0, ((byte[]) resource.getValue()).length);
    }
}
