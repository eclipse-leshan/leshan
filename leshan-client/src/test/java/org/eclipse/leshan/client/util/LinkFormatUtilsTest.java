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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.util;

import static org.junit.Assert.assertEquals;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.client.util.LinkFormatUtils;
import org.junit.Test;

public class LinkFormatUtilsTest {
    private final String VALID_REQUEST_PAYLOAD = "</lwm2m>;rt=\"oma.lwm2m\", </lwm2m/1/101>, </lwm2m/1/102>, </lwm2m/2/0>, </lwm2m/2/1>, </lwm2m/2/2>, </lwm2m/3/0>, </lwm2m/4/0>, </lwm2m/5>";
    private final String VALID_REQUEST_SIMPLE_PAYLOAD = "</1/101>, </1/102>, </2/0>, </2/1>, </2/2>, </3/0>, </4/0>, </5>";
    private final String INVALID_REQUEST_PAYLOAD = "";

    @Test
    public void testValidOne() {
        validateExpectedPayload(VALID_REQUEST_PAYLOAD);
    }

    @Test
    public void testValidTwo() {
        validateExpectedPayload(VALID_REQUEST_SIMPLE_PAYLOAD);
    }

    @Test
    public void testInvalid() {
        final LinkObject[] links = generateLinksFromPayload(INVALID_REQUEST_PAYLOAD);
        final String actualPayload = LinkFormatUtils.payloadize(links);

        assertEquals(LinkFormatUtils.INVALID_LINK_PAYLOAD, actualPayload);
    }

    private void validateExpectedPayload(final String expectedPayload) {
        final LinkObject[] links = generateLinksFromPayload(expectedPayload);
        final String actualPayload = LinkFormatUtils.payloadize(links);

        assertEquals(expectedPayload, actualPayload);
    }

    private LinkObject[] generateLinksFromPayload(final String payload) {
        return LinkObject.parse(payload.getBytes());
    }

}
