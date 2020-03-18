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
import org.eclipse.leshan.core.response.LwM2mResponse;

/**
 * A base class for concrete LWM2M Downlink request types.
 *
 * Provides generic support for specifying the target client and the resource path.
 */
public abstract class AbstractDownlinkRequest<T extends LwM2mResponse> implements DownlinkRequest<T> {

    private final LwM2mPath path;

    protected AbstractDownlinkRequest(LwM2mPath path) {
        if (path == null)
            throw new InvalidRequestException("path is mandatory");

        if (path.isResourceInstance())
            throw new InvalidRequestException("downlink request cannot target resource instance path: %s ", path);

        this.path = path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LwM2mPath getPath() {
        return this.path;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractDownlinkRequest<?> other = (AbstractDownlinkRequest<?>) obj;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        return true;
    }

    protected static LwM2mPath newPath(String path) {
        try {
            return new LwM2mPath(path);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException();
        }
    }

}