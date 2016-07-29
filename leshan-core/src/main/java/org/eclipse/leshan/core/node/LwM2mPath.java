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
 * A path pointing to a LwM2M node (root, object, object instance, resource or resource instance).
 */
public class LwM2mPath {

    private final Integer objectId;
    private final Integer objectInstanceId;
    private final Integer resourceId;
    private final Integer resourceInstanceId;

    public final static LwM2mPath ROOTPATH = new LwM2mPath();

    private LwM2mPath() {
        this.objectId = null;
        this.objectInstanceId = null;
        this.resourceId = null;
        this.resourceInstanceId = null;
    }

    /**
     * Create a path to an object
     *
     * @param objectId the object identifier
     */
    public LwM2mPath(int objectId) {
        this.objectId = objectId;
        this.objectInstanceId = null;
        this.resourceId = null;
        this.resourceInstanceId = null;
    }

    /**
     * Create a path to an object instance
     *
     * @param objectId the object identifier
     * @param objectInstanceId the instance identifier
     */
    public LwM2mPath(int objectId, int objectInstanceId) {
        this.objectId = objectId;
        this.objectInstanceId = objectInstanceId;
        this.resourceId = null;
        this.resourceInstanceId = null;
    }

    /**
     * Create a path to a resource of a given object instance
     *
     * @param objectId the object identifier
     * @param objectInstanceId the instance identifier
     * @param resourceId the resource identifier
     */
    public LwM2mPath(int objectId, int objectInstanceId, int resourceId) {
        this.objectId = objectId;
        this.objectInstanceId = objectInstanceId;
        this.resourceId = resourceId;
        this.resourceInstanceId = null;
    }

    /**
     * Create a path to a resource instance of a given resource
     *
     * @param objectId the object identifier
     * @param objectInstanceId the instance identifier
     * @param resourceId the resource identifier
     * @param resourceInstanceId the resource instance identifier
     */
    public LwM2mPath(int objectId, int objectInstanceId, int resourceId, int resourceInstanceId) {
        this.objectId = objectId;
        this.objectInstanceId = objectInstanceId;
        this.resourceId = resourceId;
        this.resourceInstanceId = resourceInstanceId;
    }

    /**
     * Constructs a {@link LwM2mPath} from a string representation
     *
     * @param path the path (e.g. "/3/0/1" or "/3")
     */
    public LwM2mPath(String path) {
        Validate.notNull(path);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String[] p = path.split("/");
        if (0 > p.length || p.length > 4) {
            throw new IllegalArgumentException("Invalid length for path: " + path);
        }
        try {
            this.objectId = (p.length >= 1 && !p[0].isEmpty()) ? Integer.valueOf(p[0]) : null;
            this.objectInstanceId = (p.length >= 2) ? Integer.valueOf(p[1]) : null;
            this.resourceId = (p.length >= 3) ? Integer.valueOf(p[2]) : null;
            this.resourceInstanceId = (p.length == 4) ? Integer.valueOf(p[3]) : null;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid elements in path: " + path, e);
        }
    }

    /**
     * @param path the end of the new path
     * @return a new path which is the concatenation of this path and the given one in parameter.
     */
    public LwM2mPath append(String path) {
        LwM2mPath pathToAdd = new LwM2mPath(path);
        if (isRoot()) {
            return pathToAdd;
        } else {
            return new LwM2mPath(this.toString() + pathToAdd.toString());
        }
    }

    /**
     * Returns the object ID in the path.
     *
     * @return the object ID. Can be <code>null</code> when this is an root path.
     */
    public Integer getObjectId() {
        return objectId;
    }

    /**
     * Returns the object instance ID in the path.
     *
     * @return the object instance ID. Can be <code>null</code> when this is an root/object path.
     */
    public Integer getObjectInstanceId() {
        return objectInstanceId;
    }

    /**
     * Returns the resource ID in the request path.
     *
     * @return the resource ID. Can be <code>null</code> when this is a root/object/object instance path.
     */
    public Integer getResourceId() {
        return resourceId;
    }

    /**
     * Returns the resource instance ID in the request path.
     *
     * @return the resource instance ID. Can be <code>null</code> when this is a root/object/object instance/resource
     *         path.
     */
    public Integer getResourceInstanceId() {
        return resourceInstanceId;
    }

    /**
     * @return <code>true</code> if this is the root path ("/").
     */
    public boolean isRoot() {
        return objectId == null && objectInstanceId == null && resourceId == null && resourceInstanceId == null;
    }

    /**
     * @return <code>true</code> if this is an Object path.
     */
    public boolean isObject() {
        return objectId != null && objectInstanceId == null && resourceId == null && resourceInstanceId == null;
    }

    /**
     * @return <code>true</code> if this is an ObjectInstance path.
     */
    public boolean isObjectInstance() {
        return objectId != null && objectInstanceId != null && resourceId == null && resourceInstanceId == null;
    }

    /**
     * @return <code>true</code> if this is a Resource path.
     */
    public boolean isResource() {
        return objectId != null && objectInstanceId != null && resourceId != null && resourceInstanceId == null;
    }

    /**
     * @return <code>true</code> if this is a Resource instance path.
     */
    public boolean isResourceInstance() {
        return objectId != null && objectInstanceId != null && resourceId != null && resourceInstanceId != null;
    }

    /**
     * The string representation of the path: /{Object ID}/{ObjectInstance ID}/{Resource ID}/{ResourceInstance ID}
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("/");
        if (getObjectId() != null) {
            b.append(getObjectId());
            if (getObjectInstanceId() != null) {
                b.append("/").append(getObjectInstanceId());
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + objectId;
        result = prime * result + ((objectInstanceId == null) ? 0 : objectInstanceId.hashCode());
        result = prime * result + ((resourceId == null) ? 0 : resourceId.hashCode());
        result = prime * result + ((resourceInstanceId == null) ? 0 : resourceInstanceId.hashCode());
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
        if (objectId == null) {
            if (other.objectId != null) {
                return false;
            }
        } else if (!objectId.equals(other.objectId)) {
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
        if (resourceInstanceId == null) {
            if (other.resourceInstanceId != null) {
                return false;
            }
        } else if (!resourceInstanceId.equals(other.resourceInstanceId)) {
            return false;
        }
        return true;
    }

}