/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
    public int getId() {
        return 0;
    }

    @Override
    public void accept(LwM2mNodeVisitor visitor) {
        // TODO: visitors currently don't support a visit from LwM2mRoot
        visitor.visit(this);
    }

    @Override
    public String toPrettyString(LwM2mPath path) {
        return null;
    }

    @Override
    public StringBuilder appendPrettyNode(StringBuilder b, LwM2mPath path) {
        return null;
    }

    public Map<Integer, LwM2mObject> getObjects() {
        return objects;
    }

    // TODO: add compare and other stuff that a LwM2m node should have
}
