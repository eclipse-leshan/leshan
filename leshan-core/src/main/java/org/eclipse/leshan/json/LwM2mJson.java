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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class LwM2mJson {
	
	private static final Logger LOG = LoggerFactory.getLogger(LwM2mJson.class);
			
	private static final Gson gson = new GsonBuilder().create();
	
	
    /**
     * <ul> This <code>toJson</code> method accepts LwM2mJsonObject object and converts to JSON String.
	 * <li>
	 * </ul>
	 * 
     * @param lwM2mJsonElement
     * @return
     */
	public static String toJsonLwM2m(LwM2mJsonObject lwM2mJsonElement) {
		String json = gson.toJson(lwM2mJsonElement);
		return json;
	}
	
	/**
    * <ul> This <code>toJson</code> method accepts JSON String and converts to LwM2mJsonObject.
	* <li>
	* </ul>
	* 
    * @param jsonString
    * @return
    * @throws LwM2mJsonException
    */
	public static LwM2mJsonObject fromJsonLwM2m(String jsonString)
			throws LwM2mJsonException {
		try {
			LwM2mJsonObject element = gson.fromJson(jsonString,
					LwM2mJsonObject.class);
			return element;
		} catch (Exception e) {
			LOG.error(" Unable to deserialize JSON String to LwM2mJsonElement object: " +e.getMessage());
			throw new LwM2mJsonException(
					" Unable to deserialize JSON String to LwM2mJsonElement object ", e);
		}
	}
	
}
