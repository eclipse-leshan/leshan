/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
package org.eclipse.leshan.client.californium.object;

import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.observe.ObserveRelationFilter;
import org.eclipse.leshan.core.node.LwM2mPath;

public class InstanceRemovedFilter implements ObserveRelationFilter {

    protected final LwM2mPath[] objectInstancePaths;

    public InstanceRemovedFilter(int objectId, int[] instanceIds) {
        objectInstancePaths = new LwM2mPath[instanceIds.length];
        for (int i = 0; i < instanceIds.length; i++) {
            objectInstancePaths[i] = new LwM2mPath(objectId, instanceIds[i]);
        }
    }

    @Override
    public boolean accept(ObserveRelation relation) {
        String relationURI = "/" + relation.getExchange().getRequest().getOptions().getUriPathString();
        LwM2mPath relationPath = new LwM2mPath(relationURI);
        for (LwM2mPath objectInstancePath : objectInstancePaths) {
            if (relationPath.startWith(objectInstancePath)) {
                return true;
            }
        }
        return false;
    }
}
