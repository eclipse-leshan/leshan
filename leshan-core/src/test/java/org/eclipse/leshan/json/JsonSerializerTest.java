/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *     Gemalto M2M GmbH
 *******************************************************************************/
package org.eclipse.leshan.json;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonSerializerTest {

    private Logger LOG = LoggerFactory.getLogger(getClass());

    @Test
    public void serialize_device_object() throws LwM2mJsonException {

        ArrayList<JsonArrayEntry> elements = new ArrayList<JsonArrayEntry>();
        JsonArrayEntry elt1 = new JsonArrayEntry();
        elt1.setName("0");
        elt1.setStringValue("Open Mobile Alliance");
        elements.add(elt1);
        JsonArrayEntry elt2 = new JsonArrayEntry();
        elt2.setName("1");
        elt2.setStringValue("Lightweight M2M Client");
        elements.add(elt2);
        JsonArrayEntry elt3 = new JsonArrayEntry();
        elt3.setName("2");
        elt3.setStringValue("345000123");
        elements.add(elt3);
        JsonArrayEntry elt4 = new JsonArrayEntry();
        elt4.setName("6/0");
        elt4.setFloatValue(1);
        elements.add(elt4);
        JsonArrayEntry elt5 = new JsonArrayEntry();
        elt5.setName("6/1");
        elt5.setFloatValue(5);
        elements.add(elt5);
        JsonArrayEntry elt6 = new JsonArrayEntry();
        elt6.setName("7/0");
        elt6.setFloatValue(3800);
        elements.add(elt6);
        JsonArrayEntry elt7 = new JsonArrayEntry();
        elt7.setName("7/1");
        elt7.setFloatValue(5000);
        elements.add(elt7);
        JsonArrayEntry elt8 = new JsonArrayEntry();
        elt8.setName("8/0");
        elt8.setFloatValue(125);
        elements.add(elt8);
        JsonArrayEntry elt9 = new JsonArrayEntry();
        elt9.setName("8/1");
        elt9.setFloatValue(900);
        elements.add(elt9);
        JsonArrayEntry elt10 = new JsonArrayEntry();
        elt10.setName("9");
        elt10.setFloatValue(100);
        elements.add(elt10);
        JsonArrayEntry elt11 = new JsonArrayEntry();
        elt11.setName("10");
        elt11.setFloatValue(15);
        elements.add(elt11);
        JsonArrayEntry elt12 = new JsonArrayEntry();
        elt12.setName("11/0");
        elt12.setFloatValue(0);
        elements.add(elt12);
        JsonArrayEntry elt13 = new JsonArrayEntry();
        elt13.setName("13");
        elt13.setFloatValue(1367491215);
        elements.add(elt13);
        JsonArrayEntry elt14 = new JsonArrayEntry();
        elt14.setName("14");
        elt14.setStringValue("+02:00");
        elements.add(elt14);
        JsonArrayEntry elt15 = new JsonArrayEntry();
        elt15.setName("15");
        elt15.setStringValue("U");
        elements.add(elt15);

        JsonRootObject element = new JsonRootObject(elements);
        String json = LwM2mJson.toJsonLwM2m(element);
        LOG.debug(" JSON String: " + json);

        JsonRootObject elementFromJson = LwM2mJson.fromJsonLwM2m(json);
        String backAgainToJson = LwM2mJson.toJsonLwM2m(elementFromJson);
        LOG.debug(" Back again to JSON String: " + backAgainToJson);

        Assert.assertTrue(json.equals(backAgainToJson));

    }

}
