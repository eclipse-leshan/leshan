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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.request.argument.Arguments;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.core.util.datatype.ULong;

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
    public ReadResponse read(LwM2mServer server, int resourceid) {
        if (resources.containsKey(resourceid)) {
            return ReadResponse.success(resources.get(resourceid));
        }
        return ReadResponse.notFound();
    }

    @Override
    public WriteResponse write(LwM2mServer server, boolean replace, int resourceid, LwM2mResource valueToWrite) {
        // Get resource model
        ResourceModel resourceModel = getModel().resources.get(resourceid);
        if (resourceModel == null) {
            return WriteResponse.notFound();
        }

        // Define new value
        LwM2mResource newValue;
        if (resourceModel.multiple && !replace) {
            // This is the special case of multiple instance resource
            // where we do not replace the resource instances but we merge it.
            LwM2mMultipleResource previousValue = (LwM2mMultipleResource) resources.get(resourceid);
            if (previousValue != null) {
                Map<Integer, LwM2mResourceInstance> mergedInstances = new HashMap<>();
                mergedInstances.putAll(previousValue.getInstances());
                mergedInstances.putAll(valueToWrite.getInstances());
                newValue = new LwM2mMultipleResource(resourceid, valueToWrite.getType(), mergedInstances.values());
            } else {
                newValue = valueToWrite;
            }
        } else {
            newValue = valueToWrite;
        }

        // Update value
        LwM2mResource previousValue = resources.put(resourceid, newValue);

        // Detect changes
        Set<LwM2mPath> changedResources = new HashSet<>();
        if (resourceModel.multiple) {
            previousValue.getInstances().forEach((previousInstanceId, previousInstance) -> {
                LwM2mResourceInstance newInstance = newValue.getInstances().get(previousInstanceId);
                if (newInstance == null) {
                    // deletion
                    changedResources.add(getResourceInstancePath(resourceid, previousInstanceId));
                } else {
                    if (!newInstance.equals(previousInstance)) {
                        // modification
                        changedResources.add(getResourceInstancePath(resourceid, previousInstanceId));
                    }
                }
            });

            newValue.getInstances().forEach((newInstanceId, newInstance) -> {
                LwM2mResourceInstance previousInstance = previousValue.getInstances().get(newInstanceId);
                if (previousInstance == null) {
                    // addition
                    changedResources.add(getResourceInstancePath(resourceid, newInstanceId));
                }
            });

            // also add resource path as we can consider that modifying child is a modification.
            if (!changedResources.isEmpty()) {
                changedResources.add(getResourcePath(resourceid));
            }

        } else {
            if (!newValue.equals(previousValue)) {
                changedResources.add(getResourcePath(resourceid));
            }
        }
        // Raise changes
        if (!changedResources.isEmpty()) {
            fireResourcesChange(changedResources.toArray(new LwM2mPath[changedResources.size()]));
        }
        return WriteResponse.success();
    }

    @Override
    public ExecuteResponse execute(LwM2mServer server, int resourceid, Arguments arguments) {
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
            case UNSIGNED_INTEGER:
                return LwM2mSingleResource.newUnsignedIntegerResource(resourceModel.id,
                        createDefaultUnsignedIntegerValueFor(objectModel, resourceModel));
            case OBJLNK:
                return LwM2mSingleResource.newObjectLinkResource(resourceModel.id,
                        createDefaultObjectLinkValueFor(objectModel, resourceModel));
            case CORELINK:
                return LwM2mSingleResource.newCoreLinkResource(resourceModel.id,
                        createDefaultCoreLinkValueFor(objectModel, resourceModel));
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

    protected ULong createDefaultUnsignedIntegerValueFor(ObjectModel objectModel, ResourceModel resourceModel) {
        return ULong.valueOf(0);
    }

    protected ObjectLink createDefaultObjectLinkValueFor(ObjectModel objectModel, ResourceModel resourceModel) {
        return new ObjectLink();
    }

    protected Link[] createDefaultCoreLinkValueFor(ObjectModel objectModel, ResourceModel resourceModel) {
        return new Link[0];
    }
}
