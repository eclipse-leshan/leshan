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

import org.eclipse.leshan.core.node.InvalidLwM2mPathException;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.LwM2mResponse;

/**
 * A base class for concrete LWM2M Downlink request types.
 *
 * Provides generic support for specifying the target client and the resource path.
 */
public abstract class AbstractSimpleDownlinkRequest<T extends LwM2mResponse> extends AbstractLwM2mRequest<T>
        implements SimpleDownlinkRequest<T> {

    private final LwM2mPath path;

    protected AbstractSimpleDownlinkRequest(LwM2mPath path, Object coapRequest) {
        super(coapRequest);
        if (path == null)
            throw new InvalidRequestException("path is mandatory");

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
        AbstractSimpleDownlinkRequest<?> other = (AbstractSimpleDownlinkRequest<?>) obj;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        return true;
    }

    protected static LwM2mPath newPath(Integer objectId) {
        try {
            return new LwM2mPath(objectId);
        } catch (InvalidLwM2mPathException e) {
            throw new InvalidRequestException();
        }
    }

    protected static LwM2mPath newPath(Integer objectId, Integer objectInstanceId) {
        try {
            return new LwM2mPath(objectId, objectInstanceId);
        } catch (InvalidLwM2mPathException e) {
            throw new InvalidRequestException();
        }
    }

    protected static LwM2mPath newPath(Integer objectId, Integer objectInstanceId, Integer resourceId) {
        try {
            return new LwM2mPath(objectId, objectInstanceId, resourceId);
        } catch (InvalidLwM2mPathException e) {
            throw new InvalidRequestException();
        }
    }

    protected static LwM2mPath newPath(Integer objectId, Integer objectInstanceId, Integer resourceId,
            Integer resourceInstancId) {
        try {
            return new LwM2mPath(objectId, objectInstanceId, resourceId, resourceInstancId);
        } catch (InvalidLwM2mPathException e) {
            throw new InvalidRequestException();
        }
    }

    protected static LwM2mPath newPath(String path) {
        try {
            return new LwM2mPath(path);
        } catch (InvalidLwM2mPathException e) {
            throw new InvalidRequestException();
        }
    }

}