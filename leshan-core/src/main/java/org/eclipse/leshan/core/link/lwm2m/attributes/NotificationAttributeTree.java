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

import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.eclipse.leshan.core.node.LwM2mPath;

/**
 * A kind of tree structure which stores {@link LwM2mAttributeSet} by {@link LwM2mPath}.
 */
public class NotificationAttributeTree {

    private final ConcurrentNavigableMap<LwM2mPath, LwM2mAttributeSet> internalTree = new ConcurrentSkipListMap<>();

    public void put(LwM2mPath path, LwM2mAttributeSet newValue) {
        if (newValue.isEmpty()) {
            internalTree.remove(path);
        } else {
            internalTree.put(path, newValue);
        }
    }

    public void remove(LwM2mPath path) {
        internalTree.remove(path);
    }

    /**
     * Remove all attribute for the given {@link LwM2mPath} and all its children.
     */
    public void removeAllUnder(LwM2mPath parentPath) {
        internalTree.subMap(parentPath, true, parentPath.toMaxDescendant(), true).clear();
    }

    public boolean isEmpty() {
        return internalTree.isEmpty();
    }

    public LwM2mAttributeSet get(LwM2mPath path) {
        return internalTree.get(path);
    }

    /**
     * @return all children {@link LwM2mPath} of given parent {@link LwM2mPath} which have attached
     *         {@link LwM2mAttributeSet}
     */
    public Set<LwM2mPath> getChildren(LwM2mPath parentPath) {
        return internalTree.subMap(parentPath, false, parentPath.toMaxDescendant(), true).keySet();
    }

    /**
     * @return {@link LwM2mAttributeSet} attached to given level merged with value inherited from higher level.
     *
     * @see <a href=
     *      "https://www.openmobilealliance.org/release/LightweightM2M/V1_2_1-20221209-A/HTML-Version/OMA-TS-LightweightM2M_Core-V1_2_1-20221209-A.html#7-3-2-0-732-lessNOTIFICATIONgreater-Class-Attributes">LWM2M-v1.2.1@core7.3.2.
     *      NOTIFICATION Class Attributes</a>
     */
    public LwM2mAttributeSet getWithInheritance(LwM2mPath path) {
        // For Root Path no need to "flatten" hierarchy
        if (path.isRoot()) {
            return get(path);
        }

        // For not root path
        // Create Attribute Set taking inherited value into account.
        LwM2mAttributeSet result = get(path);
        LwM2mPath parentPath = path.toParenPath();
        while (!parentPath.isRoot()) {
            LwM2mAttributeSet parentAttributes = get(parentPath);
            if (parentAttributes != null) {
                result = parentAttributes.merge(result);
            }
            parentPath = parentPath.toParenPath();
        }
        return result;
    }
}
