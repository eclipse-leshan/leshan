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

package org.eclipse.leshan.integration.tests;

import static org.junit.Assert.assertEquals;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class DiscoverTest {

    private IntegrationTestHelper helper = new IntegrationTestHelper();

    @Before
    public void start() {
        helper.start();
    }

    @After
    public void stop() {
        helper.stop();
    }

    @Ignore
    @Test
    public void can_discover_object() {
        // TODO implement discover
    }

    @Ignore
    @Test(expected = IllegalArgumentException.class)
    public void illegal_object_links_request_two() {
        // TODO implement discover
    }

    @Ignore
    @Test
    public void can_discover_object_instance() {
        // TODO implement discover
    }

    @Ignore
    @Test
    public void can_discover_resource() {
        // TODO implement discover
    }

    @Ignore
    @Test
    public void cant_discover_non_existent_resource() {
        // TODO implement discover
    }

    private void assertLinkFormatResponse(final DiscoverResponse response, final ResponseCode responseCode,
            final LinkObject[] expectedObjects) {
        assertEquals(responseCode, response.getCode());

        final LinkObject[] actualObjects = response.getObjectLinks();

        assertEquals(expectedObjects.length, actualObjects.length);
        for (int i = 0; i < expectedObjects.length; i++) {
            assertEquals(expectedObjects[i].toString(), actualObjects[i].toString());
        }
    }

}
