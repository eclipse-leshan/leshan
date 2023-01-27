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

import static org.eclipse.leshan.core.ResponseCode.BAD_REQUEST;
import static org.eclipse.leshan.core.ResponseCode.CONTENT;
import static org.eclipse.leshan.integration.tests.util.TestUtil.assertContentFormat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.stream.Stream;

import org.eclipse.leshan.client.object.LwM2mTestObject;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.util.TestLwM2mId;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ReadSingleValueTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("contentFormats")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllContentFormat {
    }

    static Stream<ContentFormat> contentFormats() {
        return Stream.of(//
                ContentFormat.TEXT, //
                ContentFormat.TLV, //
                ContentFormat.CBOR, //
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
    public void can_read_resource(ContentFormat contentFormat) throws InterruptedException {
        // read device model number
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, 3, 0, 1));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertContentFormat(contentFormat, response);

        LwM2mResource resource = (LwM2mResource) response.getContent();
        assertEquals(1, resource.getId());
        assertEquals(IntegrationTestHelper.MODEL_NUMBER, resource.getValue());

    }

    @TestAllContentFormat
    public void can_read_resource_instance(ContentFormat contentFormat) throws InterruptedException {
        // read resource instance
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.MULTIPLE_STRING_VALUE, 0));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertContentFormat(contentFormat, response);

        LwM2mResourceInstance resourceInstance = (LwM2mResourceInstance) response.getContent();
        assertEquals(0, resourceInstance.getId());
        assertEquals(LwM2mTestObject.INITIAL_STRING_VALUE, resourceInstance.getValue());
    }

    @TestAllContentFormat
    public void cannot_read_non_multiple_resource_instance(ContentFormat contentFormat) throws InterruptedException {
        // read single instance resource
        ReadResponse response = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, TestLwM2mId.TEST_OBJECT, 0, TestLwM2mId.INTEGER_VALUE, 0));

        // verify result
        assertEquals(BAD_REQUEST, response.getCode());
        assertEquals("invalid path : resource is not multiple", response.getErrorMessage());
    }
}
