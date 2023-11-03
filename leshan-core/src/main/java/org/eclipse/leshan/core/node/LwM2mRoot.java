/*******************************************************************************
 * Copyright (c) 2013-2023 Sierra Wireless and others.
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
 *     Orange Polska  S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.node;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LwM2mRoot implements LwM2mNode {
    private final Map<Integer, LwM2mObject> objects;

    public LwM2mRoot(Collection<LwM2mObject> objects) {
        LwM2mNodeUtil.validateNotNull(objects, "objects MUST NOT be null");
        HashMap<Integer, LwM2mObject> objectsMap = new HashMap<>(objects.size());
        for (LwM2mObject object : objects) {
            LwM2mObject previous = objectsMap.put(object.getId(), object);
            if (previous != null) {
                throw new LwM2mNodeException(
                        "Unable to create LwM2mRoot : there are several objects with the same id %d", object.getId());
            }
        }
        this.objects = Collections.unmodifiableMap(objectsMap);
    }

    @Override
    public void accept(LwM2mNodeVisitor visitor) {
        visitor.visit(this);
    }

    public Map<Integer, LwM2mObject> getObjects() {
        return objects;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((objects == null) ? 0 : objects.hashCode());
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
        LwM2mRoot other = (LwM2mRoot) obj;
        if (objects == null) {
            if (other.objects != null)
                return false;
        } else if (!objects.equals(other.objects))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("LwM2mRoot [objects=%s]", objects);
    }
}
