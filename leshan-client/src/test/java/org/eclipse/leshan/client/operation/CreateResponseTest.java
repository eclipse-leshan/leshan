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
package org.eclipse.leshan.client.operation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.eclipse.leshan.client.response.CreateResponse;
import org.junit.Test;

public class CreateResponseTest {

    @Test
    public void testEqualityRobustnessForSuccesses() {
        assertEquals(CreateResponse.success(1), CreateResponse.success(1));
        assertNotEquals(CreateResponse.success(1), CreateResponse.success(2));
        assertNotEquals(CreateResponse.success(2), CreateResponse.success(1));
        assertNotEquals(CreateResponse.success(1), CreateResponse.invalidResource());
        assertNotEquals(CreateResponse.success(1), null);
    }

    @Test
    public void testHashCodeRobustnessForSuccesses() {
        assertEquals(CreateResponse.success(1).hashCode(), CreateResponse.success(1).hashCode());
        assertNotEquals(CreateResponse.success(1).hashCode(), CreateResponse.success(2).hashCode());
        assertNotEquals(CreateResponse.success(1).hashCode(), CreateResponse.invalidResource().hashCode());
    }

    @Test
    public void testEqualityRobustnessForFailures() {
        assertEquals(CreateResponse.invalidResource(), CreateResponse.invalidResource());
        assertNotEquals(CreateResponse.invalidResource(), CreateResponse.success(2));
        assertNotEquals(CreateResponse.invalidResource(), null);
    }

    @Test
    public void testHashCodeRobustnessForFailures() {
        assertEquals(CreateResponse.invalidResource().hashCode(), CreateResponse.invalidResource().hashCode());
        assertNotEquals(CreateResponse.invalidResource(), CreateResponse.success(2).hashCode());
    }

}
