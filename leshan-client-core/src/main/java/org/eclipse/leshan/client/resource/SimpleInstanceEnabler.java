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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

public class SimpleInstanceEnabler extends BaseInstanceEnabler {

    protected Map<Integer, LwM2mResource> resources = new HashMap<>();

    public SimpleInstanceEnabler() {
    }

    public SimpleInstanceEnabler(int id) {
        super(id);
    }

    @Override
    public ReadResponse read(ServerIdentity identity, int resourceid) {
        if (resources.containsKey(resourceid)) {
            return ReadResponse.success(resources.get(resourceid));
        }
        return ReadResponse.notFound();
    }

    @Override
    public WriteResponse write(ServerIdentity identity, int resourceid, LwM2mResource value) {
        LwM2mResource previousValue = resources.put(resourceid, value);
        if (!value.equals(previousValue))
            fireResourcesChange(resourceid);
        return WriteResponse.success();
    }

    @Override
    public ExecuteResponse execute(ServerIdentity identity, int resourceid, String params) {
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

    protected LwM2mResource initializeResource(ObjectModel objectModel, ResourceModel resourceModel) {
        if (!resourceModel.multiple) {
            return initializeSingleResource(objectModel, resourceModel);
        } else {
            return initializeMultipleResource(objectModel, resourceModel);
        }
    }

    protected LwM2mSingleResource initializeSingleResource(ObjectModel objectModel, ResourceModel resourceModel) {
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

    protected LwM2mMultipleResource initializeMultipleResource(ObjectModel objectModel, ResourceModel resourceModel) {
        // no default value
        Map<Integer, ?> emptyMap = Collections.emptyMap();
        return LwM2mMultipleResource.newResource(resourceModel.id, emptyMap, resourceModel.type);
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
