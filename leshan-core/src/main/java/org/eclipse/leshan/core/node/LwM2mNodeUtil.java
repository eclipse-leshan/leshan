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

import java.util.Collection;
import java.util.Iterator;

/**
 * Utility class about {@link LwM2mNode} and {@link LwM2mPath}.
 */
public class LwM2mNodeUtil {

    public static void validateNotNull(Object value, String message, Object... args) {
        if (value == null) {
            throw new LwM2mNodeException(message, args);
        }
    }

    public static void allElementsOfType(Collection<?> collection, Class<?> clazz) {
        int i = 0;
        for (Iterator<?> it = collection.iterator(); it.hasNext(); i++) {
            if (clazz.isInstance(it.next()) == false) {
                throw new LwM2mNodeException("The validated collection contains an element not of type "
                        + clazz.getName() + " at index: " + i);
            }
        }
    }

    public static void noNullElements(Collection<?> collection) {
        validateNotNull(collection, "collection MUST NOT be null");
        int i = 0;
        for (Iterator<?> it = collection.iterator(); it.hasNext(); i++) {
            if (it.next() == null) {
                throw new LwM2mNodeException("The validated collection contains null element at index: " + i);
            }
        }
    }

    public static boolean isUnsignedInt(Integer id) {
        return id != null && 0 <= id && id <= 65535;
    }

    public static boolean isValidObjectId(Integer id) {
        return isUnsignedInt(id);
    }

    public static void validateObjectId(Integer id) {
        if (!isValidObjectId(id)) {
            throw new LwM2mNodeException("Invalid object id %d, It MUST be an unsigned int.", id);
        }
    }

    public static boolean isValidObjectInstanceId(Integer id) {
        // MAX_ID 65535 is a reserved value and MUST NOT be used for identifying an Object Instance.
        return id != null && 0 <= id && id <= 65534;
    }

    public static void validateObjectInstanceId(Integer id) {
        if (!isValidObjectInstanceId(id)) {
            throw new LwM2mNodeException(
                    "Invalid object instance id %d, It MUST be an unsigned int. (65535 is reserved)", id);
        }
    }

    public static boolean isValidResourceId(Integer id) {
        return isUnsignedInt(id);
    }

    public static void validateResourceId(Integer id) {
        if (!isValidResourceId(id)) {
            throw new LwM2mNodeException("Invalid resource id %d, It MUST be an unsigned int.", id);
        }
    }

    public static boolean isValidResourceInstanceId(Integer id) {
        return isUnsignedInt(id);
    }

    public static void validateResourceInstanceId(Integer id) {
        if (!isValidResourceInstanceId(id)) {
            throw new LwM2mNodeException("Invalid resource instance id %d, It MUST be an unsigned int.", id);
        }
    }

    public static void validatePath(LwM2mPath path) {
        if (path.isObject()) {
            LwM2mNodeUtil.validateObjectId(path.getObjectId());
        } else if (path.isObjectInstance()) {
            LwM2mNodeUtil.validateObjectId(path.getObjectId());
            LwM2mNodeUtil.validateObjectInstanceId(path.getObjectInstanceId());
        } else if (path.isResource()) {
            LwM2mNodeUtil.validateObjectId(path.getObjectId());
            LwM2mNodeUtil.validateObjectInstanceId(path.getObjectInstanceId());
            LwM2mNodeUtil.validateResourceId(path.getResourceId());
        } else if (path.isResourceInstance()) {
            LwM2mNodeUtil.validateObjectId(path.getObjectId());
            LwM2mNodeUtil.validateObjectInstanceId(path.getObjectInstanceId());
            LwM2mNodeUtil.validateResourceId(path.getResourceId());
            LwM2mNodeUtil.validateResourceInstanceId(path.getResourceInstanceId());
        } else if (!path.isRoot()) {
            throw new LwM2mNodeException("Invalid LWM2M path (%d,%d,%d,%d)", path.getObjectId(),
                    path.getObjectInstanceId(), path.getResourceId(), path.getResourceInstanceId());
        }
    }

    public static void validateIncompletePath(LwM2mPath path) {
        if (path.isObjectInstance()) {
            LwM2mNodeUtil.validateObjectId(path.getObjectId());
            LwM2mNodeUtil.validateUndefinedObjecInstanceId(path.getObjectInstanceId());
        } else if (path.isResource()) {
            LwM2mNodeUtil.validateObjectId(path.getObjectId());
            LwM2mNodeUtil.validateUndefinedObjecInstanceId(path.getObjectInstanceId());
            LwM2mNodeUtil.validateResourceId(path.getResourceId());
        } else if (path.isResourceInstance()) {
            LwM2mNodeUtil.validateObjectId(path.getObjectId());
            LwM2mNodeUtil.validateUndefinedObjecInstanceId(path.getObjectInstanceId());
            LwM2mNodeUtil.validateResourceId(path.getResourceId());
            LwM2mNodeUtil.validateResourceInstanceId(path.getResourceInstanceId());
        } else if (!path.isRoot()) {
            throw new LwM2mNodeException("Invalid LWM2M path (%d,%d,%d,%d)", path.getObjectId(),
                    path.getObjectInstanceId(), path.getResourceId(), path.getResourceInstanceId());
        }
    }

    public static void validateUndefinedObjecInstanceId(int id) {
        if (id != LwM2mObjectInstance.UNDEFINED) {
            throw new LwM2mNodeException("Instance id should be undefined");
        }
    }
}
