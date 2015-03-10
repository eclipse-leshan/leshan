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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.server.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StandardModelProvider implements LwM2mModelProvider {

    private static final Logger LOG = LoggerFactory.getLogger(StandardModelProvider.class);

    private final LwM2mModel model;

    public StandardModelProvider() {
        // build a single model with default objects
        List<ObjectModel> models = ObjectLoader.loadDefault();

        Map<Integer, ObjectModel> map = new HashMap<>();
        for (ObjectModel model : models) {
            LOG.debug("Loading object: {}", model);
            ObjectModel old = map.put(model.id, model);
            if (old != null) {
                LOG.debug("Model already exists for object {}. Overriding it.", model.id);
            }
        }
        this.model = new LwM2mModel(map);
    }

    @Override
    public LwM2mModel getObjectModel(Client client) {
        // same model for all clients
        return model;
    }
}
