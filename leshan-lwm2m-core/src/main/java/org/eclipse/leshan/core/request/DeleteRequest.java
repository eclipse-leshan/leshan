/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.core.request;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.DeleteResponse;

/**
 * A Lightweight M2M request for deleting an Object Instance within the LWM2M Client.
 */
public class DeleteRequest extends AbstractSimpleDownlinkRequest<DeleteResponse>
        implements DownlinkDeviceManagementRequest<DeleteResponse> {

    /**
     * Creates a request for deleting a particular object instance implemented by a client.
     *
     * @param objectId the object type
     * @param objectInstanceId the object instance
     */
    public DeleteRequest(int objectId, int objectInstanceId) {
        this(newPath(objectId, objectInstanceId), null);
    }

    /**
     * Creates a request for deleting a particular object instance implemented by a client.
     *
     * @param path the path of the instance to delete
     * @exception InvalidRequestException if the path is not valid.
     */
    public DeleteRequest(String path) throws InvalidRequestException {
        this(newPath(path), null);
    }

    /**
     * Creates a request for deleting a particular object instance implemented by a client.
     *
     * @param path the path of the instance to delete
     * @param coapRequest the underlying request
     *
     * @exception InvalidRequestException if the path is not valid.
     */
    public DeleteRequest(String path, Object coapRequest) throws InvalidRequestException {
        this(newPath(path), coapRequest);
    }

    /**
     * Creates a request for deleting a particular object instance implemented by a client.
     *
     * @param path the path of the instance to delete
     * @exception InvalidRequestException if the path is not valid.
     */
    public DeleteRequest(LwM2mPath path) {
        this(path, null);
    }

    private DeleteRequest(LwM2mPath target, Object coapRequest) {
        super(target, coapRequest);
        if (target.isRoot())
            throw new InvalidRequestException("Delete request cannot target root path");

        if (!target.isObjectInstance())
            throw new InvalidRequestException("Invalid path %s : Only object instances can be delete", target);
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(DownlinkDeviceManagementRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public final String toString() {
        return String.format("DeleteRequest [%s]", getPath());
    }

    public boolean canEqual(Object o) {
        return (o instanceof DeleteRequest);
    }
}
