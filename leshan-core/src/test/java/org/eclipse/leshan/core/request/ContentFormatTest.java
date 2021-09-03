/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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

import static org.eclipse.leshan.core.request.ContentFormat.*;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.junit.Test;

public class ContentFormatTest {

    @Test
    public void get_optional_content_format_for_v1_0() throws CodecException {

        List<ContentFormat> optionalContentFormat = ContentFormat
                .getOptionalContentFormatForClient(Arrays.asList(TLV, JSON, TEXT, OPAQUE), LwM2mVersion.V1_0);

        assertEquals(Arrays.asList(JSON, TEXT, OPAQUE), optionalContentFormat);
    }

    @Test
    public void get_optional_content_format_for_v1_1() throws CodecException {
        List<ContentFormat> optionalContentFormat = ContentFormat.getOptionalContentFormatForClient(
                Arrays.asList(TLV, JSON, SENML_JSON, SENML_CBOR, TEXT, OPAQUE, CBOR, LINK), LwM2mVersion.V1_1);

        assertEquals(Arrays.asList(TLV, JSON, SENML_JSON, SENML_CBOR, CBOR), optionalContentFormat);

    }
}
