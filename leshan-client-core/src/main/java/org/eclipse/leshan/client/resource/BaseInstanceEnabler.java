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
import java.util.List;

import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

public class BaseInstanceEnabler implements LwM2mInstanceEnabler {

    private List<ResourceChangedListener> listeners = new ArrayList<ResourceChangedListener>();

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
    public void reset(int resourceid) {
    }

}
