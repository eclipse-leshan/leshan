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

import org.eclipse.leshan.core.node.Value;
import org.junit.Test;

public class ValueTest {

    @Test
    public void two_identical_strings_are_equal() {
        assertEquals(Value.newStringValue("hello"), Value.newStringValue("hello"));
    }

    @Test
    public void two_non_identical_strings_are_not_equal() {
        assertNotEquals(Value.newStringValue("hello"), Value.newStringValue("world"));
    }

    @Test
    public void two_identical_opaques_are_equal() {
        assertEquals(Value.newBinaryValue("hello".getBytes()), Value.newBinaryValue("hello".getBytes()));
    }

    @Test
    public void two_non_identical_opaques_are_not_equal() {
        assertNotEquals(Value.newBinaryValue("hello".getBytes()), Value.newBinaryValue("world".getBytes()));
    }

    @Test
    public void two_string_and_binary_are_not_equal() {
        assertNotEquals(Value.newStringValue("hello"), Value.newBinaryValue("hello".getBytes()));
    }

}
