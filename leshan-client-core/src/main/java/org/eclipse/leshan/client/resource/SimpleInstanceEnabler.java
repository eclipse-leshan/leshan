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
 *     Kai Hudalla (Bosch Software Innovations GmbH) - check resource ID when executing resources
 *     Achim Kraus (Bosch Software Innovations GmbH) - add reset() for 
 *                                                     REPLACE/UPDATE implementation
 *******************************************************************************/
package org.eclipse.leshan.client.resource;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleInstanceEnabler extends BaseInstanceEnabler {

    private static Logger LOG = LoggerFactory.getLogger(SimpleInstanceEnabler.class);
    protected Map<Integer, LwM2mResource> resources = new HashMap<Integer, LwM2mResource>();
    protected ObjectModel objectModel;

    @Override
    public ReadResponse read(int resourceid) {
        if (resources.containsKey(resourceid)) {
            return ReadResponse.success(resources.get(resourceid));
        }
        return ReadResponse.notFound();
    }

    @Override
    public WriteResponse write(int resourceid, LwM2mResource value) {
        LwM2mResource previousValue = resources.put(resourceid, value);
        if (!value.equals(previousValue))
            fireResourcesChange(resourceid);
        return WriteResponse.success();
    }

    @Override
    public ExecuteResponse execute(int resourceid, String params) {
        if (objectModel.resources.containsKey(resourceid)) {
            LOG.info("Executing resource [{}] with params [{}]", resourceid, params);
            return ExecuteResponse.success();
        } else {
            return ExecuteResponse.notFound();
        }
    }

    @Override
    public void reset(int resourceid) {
        resources.remove(resourceid);
    }

    public void setObjectModel(ObjectModel objectModel) {
        this.objectModel = objectModel;

        // initialize resources
        for (ResourceModel resourceModel : objectModel.resources.values()) {
            if (resourceModel.operations.isReadable()) {
                LwM2mResource newResource = createResource(objectModel, resourceModel);
                if (newResource != null) {
                    resources.put(newResource.getId(), newResource);
                }
            }
        }
    }

    protected LwM2mResource createResource(ObjectModel objectModel, ResourceModel resourceModel) {
        if (!resourceModel.multiple) {
            switch (resourceModel.type) {
            case STRING:
                return LwM2mSingleResource.newStringResource(resourceModel.id,
                        createDefaultStringValue(objectModel, resourceModel));
            case BOOLEAN:
                return LwM2mSingleResource.newBooleanResource(resourceModel.id,
                        createDefaultBooleanValue(objectModel, resourceModel));
            case INTEGER:
                return LwM2mSingleResource.newIntegerResource(resourceModel.id,
                        createDefaultIntegerValue(objectModel, resourceModel));
            case FLOAT:
                return LwM2mSingleResource.newFloatResource(resourceModel.id,
                        createDefaultFloatValue(objectModel, resourceModel));
            case TIME:
                return LwM2mSingleResource.newDateResource(resourceModel.id,
                        createDefaultDateValue(objectModel, resourceModel));
            case OPAQUE:
                return LwM2mSingleResource.newBinaryResource(resourceModel.id,
                        createDefaultOpaqueValue(objectModel, resourceModel));
            default:
                // this should not happened
                return null;
            }
        } else {
            Map<Integer, Object> values = new HashMap<Integer, Object>();
            switch (resourceModel.type) {
            case STRING:
                values.put(0, createDefaultStringValue(objectModel, resourceModel));
                break;
            case BOOLEAN:
                values.put(0, createDefaultBooleanValue(objectModel, resourceModel));
                values.put(1, createDefaultBooleanValue(objectModel, resourceModel));
                break;
            case INTEGER:
                values.put(0, createDefaultIntegerValue(objectModel, resourceModel));
                values.put(1, createDefaultIntegerValue(objectModel, resourceModel));
                break;
            case FLOAT:
                values.put(0, createDefaultFloatValue(objectModel, resourceModel));
                values.put(1, createDefaultFloatValue(objectModel, resourceModel));
                break;
            case TIME:
                values.put(0, createDefaultDateValue(objectModel, resourceModel));
                break;
            case OPAQUE:
                values.put(0, createDefaultOpaqueValue(objectModel, resourceModel));
                break;
            default:
                // this should not happened
                values = null;
                break;
            }
            if (values != null)
                return LwM2mMultipleResource.newResource(resourceModel.id, values, resourceModel.type);
        }
        return null;
    }

    protected String createDefaultStringValue(ObjectModel objectModel, ResourceModel resourceModel) {
        return resourceModel.name;
    }

    protected long createDefaultIntegerValue(ObjectModel objectModel, ResourceModel resourceModel) {
        return (long) (Math.random() * 100 % 101);
    }

    protected boolean createDefaultBooleanValue(ObjectModel objectModel, ResourceModel resourceModel) {
        return Math.random() * 100 % 2 == 0;
    }

    protected Date createDefaultDateValue(ObjectModel objectModel, ResourceModel resourceModel) {
        return new Date();
    }

    protected double createDefaultFloatValue(ObjectModel objectModel, ResourceModel resourceModel) {
        return (double) Math.random() * 100;
    }

    protected byte[] createDefaultOpaqueValue(ObjectModel objectModel, ResourceModel resourceModel) {
        return new String("Default " + resourceModel.name).getBytes();
    }
}
