/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
import static org.eclipse.leshan.integration.tests.util.TestUtil.assertContentFormat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.stream.Stream;

import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.core.util.TestLwM2mId;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ReadCompositeTest {

    @ParameterizedTest(name = "{0} - {1}")
    @MethodSource("contentFormats")
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAllContentFormat {
    }

    static Stream<Arguments> contentFormats() {
        return Stream.of(//
                // {request content format, response content format}
                arguments(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON), //
                arguments(ContentFormat.SENML_CBOR, ContentFormat.SENML_CBOR));
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
    public void can_read_resources(ContentFormat requestContentFormat, ContentFormat responseContentFormat)
            throws InterruptedException {
        // read device model number
        ReadCompositeResponse response = helper.server.send(helper.getCurrentRegistration(),
                new ReadCompositeRequest(requestContentFormat, responseContentFormat, "/3/0/0", "/1/0/1"));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertContentFormat(responseContentFormat, response);

        LwM2mSingleResource resource = (LwM2mSingleResource) response.getContent("/3/0/0");
        assertEquals(0, resource.getId());
        assertEquals(Type.STRING, resource.getType());

        resource = (LwM2mSingleResource) response.getContent("/1/0/1");
        assertEquals(1, resource.getId());
        assertEquals(Type.INTEGER, resource.getType());

    }

    @TestAllContentFormat
    public void can_read_resource_instance(ContentFormat requestContentFormat, ContentFormat responseContentFormat)
            throws InterruptedException {
        // read resource instance
        String path = "/" + TestLwM2mId.TEST_OBJECT + "/0/" + TestLwM2mId.MULTIPLE_STRING_VALUE + "/0";
        ReadCompositeResponse response = helper.server.send(helper.getCurrentRegistration(),
                new ReadCompositeRequest(requestContentFormat, responseContentFormat, path));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertContentFormat(responseContentFormat, response);

        LwM2mResourceInstance resource = (LwM2mResourceInstance) response.getContent(path);
        assertEquals(0, resource.getId());
        assertEquals(Type.STRING, resource.getType());

    }

    @TestAllContentFormat
    public void can_read_resource_and_instance(ContentFormat requestContentFormat, ContentFormat responseContentFormat)
            throws InterruptedException {
        // read device model number
        ReadCompositeResponse response = helper.server.send(helper.getCurrentRegistration(),
                new ReadCompositeRequest(requestContentFormat, responseContentFormat, "/3/0/0", "/1"));

        // verify result
        assertEquals(CONTENT, response.getCode());
        assertContentFormat(responseContentFormat, response);

        LwM2mSingleResource resource = (LwM2mSingleResource) response.getContent("/3/0/0");
        assertEquals(0, resource.getId());
        assertEquals(Type.STRING, resource.getType());

        LwM2mObject object = (LwM2mObject) response.getContent("/1");
        assertEquals(1, object.getId());
        assertEquals(1, object.getInstances().size());
    }
}
