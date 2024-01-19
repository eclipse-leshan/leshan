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
package org.eclipse.leshan.core.link.lwm2m.attributes;

import java.util.NavigableMap;
import java.util.TreeMap;

import org.eclipse.leshan.core.node.LwM2mPath;

public class NotificationAttributeTree {

    NavigableMap<LwM2mPath, LwM2mAttributeSet> internalTree = new TreeMap<>();

    public void put(LwM2mPath path, LwM2mAttributeSet newValue) {
        internalTree.put(path, newValue);
    }

    public void remove(LwM2mPath path) {
        internalTree.remove(path);

    }

    public LwM2mAttributeSet get(LwM2mPath path) {
        return internalTree.get(path);
    }
}
