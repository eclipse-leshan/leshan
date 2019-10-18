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

import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An object description.
 * 
 * @see Lwm2M specification D.1 Object Template
 * @see http://openmobilealliance.org/tech/profiles/LWM2M.xsd
 */
public class ObjectModel {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectModel.class);

    public static final String DEFAULT_VERSION = "1.0";

    private static final int OMA_OBJECT_MIN_ID = 0;
    private static final int OMA_OBJECT_MAX_ID = 1023;

    public final int id;
    public final String name;
    public final String description;
    public final String version;
    public final boolean multiple;
    public final boolean mandatory;

    public final Map<Integer, ResourceModel> resources; // resources by ID

    public ObjectModel(int id, String name, String description, String version, boolean multiple, boolean mandatory,
            ResourceModel... resources) {
        this(id, name, description, version, multiple, mandatory, Arrays.asList(resources));
    }

    public ObjectModel(int id, String name, String description, String version, boolean multiple, boolean mandatory,
            Collection<ResourceModel> resources) {
        Validate.notEmpty(version);

        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
        this.multiple = multiple;
        this.mandatory = mandatory;

        Map<Integer, ResourceModel> resourcesMap = new HashMap<>(resources.size());
        for (ResourceModel resource : resources) {
            ResourceModel old = resourcesMap.put(resource.id, resource);
            if (old != null) {
                LOG.debug("Model already exists for resource {} of object {}. Overriding it.", resource.id, id);
            }
            resourcesMap.put(resource.id, resource);
        }
        this.resources = Collections.unmodifiableMap(resourcesMap);
    }

    public boolean isOmaObject() {
        return id >= OMA_OBJECT_MIN_ID && id <= OMA_OBJECT_MAX_ID;
    }

    /**
     * @return the version and if the version is null or empty return the default value 1.0
     * @see ObjectModel#DEFAULT_VERSION
     */
    public String getVersion() {
        if (version == null || version.isEmpty()) {
            return ObjectModel.DEFAULT_VERSION;
        }
        return version;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ObjectModel [id=").append(id).append(", name=").append(name).append(", description=")
                .append(description).append(", version=").append(version).append(", multiple=").append(multiple)
                .append(", mandatory=").append(mandatory).append(", resources=").append(resources).append("]");
        return builder.toString();
    }

}
