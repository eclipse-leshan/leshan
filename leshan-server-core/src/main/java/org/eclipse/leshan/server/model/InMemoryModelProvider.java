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
package org.eclipse.leshan.server.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.server.client.Client;

public class InMemoryModelProvider extends StandardModelProvider {

    private Map<String, LwM2mModel> models = new HashMap<>();
    private List<ModelListener> listeners = new ArrayList<>();

    public InMemoryModelProvider() {
        super();
    }

    public void updateObject(String endpoint, LinkObject[] linkObjects) {
        ObjectModel objectModel = LinkFormatHelper.getObjectModel(linkObjects, null);
        LwM2mModel model = models.get(endpoint);
        if (model == null) {
            Map<Integer, ObjectModel> objects = new HashMap<>();
            for (ObjectModel oModel : super.getObjectModel(null).getObjectModels()) {
                objects.put(oModel.id, oModel);
            }
            model = new LwM2mModel(objects);
            models.put(endpoint, model);
        }
        model.putObjectModel(objectModel.id, objectModel);
        fireObjectChanged(endpoint, objectModel);
    }

    @Override
    public LwM2mModel getObjectModel(Client client) {
        String endpoint = client.getEndpoint();
        LwM2mModel clientModel = models.get(endpoint);
        if (clientModel != null)
            return clientModel;
        else
            return super.getObjectModel(null);
    }

    public void addModelListener(ModelListener listener) {
        listeners.add(listener);
    }

    public void removeModelListener(ModelListener listener) {
        listeners.add(listener);
    }

    protected void fireModelChanged(String endpoint, LwM2mModel newModel) {
        for (ModelListener listener : listeners) {
            listener.modelChanged(endpoint, newModel);
        }
    }

    protected void fireObjectChanged(String endpoint, ObjectModel newModel) {
        for (ModelListener listener : listeners) {
            listener.objectChanged(endpoint, newModel);
        }
    }
}
