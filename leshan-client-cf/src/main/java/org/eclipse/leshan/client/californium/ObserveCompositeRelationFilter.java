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
package org.eclipse.leshan.client.californium;

import java.util.Arrays;
import java.util.List;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.observe.ObserveRelationFilter;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.request.ContentFormat;

/**
 * An {@link ObserveRelationFilter} which select {@link ObserveRelation} based on one of resource URIs.
 */
class ObserveCompositeRelationFilter implements ObserveRelationFilter {

    private final LwM2mDecoder decoder;
    private final List<LwM2mPath> paths;

    /**
     * Instantiates {@link ObserveCompositeRelationFilter} basing on resource URIs.
     *
     * @param decoder {@link LwM2mDecoder} which will decode data in supported content format.
     * @param paths the list of {@link LwM2mPath}
     */
    public ObserveCompositeRelationFilter(LwM2mDecoder decoder, LwM2mPath... paths) {
        this.decoder = decoder;
        this.paths = Arrays.asList(paths);
    }

    @Override
    public boolean accept(ObserveRelation relation) {
        Request request = getRequest(relation);

        if (!isValidObserveCompositeRequest(request)) {
            return false;
        }

        List<LwM2mPath> observationPaths = getObserveRequestPaths(request);

        if (observationPaths != null) {
            for (LwM2mPath observePath : observationPaths) {
                for (LwM2mPath path : paths) {
                    if (path.startWith(observePath)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isValidObserveCompositeRequest(Request request) {
        if (!request.getCode().equals(CoAP.Code.FETCH)) {
            return false;
        }
        if (!request.getOptions().hasContentFormat()) {
            return false;
        }
        return true;
    }

    private Request getRequest(ObserveRelation relation) {
        return relation.getExchange().getRequest();
    }

    private List<LwM2mPath> getObserveRequestPaths(Request request) {
        ContentFormat contentFormat = ContentFormat.fromCode(request.getOptions().getContentFormat());

        try {
            return decoder.decodePaths(request.getPayload(), contentFormat);
        } catch (CodecException e) {
            return null;
        }
    }
}
