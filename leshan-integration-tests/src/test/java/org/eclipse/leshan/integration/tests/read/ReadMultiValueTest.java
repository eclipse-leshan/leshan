/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Zebra Technologies - initial API and implementation
 *     Achim Kraus (Bosch Software Innovations GmbH) - add test for read security object
 *     Achim Kraus (Bosch Software Innovations GmbH) - replace close() with destroy()
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.read;

import static org.eclipse.leshan.core.ResponseCode.CONTENT;
import static org.eclipse.leshan.integration.tests.util.TestUtil.assertContentFormat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.stream.Stream;

import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ReadMultiValueTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("contentFormats")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllContentFormat {
    }

    static Stream<ContentFormat> contentFormats() {
        return Stream.of(//
                ContentFormat.TLV, //
                ContentFormat.JSON, //
                ContentFormat.SENML_JSON, //
                ContentFormat.SENML_CBOR);
    }

    protected IntegrationTestHelper helper = new IntegrationTestHelper();

    @BeforeEach
    public void start() {
        helper.initialize();
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
    }

    @AfterEach
    public void stop() {
        helper.client.destroy(false);
        helper.server.destroy();
        helper.dispose();
    }

    @TestAllContentFormat
    public void can_read_empty_object(ContentFormat contentFormat) throws InterruptedException {
        // read ACL object
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(contentFormat, 2));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertContentFormat(contentFormat, response);

        LwM2mObject object = (LwM2mObject) response.getContent();
        assertEquals(2, object.getId());
        assertTrue(object.getInstances().isEmpty());

    }

    @TestAllContentFormat
    public void can_read_object(ContentFormat contentFormat) throws InterruptedException {
        // read device object
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(contentFormat, 3));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertContentFormat(contentFormat, response);

        LwM2mObject object = (LwM2mObject) response.getContent();
        assertEquals(3, object.getId());

        LwM2mObjectInstance instance = object.getInstance(0);
        assertEquals(0, instance.getId());
    }

    @TestAllContentFormat
    public void can_read_object_instance(ContentFormat contentFormat) throws InterruptedException {
        // read device single instance
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, 3, 0));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertContentFormat(contentFormat, response);

        LwM2mObjectInstance instance = (LwM2mObjectInstance) response.getContent();
        assertEquals(0, instance.getId());
    }
}
