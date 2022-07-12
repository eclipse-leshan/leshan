/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
 *     Kai Hudalla (Bosch Software Innovations GmbH) - check resource ID when executing resources
 *     Achim Kraus (Bosch Software Innovations GmbH) - add reset() for
 *                                                     REPLACE/UPDATE implementation
 *******************************************************************************/
package org.eclipse.leshan.client.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.argument.Arguments;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

/**
 * A simple implementation of {@link LwM2mInstanceEnabler} where all supported readable or writable LWM2M resource are
 * store in map as a {@link LwM2mResource} java instance.
 */
public class SimpleInstanceEnabler extends BaseInstanceEnabler {

    protected Map<Integer, LwM2mResource> resources = new HashMap<>();
    protected Map<Integer, Object> initialValues;

    public SimpleInstanceEnabler() {
    }

    public SimpleInstanceEnabler(int id) {
        super(id);
    }

    public SimpleInstanceEnabler(int id, Map<Integer, Object> initialValues) {
        super(id);
        this.initialValues = initialValues;
    }

    public SimpleInstanceEnabler(int id, Object... initialValues) {
        super(id);
        if (initialValues.length % 2 == 1)
            throw new IllegalArgumentException("initialValues length must be even, as this is a list of ID/value");
        if (initialValues.length > 0) {
            this.initialValues = new HashMap<>(initialValues.length / 2);
            for (int i = 0; i < initialValues.length; i = i + 2) {
                this.initialValues.put((Integer) initialValues[i], initialValues[i + 1]);
            }
        }
    }

    @Override
    public ReadResponse read(ServerIdentity identity, int resourceid) {
        if (resources.containsKey(resourceid)) {
            return ReadResponse.success(resources.get(resourceid));
        }
        return ReadResponse.notFound();
    }

    @Override
    public WriteResponse write(ServerIdentity identity, boolean replace, int resourceid, LwM2mResource value) {
        // define new Value
        LwM2mResource newValue;
        List<LwM2mPath> newInstances = new ArrayList<>();
        if (value instanceof LwM2mMultipleResource && !replace) {
            // This is the special case of multiple instance resource where we do not replace the resource instances but
            // we
            // merge it.
            LwM2mMultipleResource multipleResource = (LwM2mMultipleResource) resources.get(resourceid);
            if (multipleResource != null) {
                Map<Integer, LwM2mResourceInstance> mergedInstances = new HashMap<>(multipleResource.getInstances());
                // try to detect resource instance changes
                for (Entry<Integer, LwM2mResourceInstance> entry : ((LwM2mMultipleResource) value).getInstances()
                        .entrySet()) {
                    LwM2mResourceInstance previous = mergedInstances.put(entry.getKey(), entry.getValue());
                    if (!entry.getValue().equals(previous)) {
                        newInstances.add(getResourceInstancePath(resourceid, entry.getKey()));
                    }
                }
                newValue = new LwM2mMultipleResource(resourceid, value.getType(), mergedInstances.values());
            } else {
                newValue = value;
            }
        } else {
            newValue = value;
        }

        LwM2mResource previousValue = resources.put(resourceid, newValue);

        // Either we raise resource instance changes or resource change.
        if (!newInstances.isEmpty()) {
            fireResourcesChange(newInstances.toArray(new LwM2mPath[newInstances.size()]));
        } else if (!newValue.equals(previousValue)) {
            fireResourceChange(resourceid);
        }
        return WriteResponse.success();
    }

    @Override
    public ExecuteResponse execute(ServerIdentity identity, int resourceid, Arguments arguments) {
        return ExecuteResponse.notFound();
    }

    @Override
    public void reset(int resourceid) {
        resources.remove(resourceid);
    }

    @Override
    public void setModel(ObjectModel objectModel) {
        super.setModel(objectModel);

        // initialize resources
        for (ResourceModel resourceModel : objectModel.resources.values()) {
            if (resourceModel.operations.isReadable()) {
                LwM2mResource newResource = initializeResource(objectModel, resourceModel);
                if (newResource != null) {
                    resources.put(newResource.getId(), newResource);
                }
            }
        }
    }

