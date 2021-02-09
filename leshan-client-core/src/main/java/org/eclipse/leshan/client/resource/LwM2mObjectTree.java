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
import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.Startable;
import org.eclipse.leshan.core.Stoppable;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mPath;

/**
 * The LWM2M Object Tree.
 * <p>
 * It contains all the {@link LwM2mObjectEnabler} which are the implementation of each LWM2M object supported by the
 * client.
 */
public class LwM2mObjectTree implements Startable, Stoppable, Destroyable {

    protected final ObjectListener dispatcher = new ObjectListenerDispatcher();
    protected final CopyOnWriteArrayList<ObjectsListener> listeners = new CopyOnWriteArrayList<>();
    protected final ConcurrentHashMap<Integer, LwM2mObjectEnabler> objectEnablers = new ConcurrentHashMap<>();
    protected final LwM2mModel model;

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
            enabler.addListener(dispatcher);
            enabler.setLwM2mClient(client);
        }

        this.model = new LwM2mModel() {

            @Override
            public ResourceModel getResourceModel(int objectId, int resourceId) {
                ObjectModel objectModel = this.getObjectModel(objectId);
                if (objectModel != null)
                    return objectModel.resources.get(resourceId);
                return null;
            }

            @Override
            public Collection<ObjectModel> getObjectModels() {
                // TODO implements this ?
                throw new UnsupportedOperationException("Not implemented");
            }

            @Override
            public ObjectModel getObjectModel(int objectId) {
                LwM2mObjectEnabler objectEnabler = getObjectEnabler(objectId);
                if (objectEnabler != null)
                    return objectEnabler.getObjectModel();
                return null;
            }
        };
    }

    public void addListener(ObjectsListener listener) {
        listeners.add(listener);
    }

    public void removedListener(ObjectsListener listener) {
        listeners.remove(listener);
    }

    public LwM2mModel getModel() {
        return model;
    }

    public Map<Integer, LwM2mObjectEnabler> getObjectEnablers() {
        return Collections.unmodifiableMap(objectEnablers);
    }

    public LwM2mObjectEnabler getObjectEnabler(int id) {
        return objectEnablers.get(id);
    }

    public void addObjectEnabler(LwM2mObjectEnabler enabler) {
        LwM2mObjectEnabler previousEnabler = objectEnablers.putIfAbsent(enabler.getId(), enabler);
        enabler.addListener(dispatcher);
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
            removedEnabler.removeListener(dispatcher);
            for (ObjectsListener listener : listeners) {
                listener.objectRemoved(removedEnabler);
            }
        }
    }

    @Override
    public void destroy() {
        for (LwM2mObjectEnabler objectEnabler : objectEnablers.values()) {
            if (objectEnabler instanceof Destroyable) {
                ((Destroyable) objectEnabler).destroy();
            } else if (objectEnabler instanceof Stoppable) {
                ((Stoppable) objectEnabler).stop();
            }
        }
    }

    @Override
    public void start() {
        for (LwM2mObjectEnabler objectEnabler : objectEnablers.values()) {
            if (objectEnabler instanceof Startable) {
                ((Startable) objectEnabler).start();
            }
        }
    }

    @Override
    public void stop() {
        for (LwM2mObjectEnabler objectEnabler : objectEnablers.values()) {
            if (objectEnabler instanceof Stoppable) {
                ((Stoppable) objectEnabler).stop();
            }
        }
    }

    protected class ObjectListenerDispatcher implements ObjectListener {
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

    public void beginTransaction(Map<Integer, LwM2mObjectEnabler> enablers) {
        for (LwM2mObjectEnabler enabler : enablers.values()) {
            enabler.beginTransaction(LwM2mPath.ROOT_DEPTH);
        }
    }

    public void endTransaction(Map<Integer, LwM2mObjectEnabler> enablers) {
        for (LwM2mObjectEnabler enabler : enablers.values()) {
            enabler.endTransaction(LwM2mPath.ROOT_DEPTH);
        }
    }
}
