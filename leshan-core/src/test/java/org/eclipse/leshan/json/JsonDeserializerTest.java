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
       
       	String dataString = "{"
       	        + "\"e\":["+"{"+"\"n\":\"0\","+"\"sv\":\"Open Mobile Alliance\"},"
       	        + "{"+"\"n\":\"1\","+"\"sv\":\"Lightweight M2M Client\"}," 
       	        + "{"+"\"n\":\"2\","+"\"sv\":\"345000123\"}," 
       	        + "{"+"\"n\":\"3\","+"\"sv\":\"1.0\"}," 
       	        + "{"+"\"n\":\"6/0\","+"\"v\":1"+"}," 
       	        + "{"+"\"n\":\"6/1\","+"\"v\":5"+"}," 
       	        + "{"+"\"n\":\"7/0\","+"\"v\":3800"+"}," 
       	        + "{"+"\"n\":\"7/1\","+"\"v\":5000"+"}," 
       	        + "{"+"\"n\":\"8/0\","+"\"v\":125"+"}," 
       	        + "{"+"\"n\":\"8/1\","+"\"v\":900"+"}," 
       	        + "{"+"\"n\":\"9\","+"\"v\":100"+"}," 
       	        + "{"+"\"n\":\"10\","+"\"v\":15"+"}," 
       	        + "{"+"\"n\":\"11/0\","+"\"v\":0"+"}," 
       	        + "{"+"\"n\":\"13\","+"\"v\":1367491215"+"}," 
       	        + "{"+"\"n\":\"14\","+"\"sv\":\"+02:00\""+"}," 
       	        + "{"+"\"n\":\"15\","+"\"sv\":\"U\"}"+ "]"+"}"; 
       	
       	log.debug(dataString.trim());
        LwM2mJsonObject element = LwM2mJson.fromJsonLwM2m(dataString);
        log.debug(element.toString());
        String outString = LwM2mJson.toJsonLwM2m(element);
        Assert.assertTrue(dataString.trim().equals(outString));
    }

    @Test
    public void deserialize_temperature_resource() throws LwM2mJsonException {
       // Resource containing multiple historical representations
    	// Currently Leshan does not handle the is case???
       	String dataString = "{"
       	        + "\"e\":["+"{"+"\"n\":\"1/2\","+"\"v\":22.4"+","+"\"t\":-5"+"},"
       	        + "{"+"\"n\":\"1/2\","+"\"v\":22.9"+","+"\"t\":-30"+"}," 
       	        + "{"+"\"n\":\"1/2\","+"\"v\":24.1"+","+"\"t\":-50"+"}"+ "]"
       	        +","+"\"bt\":25462634"+"}"; 
       	
       	LwM2mJsonObject element = LwM2mJson.fromJsonLwM2m(dataString);
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
       	String dataString = "{"
       	        + "\"bn\":\"/\","
       	        + "\"e\":["+"{"+"\"n\":\"A/0/0/0\","+"\"ov\":\"B:0\"},"
       	        + "{"+"\"n\":\"A/0/0/1\","+"\"ov\":\"B:1\""+"}," 
       	        + "{"+"\"n\":\"A/0/1\","+"\"sv\":\"8613800755500\"}," 
       	        + "{"+"\"n\":\"A/0/2\","+"\"v\":1"+"}," 
       	        + "{"+"\"n\":\"B/0/0\","+"\"sv\":\"myService1\"}," 
       	        + "{"+"\"n\":\"B/0/1\","+"\"sv\":\"Internet.15.234\"}," 
       	        + "{"+"\"n\":\"B/0/2\","+"\"ov\":\"C:0\"},"  
       	        + "{"+"\"n\":\"B/1/0\","+"\"sv\":\"myService2\"}," 
       	        + "{"+"\"n\":\"B/1/1\","+"\"sv\":\"Internet.15.235\"}," 
       	        + "{"+"\"n\":\"B/1/2\","+"\"ov\":\"FFFF:FFFF\"}," 
       	        + "{"+"\"n\":\"C/0/0\","+"\"sv\":\"85.76.76.84\"},"  
       	        + "{"+"\"n\":\"C/0/1\","+"\"sv\":\"85.76.255.255\"}"+ "]"+"}"; 
      
       	log.debug(dataString.trim());
        LwM2mJsonObject element = LwM2mJson.fromJsonLwM2m(dataString);
        log.debug(element.toString());
        String outString = LwM2mJson.toJsonLwM2m(element);
        Assert.assertTrue(dataString.trim().equals(outString));
    }

}
