/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
import org.eclipse.leshan.core.response.BootstrapDiscoverResponse;

/**
 * A Lightweight M2M request to let LWM2M Bootstrap server discovering which LwM2M Objects and Object Instances are
 * supported by a certain LwM2M Client.
 * 
 * @since 1.1
 */
public class BootstrapDiscoverRequest extends AbstractDownlinkRequest<BootstrapDiscoverResponse> {

    /**
     * Creates a request for discovering all objects and instances supported by the client.
     */
    public BootstrapDiscoverRequest() {
        this(LwM2mPath.ROOTPATH);
    }

    /**
     * Creates a request for discovering instance of a given object.
     *
     * @param objectId the object type
     */
    public BootstrapDiscoverRequest(int objectId) {
        this(new LwM2mPath(objectId));
    }

    /**
     * Create a request for discovering
     *
     * @param path the path of the LWM2M node to discover
     * @exception InvalidRequestException if the path is not valid.
     */
    public BootstrapDiscoverRequest(String path) throws InvalidRequestException {
        this(newPath(path));
    }

    private BootstrapDiscoverRequest(LwM2mPath target) {
        super(target);
        if (!target.isRoot() && !target.isObject()) {
            throw new InvalidRequestException("Invalid path %s : Discover request can only target root or object path",
                    target);
        }
    }

    @Override
    public final String toString() {
        return String.format("DiscoverRequest [%s]", getPath());
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }
}
