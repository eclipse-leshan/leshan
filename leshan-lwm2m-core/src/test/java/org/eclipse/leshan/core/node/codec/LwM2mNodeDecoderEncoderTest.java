/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
package org.eclipse.leshan.core.node.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.util.TestObjectLoader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class LwM2mNodeDecoderEncoderTest {

    private final LwM2mDecoder decoder = new DefaultLwM2mDecoder();
    private final LwM2mEncoder encoder = new DefaultLwM2mEncoder();
    private final LwM2mModel model = new StaticModel(TestObjectLoader.loadAllDefault());

    static Stream<org.junit.jupiter.params.provider.Arguments> contentFormats() {
        return Stream.of(//
                arguments(ContentFormat.JSON), //
                arguments(ContentFormat.SENML_JSON), //
                arguments(ContentFormat.SENML_CBOR));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contentFormats")
    public void encode_decode_timestamped_value(ContentFormat format) {

        // resource used for test
        LwM2mPath resourcePath = new LwM2mPath("3442/0/120");
        Instant t1 = Instant.parse("2024-05-23T21:55:00.556635828Z");
        LwM2mSingleResource resource = LwM2mSingleResource.newIntegerResource(resourcePath.getResourceId(), 3600);
        List<TimestampedLwM2mNode> timestampedData = Arrays.asList(new TimestampedLwM2mNode(t1, resource));

        // try to encode then to decode and compare result
        byte[] encodedTimestampedData = encoder.encodeTimestampedData(timestampedData, format, null, resourcePath,
                model);

        List<TimestampedLwM2mNode> decodeTimestampedData = decoder.decodeTimestampedData(encodedTimestampedData, format,
                null, resourcePath, model);

        assertEquals(timestampedData, decodeTimestampedData);
    }

}
