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
package org.eclipse.leshan.core.link.attributes;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class BaseAttributeTest {

    private class ExtendedBaseAttribute extends BaseAttribute {
        public ExtendedBaseAttribute(String name, Object value) {
            super(name, value, false);
        }

        @Override
        public boolean canEqual(Object obj) {
            return obj instanceof ExtendedBaseAttribute;
        }

        @Override
        public String getCoreLinkValue() {
            Object val = getValue();
            return val != null ? val.toString() : "";
        }
    }

    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(BaseAttribute.class).withRedefinedSubclass(ExtendedBaseAttribute.class).verify();
    }
}
