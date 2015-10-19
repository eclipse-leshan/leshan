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
 *******************************************************************************/
package org.eclipse.leshan.client.resource;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.Value;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

public class SimpleInstanceEnabler extends BaseInstanceEnabler {

    Map<Integer, LwM2mResource> resources = new HashMap<Integer, LwM2mResource>();

    @Override
    public ReadResponse read(int resourceid) {
        if (resources.containsKey(resourceid)) {
            return ReadResponse.success(resources.get(resourceid));
        }
        return ReadResponse.notFound();
    }

    @Override
    public WriteResponse write(int resourceid, LwM2mResource value) {
        LwM2mResource previousValue = resources.get(resourceid);
        resources.put(resourceid, value);
        if (!value.equals(previousValue))
            fireResourceChange(resourceid);
        return WriteResponse.success();
    }

    @Override
    public ExecuteResponse execute(int resourceid, byte[] params) {
        System.out.println("Execute on resource " + resourceid + " params " + params);
        return ExecuteResponse.success();
    }

    @Override
    public void setObjectModel(ObjectModel objectModel) {
        super.setObjectModel(objectModel);

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
            Value<?> value;
            switch (resourceModel.type) {
            case STRING:
                value = createDefaultStringValue(objectModel, resourceModel);
                break;
            case BOOLEAN:
                value = createDefaultBooleanValue(objectModel, resourceModel);
                break;
            case INTEGER:
                value = createDefaultIntegerValue(objectModel, resourceModel);
                break;
            case FLOAT:
                value = createDefaultFloatValue(objectModel, resourceModel);
                break;
            case TIME:
                value = createDefaultDateValue(objectModel, resourceModel);
                break;
            case OPAQUE:
                value = createDefaultOpaqueValue(objectModel, resourceModel);
                break;
            default:
                // this should not happened
                value = null;
                break;
            }
            if (value != null)
                return new LwM2mResource(resourceModel.id, value);
        } else {
            Value<?>[] values;
            switch (resourceModel.type) {
            case STRING:
                values = new Value[] { createDefaultStringValue(objectModel, resourceModel) };
                break;
            case BOOLEAN:
                values = new Value[] { createDefaultBooleanValue(objectModel, resourceModel),
                                        createDefaultBooleanValue(objectModel, resourceModel) };
                break;
            case INTEGER:
                values = new Value[] { createDefaultIntegerValue(objectModel, resourceModel),
                                        createDefaultIntegerValue(objectModel, resourceModel) };
                break;
            case FLOAT:
                values = new Value[] { createDefaultFloatValue(objectModel, resourceModel),
                                        createDefaultFloatValue(objectModel, resourceModel) };
                break;
            case TIME:
                values = new Value[] { createDefaultDateValue(objectModel, resourceModel) };
                break;
            case OPAQUE:
                values = new Value[] { createDefaultOpaqueValue(objectModel, resourceModel) };
                break;
            default:
                // this should not happened
                values = null;
                break;
            }
            if (values != null)
                return new LwM2mResource(resourceModel.id, values);
        }
        return null;
    }

    protected Value<String> createDefaultStringValue(ObjectModel objectModel, ResourceModel resourceModel) {
        return Value.newStringValue(resourceModel.name);
    }

    protected Value<Integer> createDefaultIntegerValue(ObjectModel objectModel, ResourceModel resourceModel) {
        return Value.newIntegerValue((int) (Math.random() * 100 % 101));
    }

    protected Value<Boolean> createDefaultBooleanValue(ObjectModel objectModel, ResourceModel resourceModel) {
        return Value.newBooleanValue(Math.random() * 100 % 2 == 0);
    }

    protected Value<?> createDefaultDateValue(ObjectModel objectModel, ResourceModel resourceModel) {
        return Value.newDateValue(new Date());
    }

    protected Value<?> createDefaultFloatValue(ObjectModel objectModel, ResourceModel resourceModel) {
        return Value.newFloatValue((float) Math.random() * 100);
    }

    protected Value<?> createDefaultOpaqueValue(ObjectModel objectModel, ResourceModel resourceModel) {
        return Value.newBinaryValue(new String("Default " + resourceModel.name).getBytes());
    }
}
