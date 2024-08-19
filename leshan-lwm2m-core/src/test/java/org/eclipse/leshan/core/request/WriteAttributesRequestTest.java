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
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class WriteAttributesRequestTest {

    @Test
    public void should_throw_on_pmin_greater_than_pmax() {
        // The maximum period MUST be greater than the minimum period parameter
        // https://datatracker.ietf.org/doc/html/draft-ietf-core-dynlink-07#section-4.2

        LwM2mAttributeSet sut = new LwM2mAttributeSet( //
                LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 50L), //
                LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 49L));

        // pmin cannot be greater then pmax
        assertThrowsExactly(InvalidRequestException.class, () -> {
            new WriteAttributesRequest(3, 0, 9, sut);
        });
    }

    @Test
    public void should_throw_on_epmin_greater_than_pmax() {
        // The Maximum Evaluation Period MUST be greater than the Minimum Evaluation Period parameter
        // https://datatracker.ietf.org/doc/html/draft-ietf-core-conditional-attributes-06#section-3.2.4

        LwM2mAttributeSet sut = new LwM2mAttributeSet( //
                LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 50L), //
                LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 49L));

        // pmin cannot be greater then pmax
        assertThrowsExactly(InvalidRequestException.class, () -> {
            new WriteAttributesRequest(3, 0, 9, sut);
        });
    }

    @Test
    public void should_throw_on_lt_greater_than_gt() {
        // "lt" value < "gt" value
        // https://www.openmobilealliance.org/release/LightweightM2M/V1_2_1-20221209-A/HTML-Version/OMA-TS-LightweightM2M_Core-V1_2_1-20221209-A.html#7-3-0-73-Attributes

        LwM2mAttributeSet sut = new LwM2mAttributeSet(//
                LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, 50d), //
                LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, 49d));

        // pmin cannot be greater then pmax
        assertThrowsExactly(InvalidRequestException.class, () -> {
            new WriteAttributesRequest(3, 0, 9, sut);
        });
    }

    @Test
    public void should_throw_on_gt_equals_lt() {
        // "lt" value < "gt" value
        // https://www.openmobilealliance.org/release/LightweightM2M/V1_2_1-20221209-A/HTML-Version/OMA-TS-LightweightM2M_Core-V1_2_1-20221209-A.html#7-3-0-73-Attributes

        LwM2mAttributeSet sut = new LwM2mAttributeSet(//
                LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, 50d), //
                LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, 50d));

        // pmin cannot be greater then pmax
        assertThrowsExactly(InvalidRequestException.class, () -> {
            new WriteAttributesRequest(3, 0, 9, sut);
        });
    }

    @Test
    public void should_throw_on_invalid_lt_gt_st_combination() {
        // ("lt" value + 2*"st" values) <"gt" value
        // https://www.openmobilealliance.org/release/LightweightM2M/V1_2_1-20221209-A/HTML-Version/OMA-TS-LightweightM2M_Core-V1_2_1-20221209-A.html#7-3-0-73-Attributes

        LwM2mAttributeSet sut = new LwM2mAttributeSet(//
                LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, 10d), //
                LwM2mAttributes.create(LwM2mAttributes.STEP, 2d), //
                LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, 12d));

        // pmin cannot be greater then pmax
        assertThrowsExactly(InvalidRequestException.class, () -> {
            new WriteAttributesRequest(3, 0, 9, sut);
        });
    }

    public class ExtendedWriteAttributesRequest extends WriteAttributesRequest {
        ExtendedWriteAttributesRequest(LwM2mPath path, LwM2mAttributeSet attributes) {
            super(String.valueOf(path), attributes, null);
        }

        @Override
        public boolean canEqual(Object obj) {
            return (obj instanceof ExtendedWriteAttributesRequest);
        }
    }

    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(WriteAttributesRequest.class).withRedefinedSuperclass()
                .withRedefinedSubclass(ExtendedWriteAttributesRequest.class).withIgnoredFields("coapRequest").verify();
    }
}
