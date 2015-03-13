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
 *******************************************************************************/
package org.eclipse.leshan.client.resource;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ValueResponse;

public class BaseInstanceEnabler implements LwM2mInstanceEnabler {

    private List<ResourceChangedListener> listeners = new ArrayList<ResourceChangedListener>();
    protected ObjectModel objectModel = null;

    @Override
    public void setObjectModel(ObjectModel objectModel) {
        this.objectModel = objectModel;
    }

    @Override
    public void addResourceChangedListener(ResourceChangedListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeResourceChangedListener(ResourceChangedListener listener) {
        listeners.remove(listener);
    }

    public void fireResourceChange(int resourceId) {
        for (ResourceChangedListener listener : listeners) {
            listener.resourceChanged(resourceId);
        }
    }

    @Override
    public ValueResponse read(int resourceid) {
        return new ValueResponse(ResponseCode.NOT_FOUND);
    }

    @Override
    public LwM2mResponse write(int resourceid, LwM2mResource value) {
        return new LwM2mResponse(ResponseCode.NOT_FOUND);
    }

    @Override
    public LwM2mResponse execute(int resourceid, byte[] params) {
        return new LwM2mResponse(ResponseCode.NOT_FOUND);
    }
}
