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
package org.eclipse.leshan.integration.tests.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.LwM2mResponse;

public class TestUtil {
    public static void assertContentFormat(ContentFormat expectedContentFormat, LwM2mResponse response) {
        assertNotNull(response, "response must not be null");
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
        Response coapResponse = (Response) response.getCoapResponse();
        assertTrue(coapResponse.getOptions().hasContentFormat(), "response must have content format");
        assertEquals(expectedContentFormat.getCode(), coapResponse.getOptions().getContentFormat());
    }
}
