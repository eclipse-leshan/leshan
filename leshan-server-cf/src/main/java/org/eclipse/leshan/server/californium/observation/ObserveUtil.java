/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
 *     Michał Wadowski (Orange) - Add Observe-Composite feature.
 *     Michał Wadowski (Orange) - Add Cancel Composite-Observation feature.
 *******************************************************************************/
package org.eclipse.leshan.server.californium.observation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;

/**
 * Utility functions to help to handle observation in Leshan. Those helper functions are only needed if you're
 * implementing your own {@link CaliforniumRegistrationStore}.
 */
public class ObserveUtil {

    /* keys used to populate the request context */
    public static final String CTX_ENDPOINT = "leshan-endpoint";
    public static final String CTX_REGID = "leshan-regId";
    public static final String CTX_LWM2M_PATH = "leshan-path";

    /**
     * Create a LWM2M observation from a CoAP request.
     */
    public static SingleObservation createLwM2mObservation(Request request) {
        ObserveCommon observeCommon = new ObserveCommon(request);

        return new SingleObservation(
                request.getToken().getBytes(),
                observeCommon.regId,
                observeCommon.lwm2mPath.get(0),
                observeCommon.responseContentFormat,
                observeCommon.context
        );
    }

    public static CompositeObservation createLwM2mCompositeObservation(Request request) {
        ObserveCommon observeCommon = new ObserveCommon(request);

        return new CompositeObservation(
                request.getToken().getBytes(),
                observeCommon.regId,
                observeCommon.lwm2mPath,
                observeCommon.requestContentFormat,
                observeCommon.responseContentFormat,
                observeCommon.context
        );
    }

    private static class ObserveCommon {
        String regId;
        Map<String, String> context;
        List<LwM2mPath> lwm2mPath;
        ContentFormat requestContentFormat;
        ContentFormat responseContentFormat;

        public ObserveCommon(Request request) {
            if (request.getUserContext() == null) {
                throw new IllegalStateException("missing request context");
            }

            lwm2mPath = new ArrayList<>();
            context = new HashMap<>();

            for (Entry<String, String> ctx : request.getUserContext().entrySet()) {
                switch (ctx.getKey()) {
                    case CTX_REGID:
                        regId = ctx.getValue();
                        break;
                    case CTX_LWM2M_PATH:
                        for (String path : ctx.getValue().split("\n")) {
                            lwm2mPath.add(new LwM2mPath(path));
                        }
                        break;
                    case CTX_ENDPOINT:
                        break;
                    default:
                        context.put(ctx.getKey(), ctx.getValue());
                }
            }

            if (lwm2mPath.size() == 0) {
                throw new IllegalStateException("missing path in request context");
            }

            if (request.getOptions().hasContentFormat()) {
                requestContentFormat = ContentFormat.fromCode(request.getOptions().getContentFormat());
            }

            if (request.getOptions().hasAccept()) {
                responseContentFormat = ContentFormat.fromCode(request.getOptions().getAccept());
            }
        }
    }

    /**
     * Create a CoAP observe request context with specific keys needed for internal Leshan working.
     */
    public static Map<String, String> createCoapObserveRequestContext(String endpoint, String registrationId,
            ObserveRequest request) {
        Map<String, String> context = new HashMap<>();
        context.put(CTX_ENDPOINT, endpoint);
        context.put(CTX_REGID, registrationId);
        context.put(CTX_LWM2M_PATH, request.getPath().toString());
        for (Entry<String, String> ctx : request.getContext().entrySet()) {
            context.put(ctx.getKey(), ctx.getValue());
        }
        return context;
    }

    public static Map<String, String> createCoapObserveCompositeRequestContext(String endpoint, String registrationId,
            ObserveCompositeRequest request) {
        Map<String, String> context = new HashMap<>();
        context.put(CTX_ENDPOINT, endpoint);
        context.put(CTX_REGID, registrationId);

        StringBuilder sb = new StringBuilder();
        for (LwM2mPath path : request.getPaths()) {
            sb.append(path.toString());
            sb.append("\n");
        }

        context.put(CTX_LWM2M_PATH, sb.toString());
        for (Entry<String, String> ctx : request.getContext().entrySet()) {
            context.put(ctx.getKey(), ctx.getValue());
        }
        return context;
    }

    public static String extractRegistrationId(org.eclipse.californium.core.observe.Observation observation) {
        return observation.getRequest().getUserContext().get(CTX_REGID);
    }

    public static String extractEndpoint(org.eclipse.californium.core.observe.Observation observation) {
        return observation.getRequest().getUserContext().get(CTX_ENDPOINT);
    }

    /**
     * Validate the Californium observation. It is valid if it contains all necessary context for Leshan.
     */
    public static String validateCoapObservation(org.eclipse.californium.core.observe.Observation observation) {
        if (!observation.getRequest().getUserContext().containsKey(CTX_REGID))
            throw new IllegalStateException("missing registrationId info in the request context");
        if (!observation.getRequest().getUserContext().containsKey(CTX_LWM2M_PATH))
            throw new IllegalStateException("missing lwm2m path info in the request context");

        String endpoint = observation.getRequest().getUserContext().get(CTX_ENDPOINT);
        if (endpoint == null)
            throw new IllegalStateException("missing endpoint info in the request context");

        return endpoint;
    }
}
