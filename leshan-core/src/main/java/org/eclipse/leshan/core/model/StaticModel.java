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
package org.eclipse.leshan.core.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A static implementation of {@link LwM2mModel}.
 */
public class StaticModel implements LwM2mModel {

    private static final Logger LOG = LoggerFactory.getLogger(StaticModel.class);

    private final Map<Integer, ObjectModel> objects; // objects by ID

    public StaticModel(ObjectModel... objectModels) {
        this(Arrays.asList(objectModels));
    }

    public StaticModel(Collection<ObjectModel> objectModels) {
        if (objectModels == null) {
            objects = new HashMap<>();
        } else {
            Map<Integer, ObjectModel> map = new HashMap<>();
            for (ObjectModel model : objectModels) {
                ObjectModel old = map.put(model.id, model);
                if (old != null) {
                    LOG.debug("Model already exists for object {}. Overriding it.", model.id);
                }
            }
            objects = Collections.unmodifiableMap(map);
        }
    }

    @Override
    public ResourceModel getResourceModel(int objectId, int resourceId) {
        ObjectModel object = objects.get(objectId);
        if (object != null) {
            return object.resources.get(resourceId);
        }
        return null;
    }

    @Override
    public ObjectModel getObjectModel(int objectId) {
        return objects.get(objectId);
    }

    @Override
    public Collection<ObjectModel> getObjectModels() {
        return objects.values();
    }
}
