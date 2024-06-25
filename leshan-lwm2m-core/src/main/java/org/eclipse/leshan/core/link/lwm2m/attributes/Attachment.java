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
 * The attachment level of an LwM2m attribute.
 * <p>
 * The Level (Object, Object Instance, Resource, Resource Instance) to which an Attribute is attached.
 * <p>
 * In LWM2M v1.1.1, there is some confusion between assignation level and attachement. This is attachement in a LWM2M
 * v1.2.1 meaning.
 *
 * @see <a href="https://github.com/eclipse-leshan/leshan/issues/1588">Why we are using LWM2M v1.2.1 wording</a>
 *
 */
public enum Attachment {
    ROOT, OBJECT, OBJECT_INSTANCE, RESOURCE, RESOURCE_INTANCE;

    public static Attachment fromPath(LwM2mPath path) {
        Attachment attachement = null;
        if (path.isRoot()) {
            attachement = Attachment.ROOT;
        } else if (path.isObject()) {
            attachement = Attachment.OBJECT;
        } else if (path.isObjectInstance()) {
            attachement = Attachment.OBJECT_INSTANCE;
        } else if (path.isResource()) {
            attachement = Attachment.RESOURCE;
        } else if (path.isResourceInstance()) {
            attachement = Attachment.RESOURCE_INTANCE;
        }
        return attachement;
    }
}
