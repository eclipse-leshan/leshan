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
import org.eclipse.leshan.core.response.ReadResponse;

/**
 * A Lightweight M2M request for retrieving the values of resources from a LWM2M Client.
 * 
 * The request can be used to retrieve the value(s) of one or all attributes of one particular or all instances of a
 * particular object type.
 */
public class ReadRequest extends AbstractDownlinkRequest<ReadResponse> {

    /**
     * Creates a request for reading all instances of a particular object from a client.
     * 
     * @param objectId the object ID of the resource
     */
    public ReadRequest(int objectId) {
        this(new LwM2mPath(objectId));
    }

    /**
     * Creates a request for reading a particular object instance from a client.
     * 
     * @param objectId the object ID of the resource
     * @param objectInstanceId the object instance ID
     */
    public ReadRequest(int objectId, int objectInstanceId) {
        this(new LwM2mPath(objectId, objectInstanceId));
    }

    /**
     * Creates a request for reading a specific resource from a client.
     * 
     * @param objectId the object ID of the resource
     * @param objectInstanceId the object instance ID
     * @param resourceId the (individual) resource's ID
     */
    public ReadRequest(int objectId, int objectInstanceId, int resourceId) {
        this(new LwM2mPath(objectId, objectInstanceId, resourceId));
    }

    /**
     * Create a request for reading an object/instance/resource targeted by a specific path.
     * 
     * @param target the target path
     */
    public ReadRequest(String target) {
        super(new LwM2mPath(target));
    }

    private ReadRequest(LwM2mPath target) {
        super(target);
    }

    @Override
    public final String toString() {
        return String.format("ReadRequest [%s]", getPath());
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);

    }
}
