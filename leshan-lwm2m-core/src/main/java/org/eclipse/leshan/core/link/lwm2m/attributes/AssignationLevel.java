/*******************************************************************************
 * Copyright (c) 2013-2018 Sierra Wireless and others.
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
 *     Daniel Persson (Husqvarna Group) - Attribute support
 *******************************************************************************/
package org.eclipse.leshan.core.link.lwm2m.attributes;

import org.eclipse.leshan.core.node.LwM2mPath;

/**
 * The assignation level of an {@link LwM2mAttribute}.
 * <p>
 * An attribute can only be applied on one level, but it can be assigned on many levels and then be inherited down to
 * its application level.
 */
public enum AssignationLevel {
    ROOT, OBJECT, OBJECT_INSTANCE, RESOURCE, RESOURCE_INTANCE;

    public static AssignationLevel fromPath(LwM2mPath path) {
        AssignationLevel assignationLevel = null;
        if (path.isRoot()) {
            assignationLevel = AssignationLevel.ROOT;
        } else if (path.isObject()) {
            assignationLevel = AssignationLevel.OBJECT;
        } else if (path.isObjectInstance()) {
            assignationLevel = AssignationLevel.OBJECT_INSTANCE;
        } else if (path.isResource()) {
            assignationLevel = AssignationLevel.RESOURCE;
        } else if (path.isResourceInstance()) {
            assignationLevel = AssignationLevel.RESOURCE_INTANCE;
        }
        return assignationLevel;
    }
}
