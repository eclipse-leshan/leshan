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

package org.eclipse.leshan.core.tlv;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class TlvTest {

    @Test
    public void assertEqualsHashcode() {
        byte[] bytes = { 1, 2, 3, 4, 5 };
        Tlv prefab1 = new Tlv(Tlv.TlvType.OBJECT_INSTANCE, new Tlv[0], null, 1);
        Tlv prefab2 = new Tlv(Tlv.TlvType.RESOURCE_VALUE, null, bytes, 2);

        EqualsVerifier.forClass(Tlv.class).withPrefabValues(Tlv.class, prefab1, prefab2).verify();
    }
}