/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - add reset() for 
 *                                                     REPLACE/UPDATE implementation
 *******************************************************************************/
package org.eclipse.leshan.client.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

public class BaseInstanceEnabler implements LwM2mInstanceEnabler {

    private List<ResourceChangedListener> listeners = new ArrayList<>();

    @Override
    public void addResourceChangedListener(ResourceChangedListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeResourceChangedListener(ResourceChangedListener listener) {
        listeners.remove(listener);
    }

    public void fireResourcesChange(int... resourceIds) {
        for (ResourceChangedListener listener : listeners) {
            listener.resourcesChanged(resourceIds);
        }
    }

    @Override
    public ReadResponse read(int resourceid) {
        return ReadResponse.notFound();
    }

    @Override
    public WriteResponse write(int resourceid, LwM2mResource value) {
        return WriteResponse.notFound();
    }

    @Override
    public ExecuteResponse execute(int resourceid, String params) {
        return ExecuteResponse.notFound();
    }

    @Override
    public ObserveResponse observe(int resourceid) {
        // Perform a read by default
        ReadResponse readResponse = this.read(resourceid);
        return new ObserveResponse(readResponse.getCode(), readResponse.getContent(), null, null,
                readResponse.getErrorMessage());
    }

    @Override
    public void reset(int resourceid) {
        // No default behavior
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        // By default we consider that all resources defined in the model are supported
        ArrayList<Integer> resourceIds = new ArrayList<>(model.resources.keySet());
        Collections.sort(resourceIds);
        return resourceIds;
    }
}
