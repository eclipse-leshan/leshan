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
package org.eclipse.leshan.core.node;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.util.Validate;

/**
 * An instance of {@link LwM2mObject}.
 */
public class LwM2mObjectInstance implements LwM2mNode {

    /** Undefined instance Id */
    public static final int UNDEFINED = -1;

    private final int id;

    private final Map<Integer, LwM2mResource> resources;

    public LwM2mObjectInstance(int id, Collection<LwM2mResource> resources) {
        Validate.notNull(resources);

        this.id = id;
        Map<Integer, LwM2mResource> resourcesMap = new HashMap<>(resources.size());
        for (LwM2mResource resource : resources) {
            resourcesMap.put(resource.getId(), resource);
        }
        this.resources = Collections.unmodifiableMap(resourcesMap);
    }

    public LwM2mObjectInstance(int id, LwM2mResource... resources) {
        this(id, Arrays.asList(resources));
    }

    @Override
    public void accept(LwM2mNodeVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getId() {
        return id;
    }

    /**
     * Returns a map of resources by id.
     *
     * @return the resources
     */
    public Map<Integer, LwM2mResource> getResources() {
        return resources;
    }

    /**
     * @return the resource with the given id or {@code null} if there is no resource for this id.
     */
    public LwM2mResource getResource(int id) {
        return resources.get(id);
    }

    @Override
    public String toString() {
        return String.format("LwM2mObjectInstance [id=%s, resources=%s]", id, resources);
    }

    public String prettyPrint() {
        StringBuilder builder = new StringBuilder();
        builder.append("LwM2mObjectInstance [id=").append(id).append("]");
        for (LwM2mResource r : resources.values()) {
            builder.append("\n\t").append(r);
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + ((resources == null) ? 0 : resources.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LwM2mObjectInstance other = (LwM2mObjectInstance) obj;
        if (id != other.id) {
            return false;
        }
        if (resources == null) {
            if (other.resources != null) {
                return false;
            }
        } else if (!resources.equals(other.resources)) {
            return false;
        }
        return true;
    }

}