    protected LwM2mPath[] applyValues(Map<Integer, Object> values) {
        Set<LwM2mPath> changingResources = new HashSet<>();

        for (ResourceModel resourceModel : getModel().resources.values()) {
            if (resourceModel.operations.isReadable()) {
                Object value = values.get(resourceModel.id);
                // create the resource
                LwM2mResource newResource = null;
                if (value != null) {
                    if (resourceModel.multiple) {
                        // handle multi instances
                        if (value instanceof LwM2mResourceInstance) {
                            newResource = new LwM2mMultipleResource(resourceModel.id, resourceModel.type,
                                    (LwM2mResourceInstance) value);
                            changingResources.add(getResourcePath(resourceModel.id));
                        } else if (value instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<Integer, ?> val = (Map<Integer, ?>) value;
                            newResource = LwM2mMultipleResource.newResource(resourceModel.id, val, resourceModel.type);
                        }
                    } else {
                        // handle single instances
                        newResource = LwM2mSingleResource.newResource(resourceModel.id, value, resourceModel.type);
                    }
                }
                // add the resource
                if (newResource != null) {
                    if (newResource.isMultiInstances()) {
                        for (Integer instanceID : newResource.getInstances().keySet()) {
                            changingResources.add(getResourceInstancePath(newResource.getId(), instanceID));
                        }
                    } else {
                        changingResources.add(getResourcePath(newResource.getId()));
                    }
                    LwM2mResource previous = resources.put(newResource.getId(), newResource);
                    if (previous != null && previous.isMultiInstances()) {
                        for (Integer instanceID : previous.getInstances().keySet()) {
                            changingResources.add(getResourceInstancePath(previous.getId(), instanceID));
                        }
                    }
                }
            }
        }

        return changingResources.toArray(new LwM2mPath[changingResources.size()]);
    }

    protected LwM2mResource initializeResource(ObjectModel objectModel, ResourceModel resourceModel) {
        if (!resourceModel.multiple) {
            return initializeSingleResource(objectModel, resourceModel);
        } else {
            return initializeMultipleResource(objectModel, resourceModel);
        }
    }

    protected LwM2mSingleResource initializeSingleResource(ObjectModel objectModel, ResourceModel resourceModel) {
        if (initialValues != null) {
            Object initialValue = initialValues.get(resourceModel.id);
            if (initialValue == null)
                return null;
            return LwM2mSingleResource.newResource(resourceModel.id, initialValue, resourceModel.type);
        } else {
            switch (resourceModel.type) {
            case STRING:
                return LwM2mSingleResource.newStringResource(resourceModel.id,
                        createDefaultStringValueFor(objectModel, resourceModel));
            case BOOLEAN:
                return LwM2mSingleResource.newBooleanResource(resourceModel.id,
                        createDefaultBooleanValueFor(objectModel, resourceModel));
            case INTEGER:
                return LwM2mSingleResource.newIntegerResource(resourceModel.id,
                        createDefaultIntegerValueFor(objectModel, resourceModel));
            case FLOAT:
                return LwM2mSingleResource.newFloatResource(resourceModel.id,
                        createDefaultFloatValueFor(objectModel, resourceModel));
            case TIME:
                return LwM2mSingleResource.newDateResource(resourceModel.id,
                        createDefaultDateValueFor(objectModel, resourceModel));
            case OPAQUE:
                return LwM2mSingleResource.newBinaryResource(resourceModel.id,
                        createDefaultOpaqueValueFor(objectModel, resourceModel));
            default:
                // this should not happened
                return null;
            }
        }
    }

    protected LwM2mMultipleResource initializeMultipleResource(ObjectModel objectModel, ResourceModel resourceModel) {
        if (initialValues != null) {
            Object initialValue = initialValues.get(resourceModel.id);
            if (initialValue == null)
                return null;

            if (initialValue instanceof LwM2mResourceInstance) {
                return new LwM2mMultipleResource(resourceModel.id, resourceModel.type,
                        (LwM2mResourceInstance) initialValue);
            } else if (initialValue instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<Integer, ?> val = (Map<Integer, ?>) initialValue;
                return LwM2mMultipleResource.newResource(resourceModel.id, val, resourceModel.type);
            }
            return null;
        } else {
            // no default value
            Map<Integer, ?> emptyMap = Collections.emptyMap();
            return LwM2mMultipleResource.newResource(resourceModel.id, emptyMap, resourceModel.type);
        }
    }

    protected String createDefaultStringValueFor(ObjectModel objectModel, ResourceModel resourceModel) {
        return "";
    }

    protected long createDefaultIntegerValueFor(ObjectModel objectModel, ResourceModel resourceModel) {
        return 0;
    }

    protected boolean createDefaultBooleanValueFor(ObjectModel objectModel, ResourceModel resourceModel) {
        return false;
    }

    protected Date createDefaultDateValueFor(ObjectModel objectModel, ResourceModel resourceModel) {
        return new Date(0);
    }

    protected double createDefaultFloatValueFor(ObjectModel objectModel, ResourceModel resourceModel) {
        return 0;
    }

    protected byte[] createDefaultOpaqueValueFor(ObjectModel objectModel, ResourceModel resourceModel) {
        return new byte[0];
    }
}
