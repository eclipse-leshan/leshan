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
package org.eclipse.leshan.core.link.lwm2m.attributes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.link.attributes.InvalidAttributeException;
import org.junit.jupiter.api.Test;

public class DefaultLwM2mAttributeParserTest {

    private final DefaultLwM2mAttributeParser parser = new DefaultLwM2mAttributeParser();

    @Test
    public void check_attribute_with_no_value_failed() throws LinkParseException, InvalidAttributeException {
        // first check it's OK with value
        LwM2mAttributeSet parsed = new LwM2mAttributeSet(parser.parseUriQuery("pmin&pmax=60&gt=2"));
        LwM2mAttributeSet attResult = new LwM2mAttributeSet( //
                LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD), //
                LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 60l), //
                LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, 2d));

        assertEquals(attResult, parsed);
    }
}
