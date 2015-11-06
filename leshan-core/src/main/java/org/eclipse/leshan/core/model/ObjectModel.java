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
 * An object description
 */
public class ObjectModel {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectModel.class);

    public final int id;
    public final String name;
    public final String description;
    public final boolean multiple;
    public final boolean mandatory;

    public final Map<Integer, ResourceModel> resources; // resources by ID

    public ObjectModel(int id, String name, String description, boolean multiple, boolean mandatory,
            ResourceModel... resources) {
        this(id, name, description, multiple, mandatory, Arrays.asList(resources));
    }

    public ObjectModel(int id, String name, String description, boolean multiple, boolean mandatory,
            Collection<ResourceModel> resources) {
        this.id = id;
        this.name = name;
        this.description = description;
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ObjectModel [id=").append(id).append(", name=").append(name).append(", description=")
                .append(description).append(", multiple=").append(multiple).append(", mandatory=").append(mandatory)
                .append(", resources=").append(resources).append("]");
        return builder.toString();
    }

}
