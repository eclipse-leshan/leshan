/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.core.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collection of LWM2M object definitions.
 */
public class LwM2mModel {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mModel.class);

    private final Map<Integer, ObjectModel> objects; // objects by ID

    public LwM2mModel(ObjectModel... objectModels) {
        this(Arrays.asList(objectModels));
    }

    public LwM2mModel(Collection<ObjectModel> objectModels) {
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

    /**
     * Returns the description of a given resource.
     *
     * @param objectId the object identifier
     * @param resourceId the resource identifier
     * @return the resource specification or <code>null</code> if not found
     */
    public ResourceModel getResourceModel(int objectId, int resourceId) {
        ObjectModel object = objects.get(objectId);
        if (object != null) {
            return object.resources.get(resourceId);
        }
        return null;
    }

    /**
     * Returns the description of a given object.
     *
     * @param objectId the object identifier
     * @return the object definition or <code>null</code> if not found
     */
    public ObjectModel getObjectModel(int objectId) {
        return objects.get(objectId);
    }

    /**
     * @return all the objects descriptions known.
     */
    public Collection<ObjectModel> getObjectModels() {
        return objects.values();
    }

}
