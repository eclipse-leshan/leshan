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
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.leshan.core.model.ResourceModel.Type;

/**
 * Utility class about {@link LwM2mNode} and {@link LwM2mPath}.
 */
public class LwM2mNodeUtil {

    public static final int MAX_OBJECT_ID = 65535;
    // MAX_ID 65535 is a reserved value and MUST NOT be used for identifying an Object Instance.
    public static final int MAX_OBJECT_INSTANCE_ID = 65534;
    public static final int MAX_RESOURCE_ID = 65535;
    public static final int MAX_RESOURCE_INSTANCE_ID = 65535;

    private LwM2mNodeUtil() {
    }

    public static void validateNotNull(Object value, String message, Object... args) throws LwM2mNodeException {
        if (value == null) {
            throw new LwM2mNodeException(message, args);
        }
    }

    public static void allElementsOfType(Collection<?> collection, Class<?> clazz) throws LwM2mNodeException {
        int i = 0;
        for (Iterator<?> it = collection.iterator(); it.hasNext(); i++) {
            if (!clazz.isInstance(it.next())) {
                throw new LwM2mNodeException("The validated collection contains an element not of type "
                        + clazz.getName() + " at index: " + i);
            }
        }
    }

    public static void noNullElements(Collection<?> collection) throws LwM2mNodeException {
        validateNotNull(collection, "collection MUST NOT be null");
        int i = 0;
        for (Iterator<?> it = collection.iterator(); it.hasNext(); i++) {
            if (it.next() == null) {
                throw new LwM2mNodeException("The validated collection contains null element at index: " + i);
            }
        }
    }

    public static boolean isUnsignedInt(Integer id) {
        return isUnsignedInt(id, 65535);
    }

    public static boolean isUnsignedInt(Integer id, int maxValue) {
        return id != null && 0 <= id && id <= maxValue;
    }

    public static void valueToPrettyString(StringBuilder b, Object value, Type type) {
        switch (type) {
        case STRING:
            b.append("\"").append(value).append("\"");
            break;
        case OPAQUE:
            // We don't print OPAQUE value as this could be credentials one.
            // Not ideal but didn't find better way for now.
            b.append(((byte[]) value).length).append(" Bytes");
            break;
        default:
            b.append(value);
            break;
        }
    }

    // --------------------------
    // Validate OBJECT
    // --------------------------

    public static boolean isValidObjectId(Integer id) {
        return isUnsignedInt(id, MAX_OBJECT_ID);
    }

    private static String getInvalidObjectIdCause(Integer id) {
        if (!isValidObjectId(id)) {
            return String.format("Invalid object id %d, It MUST be an unsigned int.", id);
        }
        return null;
    }

    public static void validateObjectId(Integer id) throws LwM2mNodeException {
        String err = getInvalidObjectIdCause(id);
        if (err != null)
            throw new LwM2mNodeException(err);
    }

    private static String getInvalidObjectPathCause(LwM2mPath path) {
        return getInvalidObjectIdCause(path.getObjectId());
    }

    private static String getInvalidPathForObjectCause(LwM2mObject object, LwM2mPath path) {
        if (!path.isObject()) {
            return String.format("Invalid Path %s : path does not target a LWM2M object for %s", path, object);
        } else if (object.getId() != path.getObjectId()) {
            return String.format("Invalid Path %s : path object id (%d) does not match LWM2M object id %d for %s", path,
                    path.getObjectId(), object.getId(), object);
        }
        return null;
    }

    // --------------------------
    // Validate OBJECT INSTANCE
    // --------------------------

    public static boolean isValidObjectInstanceId(Integer id) {
        return isUnsignedInt(id, MAX_OBJECT_INSTANCE_ID);

    }

    private static String getInvalidObjectInstanceIdCause(Integer id) {
        if (!isValidObjectInstanceId(id)) {
            return String.format("Invalid object instance id %d, It MUST be an unsigned int. (65535 is reserved)", id);
        }
        return null;
    }

    public static void validateObjectInstanceId(Integer id) throws LwM2mNodeException {
        String err = getInvalidObjectInstanceIdCause(id);
        if (err != null)
            throw new LwM2mNodeException(err);
    }

    private static String getInvalidObjectInstancePathCause(LwM2mPath path) {
        String cause = getInvalidObjectIdCause(path.getObjectId());
        if (cause != null)
            return cause;
        cause = getInvalidObjectInstanceIdCause(path.getObjectInstanceId());
        if (cause != null)
            return cause;
        return null;
    }

