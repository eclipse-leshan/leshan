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
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.util.Validate;

/**
 * A Lightweight M2M request for deleting an Object Instance within the LWM2M Client.
 */
public class DeleteRequest extends AbstractDownlinkRequest<DeleteResponse> {

    /**
     * Creates a request for deleting a particular object instance implemented by a client.
     *
     * @param objectId the object type
     * @param objectInstanceId the object instance
     */
    public DeleteRequest(final int objectId, final int objectInstanceId) {
        this(new LwM2mPath(objectId, objectInstanceId));
    }

    /**
     * Creates a request for deleting a particular object instance implemented by a client.
     *
     * @param path the path of the instance to delete
     * @throw IllegalArgumentException if the path is not valid
     */
    public DeleteRequest(final String path) {
        super(new LwM2mPath(path));
    }

    private DeleteRequest(final LwM2mPath target) {
        super(target);
        Validate.isTrue(target.isObjectInstance(), "Only object instance can be delete.");
    }

    @Override
    public void accept(final DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public final String toString() {
        return String.format("DeleteRequest [%s]", getPath());
    }
}
