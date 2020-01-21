/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
    private boolean inTransaction = false;
    private List<Integer> instancesAdded = new ArrayList<>();
    private List<Integer> instancesRemoved = new ArrayList<>();
    private Map<Integer, List<Integer>> resourcesChangedByInstance = new HashMap<>();

    private LwM2mObjectEnabler objectEnabler;
    private ObjectListener innerListener;

    public TransactionalObjectListener(LwM2mObjectEnabler objectEnabler, ObjectListener listener) {
        this.objectEnabler = objectEnabler;
        this.innerListener = listener;
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

    private void fireStoredEvents() {
        if (!instancesAdded.isEmpty())
            innerListener.objectInstancesAdded(objectEnabler, toIntArray(instancesAdded));
        if (!instancesRemoved.isEmpty())
            innerListener.objectInstancesRemoved(objectEnabler, toIntArray(instancesRemoved));

        for (Map.Entry<Integer, List<Integer>> entry : resourcesChangedByInstance.entrySet()) {
            innerListener.resourceChanged(objectEnabler, entry.getKey(), toIntArray(entry.getValue()));
        }
    }

    @Override
    public void objectInstancesAdded(LwM2mObjectEnabler object, int... instanceIds) {
        if (!inTransaction) {
            innerListener.objectInstancesAdded(object, instanceIds);
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
            innerListener.objectInstancesRemoved(object, instanceIds);
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
            innerListener.resourceChanged(object, instanceId, resourcesIds);
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

    private int[] toIntArray(List<Integer> list) {
        int[] ret = new int[list.size()];
        int i = 0;
        for (Integer e : list)
            ret[i++] = e;
        return ret;
    }
}
