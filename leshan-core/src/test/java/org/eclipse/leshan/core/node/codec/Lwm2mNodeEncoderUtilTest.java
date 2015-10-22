/*******************************************************************************
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
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
 *     Kai Hudalla (Bosch Software Innovations GmbH) - initial creation
 *******************************************************************************/
package org.eclipse.leshan.core.node.codec;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.Value;
import org.eclipse.leshan.core.node.Value.DataType;
import org.junit.Test;

public class Lwm2mNodeEncoderUtilTest {

    @Test
    public void testConvertValueParsesHexString() {
        Value<String> hexString = Value.newStringValue("10FF"); // 16 * 256 + 255 = 4351
        Value<?> opaqueValue = Lwm2mNodeEncoderUtil.convertValue(hexString, Type.OPAQUE);
        assertThat(opaqueValue.type, is(DataType.OPAQUE));
        assertThat(opaqueValue.value, instanceOf(byte[].class));
        assertThat((byte[]) opaqueValue.value, is(new byte[] { (byte) 0x10, (byte) 0xff }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertValueDetectsNonHexChars() {
        Value<String> hexString = Value.newStringValue("10R8");
        Lwm2mNodeEncoderUtil.convertValue(hexString, Type.OPAQUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertValueDetectsInvalidHexStringLength() {
        Value<String> hexString = Value.newStringValue("10F");
        Lwm2mNodeEncoderUtil.convertValue(hexString, Type.OPAQUE);
    }

}
