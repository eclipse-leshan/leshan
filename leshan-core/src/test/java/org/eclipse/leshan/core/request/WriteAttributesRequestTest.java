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
package org.eclipse.leshan.core.request;

import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

public class WriteAttributesRequestTest {

    @Test()
    public void should_throw_on_invalid_pmin_pmax() {
        LwM2mAttributeSet sut = new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 50L),
                LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 49L));

        // pmin cannot be greater then pmax
        assertThrowsExactly(InvalidRequestException.class, () -> {
            new WriteAttributesRequest(3, 0, 9, sut);
        });
    }

    @Test
    public void should_throw_on_invalid_epmin_epmax() {
        LwM2mAttributeSet sut = new LwM2mAttributeSet(
                LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MINIMUM_PERIOD, 50L),
                LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MAXIMUM_PERIOD, 49L));

        // pmin cannot be greater then pmax
        assertThrowsExactly(InvalidRequestException.class, () -> {
            new WriteAttributesRequest(3, 0, 9, sut);
        });
    }

}
