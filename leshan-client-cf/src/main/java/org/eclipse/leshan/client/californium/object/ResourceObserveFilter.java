/*******************************************************************************
 * Copyright (c) 2016 Bosch Software Innovations GmbH and others.
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
 *    Achim Kraus (Bosch Software Innovations GmbH) - Initial creation
 ******************************************************************************/

package org.eclipse.leshan.client.californium.object;

import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.observe.ObserveRelationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link ObserveRelationFilter} which select {@link ObserveRelation} based on resource URI.
 */
public class ResourceObserveFilter implements ObserveRelationFilter {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceObserveFilter.class);

    protected final String notifyURI;

    public ResourceObserveFilter(String notifyURI) {
        this.notifyURI = notifyURI;
    }

    @Override
    public boolean accept(ObserveRelation relation) {
        String relationURI = relation.getExchange().getRequest().getOptions().getUriPathString();
        boolean result = relationURI.equals(notifyURI);
        if (LOG.isTraceEnabled()) {
            LOG.trace("observe " + relationURI + " / " + notifyURI + ": " + result);
        }
        return result;
    }

}