    private static String getInvalidUndefinedObjectInstanceIdCause(Integer id) {
        if (id != LwM2mObjectInstance.UNDEFINED) {
            return String.format("Instance id should be undefined(%d) but was %d", LwM2mObjectInstance.UNDEFINED, id);
        }
        return null;
    }

    public static void validateUndefinedObjecInstanceId(int id) throws LwM2mNodeException {
        String err = getInvalidUndefinedObjectInstanceIdCause(id);
        if (err != null)
            throw new LwM2mNodeException(err);
    }

    private static String getInvalidIncompleteObjectInstancePathCause(LwM2mPath path) {
        String cause = getInvalidObjectIdCause(path.getObjectId());
        if (cause != null)
            return cause;
        cause = getInvalidUndefinedObjectInstanceIdCause(path.getObjectInstanceId());
        if (cause != null)
            return cause;
        return null;
    }

    private static String getInvalidPathForObjectInstanceCause(LwM2mObjectInstance objectInstance, LwM2mPath path) {
        if (!path.isObjectInstance()) {
            return String.format("Invalid Path %s : path does not target a LWM2M object instance for %s", path,
                    objectInstance);
        } else if (objectInstance.getId() != path.getObjectInstanceId()) {
            return String.format(
                    "Invalid Path %s : path object instance id (%d) does not match LWM2M object instance id %d for %s",
                    path, path.getObjectInstanceId(), objectInstance.getId(), objectInstance);
        }
        return null;
    }

    // --------------------------
    // Validate RESOURCE
    // --------------------------

    public static boolean isValidResourceId(Integer id) {
        return isUnsignedInt(id, MAX_RESOURCE_ID);
    }

    private static String getInvalidResourceIdCause(Integer id) {
        if (!isValidResourceId(id)) {
            return String.format("Invalid resource id %d, It MUST be an unsigned int.", id);
        }
        return null;
    }

    public static void validateResourceId(Integer id) throws LwM2mNodeException {
        String err = getInvalidResourceIdCause(id);
        if (err != null)
            throw new LwM2mNodeException(err);
    }

    private static String getInvalidResourcePathCause(LwM2mPath path) {
        String cause = getInvalidObjectIdCause(path.getObjectId());
        if (cause != null)
            return cause;
        cause = getInvalidObjectInstanceIdCause(path.getObjectInstanceId());
        if (cause != null)
            return cause;
        cause = getInvalidResourceIdCause(path.getResourceId());
        if (cause != null)
            return cause;
        return null;
    }

    private static String getInvalidIncompleteResourcePathCause(LwM2mPath path) {
        String cause = getInvalidObjectIdCause(path.getObjectId());
        if (cause != null)
            return cause;
        cause = getInvalidUndefinedObjectInstanceIdCause(path.getObjectInstanceId());
        if (cause != null)
            return cause;
        cause = getInvalidResourceIdCause(path.getResourceId());
        if (cause != null)
            return cause;
        return null;
    }

    private static String getInvalidPathForResourceCause(LwM2mResource resource, LwM2mPath path) {
        if (!path.isResource()) {
            return String.format("Invalid Path %s : path does not target a LWM2M resource for %s", path, resource);
        } else if (resource.getId() != path.getResourceId()) {
            return String.format("Invalid Path %s : path resource id (%d) does not match LWM2M resource id %d for %s",
                    path, path.getResourceId(), resource.getId(), resource);
        }
        return null;
    }

    // --------------------------
    // Validate RESOURCE INSTANCE
    // --------------------------

    public static boolean isValidResourceInstanceId(Integer id) {
        return isUnsignedInt(id, MAX_RESOURCE_INSTANCE_ID);
    }

    private static String getInvalidResourceInstanceIdCause(Integer id) {
        if (!isValidResourceInstanceId(id)) {
            return String.format("Invalid resource instance id %d, It MUST be an unsigned int.", id);
        }
        return null;
    }

    public static void validateResourceInstanceId(Integer id) throws LwM2mNodeException {
        String err = getInvalidResourceInstanceIdCause(id);
        if (err != null)
            throw new LwM2mNodeException(err);
    }

    private static String getInvalidResourceInstancePathCause(LwM2mPath path) {
        String cause = getInvalidObjectIdCause(path.getObjectId());
        if (cause != null)
            return cause;
        cause = getInvalidObjectInstanceIdCause(path.getObjectInstanceId());
        if (cause != null)
            return cause;
        cause = getInvalidResourceIdCause(path.getResourceId());
        if (cause != null)
            return cause;
        cause = getInvalidResourceInstanceIdCause(path.getResourceInstanceId());
        if (cause != null)
            return cause;
        return null;
    }

