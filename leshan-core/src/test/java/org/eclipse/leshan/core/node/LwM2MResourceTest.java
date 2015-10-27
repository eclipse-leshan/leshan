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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class LwM2MResourceTest {

    @Test
    public void two_identical_strings_are_equal() {
        assertEquals(LwM2mSingleResource.newStringResource(10, "hello"),
                LwM2mSingleResource.newStringResource(10, "hello"));
    }

    @Test
    public void two_non_identical_strings_are_not_equal() {
        assertNotEquals(LwM2mSingleResource.newStringResource(10, "hello"),
                LwM2mSingleResource.newStringResource(10, "world"));
        assertNotEquals(LwM2mSingleResource.newStringResource(11, "hello"),
                LwM2mSingleResource.newStringResource(10, "hello"));
    }

    @Test
    public void two_identical_opaques_are_equal() {
        assertEquals(LwM2mSingleResource.newBinaryResource(10, "hello".getBytes()),
                LwM2mSingleResource.newBinaryResource(10, "hello".getBytes()));
    }

    @Test
    public void two_non_identical_opaques_are_not_equal() {
        assertNotEquals(LwM2mSingleResource.newBinaryResource(10, "hello".getBytes()),
                LwM2mSingleResource.newBinaryResource(10, "world".getBytes()));
        assertNotEquals(LwM2mSingleResource.newBinaryResource(11, "hello".getBytes()),
                LwM2mSingleResource.newBinaryResource(10, "hello".getBytes()));
    }

    @Test
    public void two_string_and_binary_are_not_equal() {
        assertNotEquals(LwM2mSingleResource.newStringResource(10, "hello"),
                LwM2mSingleResource.newBinaryResource(10, "hello".getBytes()));
    }

    @Test
    public void two_identical_multiple_strings_are_equal() {
        Map<Integer, String> values1 = new HashMap<>();
        values1.put(0, "hello");
        Map<Integer, String> values2 = new HashMap<>();
        values2.put(0, "hello");

        assertEquals(LwM2mMultipleResource.newStringResource(10, values1),
                LwM2mMultipleResource.newStringResource(10, values2));
    }

    @Test
    public void two_non_identical_multiple_strings_are_not_equal() {
        Map<Integer, String> values1 = new HashMap<>();
        values1.put(0, "hello");
        Map<Integer, String> values2 = new HashMap<>();
        values2.put(0, "world");

        assertNotEquals(LwM2mMultipleResource.newStringResource(10, values1),
                LwM2mMultipleResource.newStringResource(10, values2));
        assertNotEquals(LwM2mMultipleResource.newStringResource(11, values1),
                LwM2mMultipleResource.newStringResource(10, values1));
    }

    @Test
    public void two_identical_multiple_opaques_are_equal() {
        Map<Integer, byte[]> values1 = new HashMap<>();
        values1.put(0, "hello".getBytes());
        Map<Integer, byte[]> values2 = new HashMap<>();
        values2.put(0, "hello".getBytes());

        assertEquals(LwM2mMultipleResource.newBinaryResource(10, values1),
                LwM2mMultipleResource.newBinaryResource(10, values2));
    }

    @Test
    public void two_non_identical_multiple_opaques_are_not_equal() {
        Map<Integer, byte[]> values1 = new HashMap<>();
        values1.put(0, "hello".getBytes());
        Map<Integer, byte[]> values2 = new HashMap<>();
        values2.put(0, "world".getBytes());

        assertNotEquals(LwM2mMultipleResource.newBinaryResource(10, values1),
                LwM2mMultipleResource.newBinaryResource(10, values2));
        assertNotEquals(LwM2mMultipleResource.newBinaryResource(11, values1),
                LwM2mMultipleResource.newBinaryResource(10, values1));
    }

    @Test
    public void two_multiple_string_and_multiple_binary_are_not_equal() {
        Map<Integer, String> values1 = new HashMap<>();
        values1.put(0, "hello");
        Map<Integer, byte[]> values2 = new HashMap<>();
        values2.put(0, "hello".getBytes());

        assertNotEquals(LwM2mMultipleResource.newStringResource(10, values1),
                LwM2mMultipleResource.newBinaryResource(10, values2));
    }
}
