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
import java.util.TreeMap;

import org.eclipse.leshan.core.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collection of LWM2M object definitions which could contained several definitions of the same object in different
 * version.
 */
public class LwM2mModelRepository {
    private static final Logger LOG = LoggerFactory.getLogger(LwM2mModelRepository.class);

    // This map contains all the object models available. Different version could be used.
    // This map is indexed by a string composed of objectid and version (objectid##version)
    private final Map<String, ObjectModel> objects;

    public LwM2mModelRepository(ObjectModel... objectModels) {
        this(Arrays.asList(objectModels));
    }

    public LwM2mModelRepository(Collection<ObjectModel> objectModels) {
        if (objectModels == null) {
            objects = new TreeMap<>();
        } else {
            Map<String, ObjectModel> map = new HashMap<>();
            for (ObjectModel model : objectModels) {
                String key = getKey(model);
                if (key == null) {
                    throw new IllegalArgumentException(
                            String.format("Model %s is invalid : object id is missing.", model));
                }
                ObjectModel old = map.put(key, model);
                if (old != null) {
                    LOG.debug("Model already exists for object {} in version {}. Overriding it.", model.id,
                            model.version);
                }
            }
            objects = Collections.unmodifiableMap(map);
        }
    }

    public ObjectModel getObjectModel(Integer objectId, String version) {
        Validate.notNull(objectId, "objectid must not be null");
        Validate.notNull(version, "version must not be null");

        return objects.get(getKey(objectId, version));
    }

    private String getKey(ObjectModel objectModel) {
        return getKey(objectModel.id, objectModel.version);
    }

    private String getKey(Integer objectId, String version) {
        if (objectId == null) {
            return null;
        }
        return objectId + "##" + version;
    }
}
