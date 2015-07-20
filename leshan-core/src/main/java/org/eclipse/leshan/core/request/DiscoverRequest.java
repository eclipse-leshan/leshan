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
 *******************************************************************************/
package org.eclipse.leshan.core.request;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.response.DiscoverResponse;

public class DiscoverRequest extends AbstractDownlinkRequest<DiscoverResponse> {

    /**
     * Creates a request for discovering the resources implemented by a client for a particular object type.
     *
     * @param objectId the object type
     */
    public DiscoverRequest(int objectId) {
        this(new LwM2mPath(objectId));
    }

    /**
     * Creates a request for discovering the resources implemented by a client for a particular object instance.
     *
     * @param objectId the object type
     * @param objectInstanceId the object instance
     */
    public DiscoverRequest(int objectId, int objectInstanceId) {
        this(new LwM2mPath(objectId, objectInstanceId));
    }

    /**
     * Creates a request for discovering the attributes of a particular resource implemented by a client.
     *
     * @param objectId the object type
     * @param objectInstanceId the object instance
     * @param resourceId the resource
     */
    public DiscoverRequest(int objectId, int objectInstanceId, int resourceId) {
        this(new LwM2mPath(objectId, objectInstanceId, resourceId));
    }

    /**
     * Create a request for discovering the attributes of a particular object/instance/resource targeted by a specific
     * path.
     *
     * @param target the target path
     */
    public DiscoverRequest(String target) {
        super(new LwM2mPath(target));
    }

    private DiscoverRequest(LwM2mPath target) {
        super(target);
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public final String toString() {
        return String.format("DiscoverRequest [%s]", getPath());
    }
}
