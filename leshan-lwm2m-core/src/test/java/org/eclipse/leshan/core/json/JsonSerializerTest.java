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
 *     Sierra Wireless - initial API and implementation
 *     Gemalto M2M GmbH
 *******************************************************************************/
package org.eclipse.leshan.core.json;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.eclipse.leshan.core.json.jackson.LwM2mJsonJacksonEncoderDecoder;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonSerializerTest {

    private static final Logger LOG = LoggerFactory.getLogger(JsonSerializerTest.class);

    private static final LwM2mJsonJacksonEncoderDecoder LWM2M_JSON_ENCODER_DECODER = new LwM2mJsonJacksonEncoderDecoder();

    @Test
    public void serialize_device_object() throws LwM2mJsonException {

        ArrayList<JsonArrayEntry> elements = new ArrayList<>();
        JsonArrayEntry elt1 = new JsonArrayEntry("0", null, null, null, "Open Mobile Alliance", null);
        elements.add(elt1);
        JsonArrayEntry elt2 = new JsonArrayEntry("1", null, null, null, "Lightweight M2M Client", null);
        elements.add(elt2);
        JsonArrayEntry elt3 = new JsonArrayEntry("2", null, null, null, "345000123", null);
        elements.add(elt3);
        JsonArrayEntry elt4 = new JsonArrayEntry("6/0", 1, null, null, null, null);
        elements.add(elt4);
        JsonArrayEntry elt5 = new JsonArrayEntry("6/1", 5, null, null, null, null);
        elements.add(elt5);
        JsonArrayEntry elt6 = new JsonArrayEntry("7/0", 3800, null, null, null, null);
        elements.add(elt6);
        JsonArrayEntry elt7 = new JsonArrayEntry("7/1", 5000, null, null, null, null);
        elements.add(elt7);
        JsonArrayEntry elt8 = new JsonArrayEntry("8/0", 125, null, null, null, null);
        elements.add(elt8);
        JsonArrayEntry elt9 = new JsonArrayEntry("8/1", 900, null, null, null, null);
        elements.add(elt9);
        JsonArrayEntry elt10 = new JsonArrayEntry("9", 100, null, null, null, null);
        elements.add(elt10);
        JsonArrayEntry elt11 = new JsonArrayEntry("10", 15, null, null, null, null);
        elements.add(elt11);
        JsonArrayEntry elt12 = new JsonArrayEntry("11/0", 0, null, null, null, null);
        elements.add(elt12);
        JsonArrayEntry elt13 = new JsonArrayEntry("13", 1367491215, null, null, null, null);
        elements.add(elt13);
        JsonArrayEntry elt14 = new JsonArrayEntry("14", null, null, null, "+02:00", null);
        elements.add(elt14);
        JsonArrayEntry elt15 = new JsonArrayEntry("15", null, null, null, "U", null);
        elements.add(elt15);

        JsonRootObject element = new JsonRootObject(null, elements, null);
        String json = LWM2M_JSON_ENCODER_DECODER.toJsonLwM2m(element);
        LOG.debug(" JSON String: " + json);

        JsonRootObject elementFromJson = LWM2M_JSON_ENCODER_DECODER.fromJsonLwM2m(json);
        String backAgainToJson = LWM2M_JSON_ENCODER_DECODER.toJsonLwM2m(elementFromJson);
        LOG.debug(" Back again to JSON String: " + backAgainToJson);

        assertTrue(json.equals(backAgainToJson));

    }

}
