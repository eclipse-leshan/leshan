/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.client.resource.listener.ObjectListener;

/**
 * An {@link ObjectListener} which is able to store notification during transaction and raise all grouped event at the
 * end of the transaction.
 * <p>
 * This class is not threadsafe.
 */
public class TransactionalObjectListener implements ObjectListener {
    protected boolean inTransaction = false;
    protected List<Integer> instancesAdded = new ArrayList<>();
    protected List<Integer> instancesRemoved = new ArrayList<>();
    protected Map<Integer, List<Integer>> resourcesChangedByInstance = new HashMap<>();

    protected LwM2mObjectEnabler objectEnabler;
    protected List<ObjectListener> innerListeners = new ArrayList<ObjectListener>();

    public TransactionalObjectListener(LwM2mObjectEnabler objectEnabler) {
        this.objectEnabler = objectEnabler;
    }

    public void addListener(ObjectListener listener) {
        innerListeners.add(listener);
    }

    public void removeListener(ObjectListener listener) {
        innerListeners.remove(listener);
    }

    public void beginTransaction() {
        inTransaction = true;
    }

    public void endTransaction() {
        fireStoredEvents();
        instancesAdded.clear();
        instancesRemoved.clear();
        resourcesChangedByInstance.clear();
        inTransaction = false;
    }

    protected void fireStoredEvents() {
        if (!instancesAdded.isEmpty())
            fireObjectInstancesAdded(toIntArray(instancesAdded));
        if (!instancesRemoved.isEmpty())
            fireObjectInstancesRemoved(toIntArray(instancesRemoved));

        for (Map.Entry<Integer, List<Integer>> entry : resourcesChangedByInstance.entrySet()) {
            fireResourcesChanged(entry.getKey(), toIntArray(entry.getValue()));
        }
    }

    @Override
    public void objectInstancesAdded(LwM2mObjectEnabler object, int... instanceIds) {
        if (!inTransaction) {
            fireObjectInstancesAdded(instanceIds);
        } else {
            // store additions
            for (int instanceId : instanceIds) {
                if (instancesRemoved.contains(instanceId)) {
                    instancesRemoved.remove(instanceId);
                } else if (!instancesAdded.contains(instanceId)) {
                    instancesAdded.add(instanceId);
                }
            }
        }
    }

    @Override
    public void objectInstancesRemoved(LwM2mObjectEnabler object, int... instanceIds) {
        if (!inTransaction) {
            fireObjectInstancesRemoved(instanceIds);
        } else {
            // store deletion
            for (int instanceId : instanceIds) {
                if (instancesAdded.contains(instanceId)) {
                    instancesAdded.remove(instanceId);
                } else if (!instancesRemoved.contains(instanceId)) {
                    instancesRemoved.add(instanceId);
                }
            }
        }
    }

    @Override
    public void resourceChanged(LwM2mObjectEnabler object, int instanceId, int... resourcesIds) {
        if (!inTransaction) {
            fireResourcesChanged(instanceId, resourcesIds);
        } else {
            List<Integer> resourcesChanged = resourcesChangedByInstance.get(instanceId);
            if (resourcesChanged == null) {
                resourcesChanged = new ArrayList<Integer>();
                resourcesChangedByInstance.put(instanceId, resourcesChanged);
            }
            for (int resourceId : resourcesIds) {
                resourcesChanged.add(resourceId);
            }
        }
    }

    protected int[] toIntArray(List<Integer> list) {
        int[] ret = new int[list.size()];
        int i = 0;
        for (Integer e : list)
            ret[i++] = e;
        return ret;
    }

    protected void fireObjectInstancesAdded(int... instanceIds) {
        for (ObjectListener listener : innerListeners) {
            listener.objectInstancesAdded(objectEnabler, instanceIds);
        }
    }

    protected void fireObjectInstancesRemoved(int... instanceIds) {
        for (ObjectListener listener : innerListeners) {
            listener.objectInstancesRemoved(objectEnabler, instanceIds);
        }
    }

    protected void fireResourcesChanged(int instanceid, int... resourceIds) {
        for (ObjectListener listener : innerListeners) {
            listener.resourceChanged(objectEnabler, instanceid, resourceIds);
        }
    }
}
