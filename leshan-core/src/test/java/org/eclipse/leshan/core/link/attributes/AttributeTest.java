/*******************************************************************************
 * Copyright (c) 2022    Sierra Wireless and others.
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
package org.eclipse.leshan.core.link.attributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.link.lwm2m.attributes.AssignationLevel;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.junit.jupiter.api.Test;

public class AttributeTest {

    @Test
    public void should_pick_correct_model() {
        LwM2mAttribute<Version> verAttribute = LwM2mAttributes.create(LwM2mAttributes.OBJECT_VERSION,
                new Version("1.0"));
        assertEquals("ver", verAttribute.getName());
        assertEquals(new Version("1.0"), verAttribute.getValue());
        assertTrue(verAttribute.canBeAssignedTo(AssignationLevel.OBJECT));
        assertFalse(verAttribute.isWritable());
    }
}
