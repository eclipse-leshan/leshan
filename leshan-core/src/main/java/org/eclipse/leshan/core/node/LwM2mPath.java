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
package org.eclipse.leshan.core.node;

import org.eclipse.leshan.util.Validate;

/**
 * A path pointing to a LwM2M node (object, object instance or resource).
 */
public class LwM2mPath {

    private final int objectId;
    private final Integer objectInstanceId;
    private final Integer resourceId;

    /**
     * Create a path to an object
     *
     * @param objectId the object identifier
     */
    public LwM2mPath(int objectId) {
        this.objectId = objectId;
        this.objectInstanceId = null;
        this.resourceId = null;
    }

    /**
     * Create a path to an object instance
     *
     * @param objectId the object identifier
     * @param objectInstanceId the instance
     */
    public LwM2mPath(int objectId, int objectInstanceId) {
        this.objectId = objectId;
        this.objectInstanceId = objectInstanceId;
        this.resourceId = null;
    }

    /**
     * Create a path to a resource of a given object instance
     *
     * @param objectId the object identifier
     * @param objectInstanceId
     * @param resourceId the resource identifier
     */
    public LwM2mPath(int objectId, int objectInstanceId, int resourceId) {
        this.objectId = objectId;
        this.objectInstanceId = objectInstanceId;
        this.resourceId = resourceId;
    }

    /**
     * Constructs a {@link LwM2mPath} from a string representation
     *
     * @param path the path (e.g. "/3/0/1" or "/3")
     */
    public LwM2mPath(String path) {
        Validate.notEmpty(path);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        String[] p = path.split("/");
        if (p.length < 1 || p.length > 3) {
            throw new IllegalArgumentException("Invalid length for path: " + path);
        }
        try {
            this.objectId = Integer.valueOf(p[0]);
            this.objectInstanceId = (p.length >= 2) ? Integer.valueOf(p[1]) : null;
            this.resourceId = (p.length == 3) ? Integer.valueOf(p[2]) : null;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid elements in path: " + path, e);
        }
    }

    /**
     * Returns the object ID in the path.
     *
     * @return the object ID
     */
    public int getObjectId() {
        return objectId;
    }

    /**
     * Returns the object instance ID in the path.
     *
     * @return the object instance ID. Can be <code>null</code> when this is an object path.
     */
    public Integer getObjectInstanceId() {
        return objectInstanceId;
    }

    /**
     * Returns the resource ID in the request path.
     *
     * @return the resource ID. Can be <code>null</code> when this is a object/object instance path.
     */
    public Integer getResourceId() {
        return resourceId;
    }

    /**
     * @return <code>true</code> if this is an Object path.
     */
    public boolean isObject() {
        return objectInstanceId == null && resourceId == null;
    }

    /**
     * @return <code>true</code> if this is an ObjectInstance path.
     */
    public boolean isObjectInstance() {
        return objectInstanceId != null && resourceId == null;
    }

    /**
     * @return <code>true</code> if this is a Resource path.
     */
    public boolean isResource() {
        return objectInstanceId != null && resourceId != null;
    }

    /**
     * The string representation of the path: /{Object ID}/{ObjectInstance ID}/{Resource ID}
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("/");
        b.append(getObjectId());
        if (getObjectInstanceId() != null) {
            b.append("/").append(getObjectInstanceId());
            if (getResourceId() != null) {
                b.append("/").append(getResourceId());
            }
        }
        return b.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + objectId;
        result = prime * result + ((objectInstanceId == null) ? 0 : objectInstanceId.hashCode());
        result = prime * result + ((resourceId == null) ? 0 : resourceId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LwM2mPath other = (LwM2mPath) obj;
        if (objectId != other.objectId) {
            return false;
        }
        if (objectInstanceId == null) {
            if (other.objectInstanceId != null) {
                return false;
            }
        } else if (!objectInstanceId.equals(other.objectInstanceId)) {
            return false;
        }
        if (resourceId == null) {
            if (other.resourceId != null) {
                return false;
            }
        } else if (!resourceId.equals(other.resourceId)) {
            return false;
        }
        return true;
    }

}