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
import org.eclipse.leshan.core.node.LwM2mPath;

/**
 * An {@link ObserveRelationFilter} which select {@link ObserveRelation} based on resource URI.
 */
public class ResourceObserveFilter implements ObserveRelationFilter {

    protected final LwM2mPath[] paths;

    public ResourceObserveFilter(LwM2mPath... paths) {
        this.paths = paths;
    }

    @Override
    public boolean accept(ObserveRelation relation) {
        String relationURI = "/" + relation.getExchange().getRequest().getOptions().getUriPathString();
        LwM2mPath relationPath = new LwM2mPath(relationURI);
        for (LwM2mPath path : paths) {
            if (path.startWith(relationPath)) {
                return true;
            }
        }
        return false;
    }

}