    private static String getInvalidIncompleteResourceInstancePathCause(LwM2mPath path) {
        String cause = getInvalidObjectIdCause(path.getObjectId());
        if (cause != null)
            return cause;
        cause = getInvalidUndefinedObjectInstanceIdCause(path.getObjectInstanceId());
        if (cause != null)
            return cause;
        cause = getInvalidResourceIdCause(path.getResourceId());
        if (cause != null)
            return cause;
        cause = getInvalidResourceInstanceIdCause(path.getResourceInstanceId());
        if (cause != null)
            return cause;
        return null;
    }

    private static String getInvalidPathForResourceInstanceCause(LwM2mResourceInstance resourceInstance,
            LwM2mPath path) {
        if (!path.isResourceInstance()) {
            return String.format("Invalid Path %s : path does not target a LWM2M resource instance for %s", path,
                    resourceInstance);
        } else if (resourceInstance.getId() != path.getResourceInstanceId()) {
            return String.format(
                    "Invalid Path %s : path resource instance id (%d) does not match LWM2M resource instance id %d for %s",
                    path, path.getResourceInstanceId(), resourceInstance.getId(), resourceInstance);
        }
        return null;
    }

    // --------------------------
    // Validate Path
    // --------------------------
    private static String getInvalidPathCause(LwM2mPath path) {
        if (path.isObject()) {
            return getInvalidObjectPathCause(path);
        } else if (path.isObjectInstance()) {
            return getInvalidObjectInstancePathCause(path);
        } else if (path.isResource()) {
            return getInvalidResourcePathCause(path);
        } else if (path.isResourceInstance()) {
            return getInvalidResourceInstancePathCause(path);
        } else if (!path.isRoot()) {
            return String.format("Invalid LWM2M path (%s)", path);
        }
        return null;
    }

    public static void validatePath(LwM2mPath path) throws InvalidLwM2mPathException {
        String err = getInvalidPathCause(path);
        if (err != null)
            throw new InvalidLwM2mPathException(err);
    }

    private static String getInvalidIncompletePathCause(LwM2mPath path) {
        if (path.isObjectInstance()) {
            return getInvalidIncompleteObjectInstancePathCause(path);
        } else if (path.isResource()) {
            return getInvalidIncompleteResourcePathCause(path);
        } else if (path.isResourceInstance()) {
            return getInvalidIncompleteResourceInstancePathCause(path);
        } else {
            return String.format("Invalid 'Incomplete LWM2M path' (%s)", path);
        }
    }

    public static void validateIncompletePath(LwM2mPath path) throws InvalidLwM2mPathException {
        String err = getInvalidIncompletePathCause(path);
        if (err != null)
            throw new InvalidLwM2mPathException(err);
    }

    public static String getInvalidPathForNodeCause(LwM2mNode node, LwM2mPath path) {
        // Handle Root Node
        if (!(node instanceof LwM2mChildNode)) {
            if (!path.isRoot()) {
                return String.format("Invalid Path %s : path does not target a root node for %s", path, node);
            }
            return null;
        }

        // Handle child Node
        LwM2mChildNode childNode = (LwM2mChildNode) node;
        if (childNode instanceof LwM2mObject) {
            return getInvalidPathForObjectCause((LwM2mObject) childNode, path);
        } else if (childNode instanceof LwM2mObjectInstance) {
            return getInvalidPathForObjectInstanceCause((LwM2mObjectInstance) childNode, path);
        } else if (childNode instanceof LwM2mResource) {
            return getInvalidPathForResourceCause((LwM2mResource) childNode, path);
        } else if (childNode instanceof LwM2mResourceInstance) {
            return getInvalidPathForResourceInstanceCause((LwM2mResourceInstance) childNode, path);
        }
        return null;
    }

    /**
     * Return null is the node is an instance of allowed classes, else return a string with the error cause
     */
    public static String getUnsupportedNodeCause(LwM2mNode node, List<Class<? extends LwM2mNode>> allowedClasses) {
        for (Class<? extends LwM2mNode> allowedClass : allowedClasses) {
            if (allowedClass.isAssignableFrom(node.getClass())) {
                return null;
            }
        }
        return String.format("%s is not supported, only %s is allowed", node.getClass().getSimpleName(),
                allowedClasses.stream().map(Class::getSimpleName).collect(Collectors.toList()));
    }

    public static void validatePathForNode(LwM2mNode node, LwM2mPath path) throws InvalidLwM2mPathException {
        String err = getInvalidPathForNodeCause(node, path);
        if (err != null)
            throw new InvalidLwM2mPathException(err);
    }
}
