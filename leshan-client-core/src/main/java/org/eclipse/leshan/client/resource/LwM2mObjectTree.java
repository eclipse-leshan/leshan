/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.resource;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.leshan.client.LwM2mClient;
import org.eclipse.leshan.client.resource.listener.ObjectListener;
import org.eclipse.leshan.client.resource.listener.ObjectsListener;

public class LwM2mObjectTree {

    private ObjectListener dispatcher = new ObjectListenerDispatcher();
    private CopyOnWriteArrayList<ObjectsListener> listeners = new CopyOnWriteArrayList<>();
    private ConcurrentHashMap<Integer, LwM2mObjectEnabler> objectEnablers = new ConcurrentHashMap<>();

    public LwM2mObjectTree(LwM2mClient client, LwM2mObjectEnabler... enablers) {
        this(client, Arrays.asList(enablers));
    }

    public LwM2mObjectTree(LwM2mClient client, Collection<? extends LwM2mObjectEnabler> enablers) {
        for (LwM2mObjectEnabler enabler : enablers) {
            LwM2mObjectEnabler previousEnabler = objectEnablers.putIfAbsent(enabler.getId(), enabler);
            if (previousEnabler != null) {
                throw new IllegalArgumentException(
                        String.format("Can not add 2 enablers for the same id %d", enabler.getId()));
            }
        }
        for (LwM2mObjectEnabler enabler : enablers) {
            enabler.setListener(dispatcher);
            enabler.setLwM2mClient(client);
        }
    }

    public void addListener(ObjectsListener listener) {
        listeners.add(listener);
    }

    public void removedListener(ObjectsListener listener) {
        listeners.remove(listener);
    }

    public Map<Integer, LwM2mObjectEnabler> getObjectEnablers() {
        return Collections.unmodifiableMap(objectEnablers);
    }

    public LwM2mObjectEnabler getObjectEnabler(int id) {
        return objectEnablers.get(id);
    }

    public void addObjectEnabler(LwM2mObjectEnabler enabler) {
        LwM2mObjectEnabler previousEnabler = objectEnablers.putIfAbsent(enabler.getId(), enabler);
        enabler.setListener(dispatcher);
        if (previousEnabler != null) {
            throw new IllegalArgumentException(
                    String.format("Can not add 2 enablers for the same id %d", enabler.getId()));
        }
        for (ObjectsListener listener : listeners) {
            listener.objectAdded(enabler);
        }
    }

    public void removeObjectEnabler(int objectId) {
        LwM2mObjectEnabler removedEnabler = objectEnablers.remove(objectId);
        if (removedEnabler != null) {
            removedEnabler.setListener(null);
            for (ObjectsListener listener : listeners) {
                listener.objectRemoved(removedEnabler);
            }
        }
    }

    private class ObjectListenerDispatcher implements ObjectListener {
        @Override
        public void objectInstancesAdded(LwM2mObjectEnabler object, int... instanceIds) {
            for (ObjectsListener listener : listeners) {
                listener.objectInstancesAdded(object, instanceIds);
            }
        }

        @Override
        public void objectInstancesRemoved(LwM2mObjectEnabler object, int... instanceIds) {
            for (ObjectsListener listener : listeners) {
                listener.objectInstancesRemoved(object, instanceIds);
            }
        }

        @Override
        public void resourceChanged(LwM2mObjectEnabler object, int instanceId, int... resourcesIds) {
            for (ObjectsListener listener : listeners) {
                listener.resourceChanged(object, instanceId, resourcesIds);
            }
        }
    }
}
