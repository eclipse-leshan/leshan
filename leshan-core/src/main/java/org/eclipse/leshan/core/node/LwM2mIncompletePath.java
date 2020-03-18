/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
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
package org.eclipse.leshan.core.node;

/**
 * A path pointing to a LwM2M node (root, object, object instance, resource or resource instance) but with missing
 * instance id part.
 * <p>
 * This is mainly used for CREATE request where object instance id could be omitted and so client choose it.
 */
public class LwM2mIncompletePath extends LwM2mPath {

    /**
     * Create a path to an object instance without object instance id
     *
     * @param objectId the object identifier
     */
    public LwM2mIncompletePath(int objectId) {
        super(objectId, LwM2mObjectInstance.UNDEFINED, null, null);
        validate();
    }

    /**
     * Create a path to a resource of a given object instance without object instance id
     *
     * @param objectId the object identifier
     * @param resourceId the resource identifier
     */
    public LwM2mIncompletePath(int objectId, int resourceId) {
        super(objectId, LwM2mObjectInstance.UNDEFINED, resourceId, null);
        validate();
    }

    /**
     * Create a path to a resource instance of a given resource without object instance id
     *
     * @param objectId the object identifier
     * @param resourceId the resource identifier
     * @param resourceInstanceId the resource instance identifier
     */
    public LwM2mIncompletePath(int objectId, int resourceId, int resourceInstanceId) {
        super(objectId, LwM2mObjectInstance.UNDEFINED, resourceId, resourceInstanceId);
        validate();
    }

    @Override
    public LwM2mPath append(String path) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public LwM2mPath append(int end) {
        if (isObjectInstance()) {
            return new LwM2mIncompletePath(getObjectId(), end);
        } else if (isResource()) {
            return new LwM2mIncompletePath(getObjectId(), getResourceId(), end);
        } else {
            throw new IllegalArgumentException(String.format(
                    "Unable to append Id(%d) to path %s. Resource instance level is the deeper one.", end, this));
        }
    }

    @Override
    protected void validate() {
        LwM2mNodeUtil.validateIncompletePath(this);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("/");
        if (getObjectId() != null) {
            b.append(getObjectId());
            if (getObjectInstanceId() != null) {
                b.append("/undefined");
                if (getResourceId() != null) {
                    b.append("/").append(getResourceId());
                    if (getResourceInstanceId() != null) {
                        b.append("/").append(getResourceInstanceId());
                    }
                }
            }
        }
        return b.toString();
    }
}
