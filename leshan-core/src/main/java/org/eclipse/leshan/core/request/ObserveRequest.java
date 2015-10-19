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
import org.eclipse.leshan.core.response.ObserveResponse;

/**
 * A Lightweight M2M request for observing changes of a specific Resource, Resources within an Object Instance or for
 * all the Object Instances of an Object within the LWM2M Client.
 */
public class ObserveRequest extends AbstractDownlinkRequest<ObserveResponse> {

    public ObserveRequest(String target) {
        super(new LwM2mPath(target));
    }

    /**
     * Creates a request for observing future changes of all instances of a particular object of a client.
     * 
     * @param objectId the object ID of the resource
     */
    public ObserveRequest(int objectId) {
        super(new LwM2mPath(objectId));
    }

    /**
     * Creates a request for observing future changes of a particular object instance of a client.
     * 
     * @param objectId the object ID of the resource
     * @param objectInstanceId the object instance ID
     */
    public ObserveRequest(int objectId, int objectInstanceId) {
        super(new LwM2mPath(objectId, objectInstanceId));
    }

    /**
     * Creates a request for observing future changes of a specific resource of a client.
     * 
     * @param objectId the object ID of the resource
     * @param objectInstanceId the object instance ID
     * @param resourceId the (individual) resource's ID
     */
    public ObserveRequest(int objectId, int objectInstanceId, int resourceId) {
        super(new LwM2mPath(objectId, objectInstanceId, resourceId));
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public final String toString() {
        return String.format("ObserveRequest [%s]", getPath());
    }

}
