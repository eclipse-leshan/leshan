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

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonDeserializer;

/**
 * Unit test for {@link JsonDeserializer}
 */
public class JsonDeserializerTest {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void deserialize_device_object() throws LwM2mJsonException {

        StringBuilder b = new StringBuilder();
        b.append("{\"e\":[");
        b.append("{\"n\":\"0\",\"sv\":\"Open Mobile Alliance\"},");
        b.append("{\"n\":\"1\",\"sv\":\"Lightweight M2M Client\"},");
        b.append("{\"n\":\"2\",\"sv\":\"345000123\"},");
        b.append("{\"n\":\"3\",\"sv\":\"1.0\"},");
        b.append("{\"n\":\"6/0\",\"v\":1},");
        b.append("{\"n\":\"6/1\",\"v\":5},");
        b.append("{\"n\":\"7/0\",\"v\":3800},");
        b.append("{\"n\":\"7/1\",\"v\":5000},");
        b.append("{\"n\":\"8/0\",\"v\":125},");
        b.append("{\"n\":\"8/1\",\"v\":900},");
        b.append("{\"n\":\"9\",\"v\":100},");
        b.append("{\"n\":\"10\",\"v\":15},");
        b.append("{\"n\":\"11/0\",\"v\":0},");
        b.append("{\"n\":\"13\",\"v\":1367491215},");
        b.append("{\"n\":\"14\",\"sv\":\"+02:00\"},");
        b.append("{\"n\":\"15\",\"sv\":\"U\"}]}");

        String dataString = b.toString();
        log.debug(dataString.trim());
        JsonRootObject element = LwM2mJson.fromJsonLwM2m(dataString);
        log.debug(element.toString());
        String outString = LwM2mJson.toJsonLwM2m(element);
        Assert.assertTrue(dataString.trim().equals(outString));
    }

    @Test
    public void deserialize_temperature_resource() throws LwM2mJsonException {
        // Resource containing multiple historical representations
        // Currently Leshan does not handle the is case???
        StringBuilder b = new StringBuilder();
        b.append("{\"e\":[");
        b.append("{\"n\":\"1/2\",\"v\":22.4,\"t\":-5},");
        b.append("{\"n\":\"1/2\",\"v\":22.9,\"t\":-30},");
        b.append("{\"n\":\"1/2\",\"v\":24.1,\"t\":-50}],");
        b.append("\"bt\":25462634}");

        String dataString = b.toString();

        JsonRootObject element = LwM2mJson.fromJsonLwM2m(dataString);
        log.debug(element.toString());
        String outString = LwM2mJson.toJsonLwM2m(element);
        Assert.assertTrue(dataString.trim().equals(outString));
    }

    @Test
    public void deserialize_baseNameSpecified_object() throws LwM2mJsonException {
        // Not sure yet how Leshan will handle the object lin case
        // As it said in the Specs sec. 6.3.4 JSON
        // Table 20 Value as a JSON string if the Resource data type is Objlnk
        // Format according to Appendix C (e.g “10:03”)

        StringBuilder b = new StringBuilder();
        b.append("{\"bn\":\"/\",");
        b.append("\"e\":[");
        b.append("{\"n\":\"A/0/0/0\",\"ov\":\"B:0\"},");
        b.append("{\"n\":\"A/0/0/1\",\"ov\":\"B:1\"},");
        b.append("{\"n\":\"A/0/1\",\"sv\":\"8613800755500\"},");
        b.append("{\"n\":\"B/0/0\",\"sv\":\"myService1\"},");
        b.append("{\"n\":\"B/0/1\",\"sv\":\"Internet.15.234\"},");
        b.append("{\"n\":\"B/0/2\",\"ov\":\"C:0\"},");
        b.append("{\"n\":\"B/1/0\",\"sv\":\"myService2\"},");
        b.append("{\"n\":\"B/1/1\",\"sv\":\"Internet.15.235\"},");
        b.append("{\"n\":\"B/1/2\",\"ov\":\"FFFF:FFFF\"},");
        b.append("{\"n\":\"C/0/0\",\"sv\":\"85.76.76.84\"},");
        b.append("{\"n\":\"C/0/1\",\"sv\":\"85.76.255.255\"}]}");

        String dataString = b.toString();

        log.debug(dataString.trim());
        JsonRootObject element = LwM2mJson.fromJsonLwM2m(dataString);
        log.debug(element.toString());
        String outString = LwM2mJson.toJsonLwM2m(element);
        Assert.assertTrue(dataString.trim().equals(outString));
    }

}
