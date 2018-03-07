/*******************************************************************************
 * Copyright (c) 2013-2018 Sierra Wireless and others.
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
 *     Daniel Persson (Husqvarna Group) - Attribute support
 *******************************************************************************/
package org.eclipse.leshan.core.attributes;

import static org.junit.Assert.*;

import java.util.EnumSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AttributeTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();
    
    @Test
    public void should_pick_correct_model() {
        Attribute verAttribute = new Attribute(AttributeName.OBJECT_VERSION, "1.0");
        assertEquals("ver", verAttribute.getCoRELinkParam());
        assertEquals(AttributeName.OBJECT_VERSION, verAttribute.getName());
        assertEquals("1.0", verAttribute.getValue());
        assertTrue(verAttribute.canBeAssignedTo(AssignationLevel.OBJECT));
        assertFalse(verAttribute.isWritable());
    }
    
    @Test
    public void should_throw_on_invalid_value_type() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(AttributeName.OBJECT_VERSION.name());
        new Attribute(AttributeName.OBJECT_VERSION, 123);
    }
}
