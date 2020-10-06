/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.core.node;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class LwM2mNodeBuilder {

    public static class InstanceBuilder {
        private int id;
        private Map<Integer, Object> resources = new HashMap<>();

        public InstanceBuilder(int id) {
            this.id = id;
        }

        public InstanceBuilder set(int resourceId, Object resourceValue) {
            Object previous = resources.put(resourceId, resourceValue);
            if (previous != null)
                throw new IllegalStateException(
                        String.format("a value already set for this resource /?/%s/%s", id, resourceId));
            return this;
        }

        @SuppressWarnings("unchecked")
        public InstanceBuilder set(int resourceId, int resourceInstanceId, Object resourceInstanceValue) {
            Object object = resources.get(resourceId);

            Map<Integer, Object> resourceInstances;
            if (object == null) {
                resourceInstances = new HashMap<>();
                resources.put(resourceId, resourceInstances);
            } else {
                if (object instanceof Map<?, ?>) {
                    resourceInstances = (Map<Integer, Object>) object;
                } else {
                    throw new IllegalStateException(
                            String.format("a SINGLE value already set for this resource /?/%s/%s", id, resourceId));
                }
            }
            resourceInstances.put(resourceInstanceId, resourceInstanceValue);

            return this;
        }

        public LwM2mObjectInstance create() {
            List<LwM2mResource> lwm2mResources = new ArrayList<>();
            for (Entry<Integer, Object> resource : resources.entrySet()) {
                Object value = resource.getValue();
                if (value instanceof Byte || value instanceof Short || value instanceof Integer
                        || value instanceof Long) {
                    lwm2mResources.add(
                            LwM2mSingleResource.newIntegerResource(resource.getKey(), ((Number) value).longValue()));
                } else if (value instanceof Float || value instanceof Double) {
                    lwm2mResources.add(
                            LwM2mSingleResource.newFloatResource(resource.getKey(), ((Number) value).doubleValue()));
                } else if (value instanceof Boolean) {
                    lwm2mResources.add(LwM2mSingleResource.newBooleanResource(resource.getKey(), (boolean) value));
                } else if (value instanceof byte[]) {
                    lwm2mResources.add(LwM2mSingleResource.newBinaryResource(resource.getKey(), (byte[]) value));
                } else if (value instanceof String) {
                    lwm2mResources.add(LwM2mSingleResource.newStringResource(resource.getKey(), (String) value));
                } else if (value instanceof Date) {
                    lwm2mResources.add(LwM2mSingleResource.newDateResource(resource.getKey(), (Date) value));
                } else if (value instanceof ObjectLink) {
                    lwm2mResources
                            .add(LwM2mSingleResource.newObjectLinkResource(resource.getKey(), (ObjectLink) value));
                } else if (!(value instanceof Map<?, ?>)) {
                    // TODO support multi instance resources
                    throw new IllegalStateException("not implemented");
                }
            }

            return new LwM2mObjectInstance(id, lwm2mResources);
        }
    }

    public static InstanceBuilder instance(int id) {
        return new InstanceBuilder(id);
    }
}
