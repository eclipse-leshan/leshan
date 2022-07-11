/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
package org.eclipse.leshan.server.demo.servlet.log;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.OptionNumberRegistry;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;

public class CoapMessage {

    public long timestamp;

    // true for received message and false for sent messages
    public boolean incoming;

    // Confirmable, Non-confirmable, Acknowledgment, Reset
    public String type;

    // Request method or Response code
    public String code;

    public int mId;

    public String token;

    public String options;

    public String payload;

    public CoapMessage(Request request, boolean incoming) {
        this(incoming, request.getType(), request.getMID(), request.getTokenString(), request.getOptions(),
                request.getPayload());
        this.code = request.getCode().toString();
    }

    public CoapMessage(Response request, boolean incoming) {
        this(incoming, request.getType(), request.getMID(), request.getTokenString(), request.getOptions(),
                request.getPayload());
        this.code = request.getCode().toString();
    }

    public CoapMessage(EmptyMessage request, boolean incoming) {
        this(incoming, request.getType(), request.getMID(), request.getTokenString(), request.getOptions(),
                request.getPayload());
    }

    private CoapMessage(boolean incoming, Type type, int mId, String token, OptionSet options, byte[] payload) {
        this.incoming = incoming;
        this.timestamp = System.currentTimeMillis();
        this.type = type.toString();
        this.mId = mId;
        this.token = token;

        if (options != null) {
            List<Option> opts = options.asSortedList();
            if (!opts.isEmpty()) {
                Map<String, List<String>> optMap = new HashMap<>();
                for (Option opt : opts) {
                    String strOption = OptionNumberRegistry.toString(opt.getNumber());
                    List<String> values = optMap.get(strOption);
                    if (values == null) {
                        values = new ArrayList<>();
                        optMap.put(strOption, values);
                    }
                    values.add(opt.toValueString());
                }

                StringBuilder builder = new StringBuilder();
                for (Entry<String, List<String>> e : optMap.entrySet()) {
                    if (builder.length() > 0) {
                        builder.append(" - ");
                    }
                    builder.append(e.getKey()).append(": ").append(StringUtils.join(e.getValue(), ", "));
                }
                this.options = builder.toString();

            }
        }
        if (payload != null && payload.length > 0) {
            String strPayload = new String(payload, StandardCharsets.UTF_8);
            if (StringUtils.isAsciiPrintable(strPayload)) {
                this.payload = strPayload;
            } else {
                this.payload = "Hex:" + Hex.encodeHexString(payload);
            }
        }
    }
}
