/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
import org.eclipse.leshan.core.response.BootstrapDeleteResponse;

/**
 * A LWM2M request for deleting object instances during the bootstrap phase.
 */
public class BootstrapDeleteRequest extends AbstractDownlinkRequest<BootstrapDeleteResponse> {

    /**
     * Creates a request for deleting all Instances of all Objects in the LwM2M Client (except LwM2M Bootstrap-Server
     * Account and the single Instance of the Mandatory Device Object (ID:3)).
     */
    public BootstrapDeleteRequest() {
        this(LwM2mPath.ROOTPATH);
    }

    /**
     * Creates a request for deleting all instances of a particular object implemented by a client. (except the LwM2M
     * Bootstrap-Server Account and the Instance of the Device Object)
     *
     * @param objectId the object Id
     */
    public BootstrapDeleteRequest(int objectId) {
        this(new LwM2mPath(objectId));
    }

    /**
     * Creates a request for deleting a particular object instance implemented by a client. (except the LwM2M
     * Bootstrap-Server Account and the Instance of the Device Object)
     *
     * @param objectId the object type
     * @param objectInstanceId the object instance
     */
    public BootstrapDeleteRequest(int objectId, int objectInstanceId) {
        this(new LwM2mPath(objectId, objectInstanceId));
    }

    /**
     * Creates a request for deleting object instances implemented by a client. (except the LwM2M Bootstrap-Server
     * Account and the Instance of the Device Object)
     *
     * @param path the path (could be a root path, an object path or an object instance path)
     * @exception InvalidRequestException if the path is not valid.
     */
    public BootstrapDeleteRequest(String path) throws InvalidRequestException {
        this(newPath(path));
    }

    private BootstrapDeleteRequest(LwM2mPath target) {
        super(target);
        if (target.isResource() || target.isResourceInstance())
            throw new InvalidRequestException("Invalid path %s : Only objects or object instances can be deleted",
                    target);
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public final String toString() {
        return String.format("BootstrapDeleteRequest [%s]", getPath());
    }

}
