/*******************************************************************************
 * Copyright (c) 2021 Orange.
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
 *     Michał Wadowski (Orange) - Add Observe-Composite feature.
 *******************************************************************************/
package org.eclipse.leshan.core.response;

import static org.eclipse.leshan.core.ResponseCode.CHANGED;
import static org.eclipse.leshan.core.ResponseCode.CONTENT;
import static org.eclipse.leshan.core.node.LwM2mSingleResource.newResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ObserveResponseTest {

    @Parameters(name = "{0}")
    public static Collection<?> responseCodes() {
        return Arrays.asList(CONTENT, CHANGED);
    }

    private final ResponseCode responseCode;

    public ObserveResponseTest(ResponseCode responseCode) {
        this.responseCode = responseCode;
    }

    @Test
    public void should_throw_invalid_response_exception_if_no_content() {
        assertThrows(InvalidResponseException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                new ObserveResponse(responseCode, null, null, null, null);
            }
        });
    }

    @Test
    public void should_throw_invalid_response_exception_if_no_content_and_empty_timestamped_values() {
        assertThrows(InvalidResponseException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                new ObserveResponse(responseCode, null, Collections.<TimestampedLwM2mNode> emptyList(), null, null);
            }
        });
    }

    @Test
    public void should_not_throw_exception_if_has_content() {
        // given
        LwM2mSingleResource exampleResource = newResource(15, "example");

        // when
        ObserveResponse response = new ObserveResponse(responseCode, exampleResource, null, null, null);

        // then
        assertEquals(exampleResource, response.getContent());
        assertNull(response.getTimestampedLwM2mNode());
    }

    @Test
    public void should_get_content_from_first_of_timestamped_values() {
        // given
        List<TimestampedLwM2mNode> timestampedValues = Arrays.asList(
                new TimestampedLwM2mNode(123L, newResource(15, "example 1")),
                new TimestampedLwM2mNode(456L, newResource(15, "example 2")));

        LwM2mSingleResource content = responseCode == CHANGED ? newResource(15, "example 1") : null;

        // when
        ObserveResponse response = new ObserveResponse(responseCode, content, timestampedValues, null, null);

        // then
        assertEquals(timestampedValues.get(0).getNode(), response.getContent());
        assertEquals(timestampedValues, response.getTimestampedLwM2mNode());
    }
}
