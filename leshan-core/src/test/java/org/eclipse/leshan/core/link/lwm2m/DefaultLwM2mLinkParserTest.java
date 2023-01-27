/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.core.link.lwm2m;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.link.attributes.AttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.junit.jupiter.api.Test;

public class DefaultLwM2mLinkParserTest {

    private final LwM2mLinkParser parser = new DefaultLwM2mLinkParser();

    @Test
    public void check_invalid_values() throws LinkParseException {
        // first check it's OK with valid value
        LwM2mLink[] parsed = parser.parseLwM2mLinkFromCoreLinkFormat("</3/0/11>;dim=255".getBytes(), null);
        assertEquals(new LwM2mPath(3, 0, 11), parsed[0].getPath());
        AttributeSet attResult = new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.DIMENSION, 255l));
        assertEquals(attResult, parsed[0].getAttributes());

        // then check an invalid one
        assertThrowsExactly(LinkParseException.class, () -> {
            // dim should be between 0-255
            parser.parseLwM2mLinkFromCoreLinkFormat("</3/0/11>;dim=256".getBytes(), null);
        });

        // first check it's OK with valid value
        parsed = parser.parseLwM2mLinkFromCoreLinkFormat("</0/1>;ssid=1".getBytes(), null);
        assertEquals(new LwM2mPath(0, 1), parsed[0].getPath());
        attResult = new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.SHORT_SERVER_ID, 1l));
        assertEquals(attResult, parsed[0].getAttributes());

        // then check an invalid one
        assertThrowsExactly(LinkParseException.class, () -> {
            // ssid should be between 1-65534
            parser.parseLwM2mLinkFromCoreLinkFormat("</0/1>;ssid=0".getBytes(), null);
        });
    }

    @Test
    public void check_attribute_with_no_value_failed() throws LinkParseException {
        // first check it's OK with value
        LwM2mLink[] parsed = parser.parseLwM2mLinkFromCoreLinkFormat("</3/0/11>;pmin=200".getBytes(), null);
        assertEquals(new LwM2mPath(3, 0, 11), parsed[0].getPath());
        AttributeSet attResult = new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 200l));
        assertEquals(attResult, parsed[0].getAttributes());

        // then check an invalid one
        assertThrowsExactly(LinkParseException.class, () -> {
            // dim should be between 0-255
            parser.parseLwM2mLinkFromCoreLinkFormat("</3/0/11>;pmin".getBytes(), null);
        });
    }
}
