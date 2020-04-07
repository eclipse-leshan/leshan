/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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

import org.eclipse.leshan.core.util.Validate;

/**
 * The object link is used to refer an Instance of a given Object. An Object link value is composed of two concatenated
 * 16-bits unsigned integers following the Network Byte Order convention. The Most Significant Halfword is an ObjectID,
 * the Least Significant Hafword is an ObjectInstance ID. An Object Link referencing no Object Instance will contain the
 * concatenation of 2 MAX-ID values (null link)
 * 
 * MAX-ID = 65535.
 */
public class ObjectLink {
    private final static int MAXID = 65535;

    private final int objectId;
    private final int objectInstanceId;

    /**
     * Create a null link. An Object Link referencing no object instance
     */
    public ObjectLink() {
        this(MAXID, MAXID);
    }

    /**
     * Create a Object Link referencing an object instance with the objectId/objectInstanceId path.
     */
    public ObjectLink(int objectId, int objectInstanceId) {
        // validate range
        Validate.isTrue(0 <= objectId && objectId <= MAXID);
        Validate.isTrue(0 <= objectInstanceId && objectInstanceId <= MAXID);

        // validate null link
        Validate.isTrue(
                (objectId != MAXID && objectInstanceId != MAXID) || objectId == MAXID && objectInstanceId == MAXID);

        this.objectId = objectId;
        this.objectInstanceId = objectInstanceId;
    }

    /**
     * Create a Object Link referencing an object instance with the given path.
     */
    public static ObjectLink fromPath(String path) {
        LwM2mPath lwM2mPath = new LwM2mPath(path);
        if (lwM2mPath.isRoot()) {
            return new ObjectLink(); // create null link
        } else if (lwM2mPath.isObjectInstance()) {
            return new ObjectLink(lwM2mPath.getObjectId(), lwM2mPath.getObjectInstanceId());
        } else {
            throw new IllegalArgumentException("Invalid path: ObjectLink should reference an object instance");
        }
    }

    public int getObjectId() {
        return objectId;
    }

    public int getObjectInstanceId() {
        return objectInstanceId;
    }

    public boolean isNullLink() {
        return objectId == MAXID && objectInstanceId == MAXID;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + objectId;
        result = prime * result + objectInstanceId;
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
        ObjectLink other = (ObjectLink) obj;
        if (objectId != other.objectId)
            return false;
        if (objectInstanceId != other.objectInstanceId)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("/%d/%d", objectId, objectInstanceId);
    }
}
