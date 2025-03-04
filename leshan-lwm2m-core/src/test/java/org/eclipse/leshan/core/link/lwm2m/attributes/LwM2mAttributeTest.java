/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Natalia Krzykała Orange Polska S.A. - initial implementation
 *******************************************************************************/
package org.eclipse.leshan.core.link.lwm2m.attributes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class LwM2mAttributeTest {
    @Test
    void assertEqualsHashcode() {
        EqualsVerifier.forClass(LwM2mAttribute.class).verify();
    }

    @Test
    void assertEqualsHashcodeWithBigDecimalAttribute() {

        LwM2mAttribute<BigDecimal> a1 = LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, new BigDecimal("1.0"));
        LwM2mAttribute<BigDecimal> a2 = LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, new BigDecimal("1"));

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
    }
}
