/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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

public enum LwM2mNodeLevel {

    ROOT(0), OBJECT(1), OBJECT_INSTANCE(2), RESOURCE(3), RESOURCE_INSTANCE(4);

    private final int level;

    private LwM2mNodeLevel(int level) {
        this.level = level;
    }

    public boolean isRoot() {
        return this == ROOT;
    }

    public boolean isObject() {
        return this == OBJECT;
    }

    public boolean isObjectInstance() {
        return this == OBJECT_INSTANCE;
    }

    public boolean isResource() {
        return this == RESOURCE;
    }

    public boolean isResourceInstance() {
        return this == RESOURCE_INSTANCE;
    }

    public boolean isDeeperOrEqualThan(LwM2mNodeLevel depth) {
        return this.level >= depth.level;
    }
}
