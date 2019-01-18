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

import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyInstanceEnabler extends SimpleInstanceEnabler {

    private static Logger LOG = LoggerFactory.getLogger(DummyInstanceEnabler.class);

    public DummyInstanceEnabler() {
    }

    public DummyInstanceEnabler(int id) {
        super(id);
    }

    @Override
    public ExecuteResponse execute(ServerIdentity identity, int resourceid, String params) {
        if (getModel().resources.containsKey(resourceid)) {
            LOG.info("Executing resource [{}] with params [{}]", resourceid, params);
            return ExecuteResponse.success();
        } else {
            return ExecuteResponse.notFound();
        }
    }

    @Override
    public void onDelete(ServerIdentity identity) {
        LOG.info("Instance {} from object {} ({}) deleted.", getId(), getModel().name, getModel().id);
    }

    @Override
    protected LwM2mMultipleResource initializeMultipleResource(ObjectModel objectModel, ResourceModel resourceModel) {
        Map<Integer, Object> values = new HashMap<>();
        switch (resourceModel.type) {
        case STRING:
            values.put(0, createDefaultStringValueFor(objectModel, resourceModel));
            break;
        case BOOLEAN:
            values.put(0, createDefaultBooleanValueFor(objectModel, resourceModel));
            values.put(1, createDefaultBooleanValueFor(objectModel, resourceModel));
            break;
        case INTEGER:
            values.put(0, createDefaultIntegerValueFor(objectModel, resourceModel));
            values.put(1, createDefaultIntegerValueFor(objectModel, resourceModel));
            break;
        case FLOAT:
            values.put(0, createDefaultFloatValueFor(objectModel, resourceModel));
            values.put(1, createDefaultFloatValueFor(objectModel, resourceModel));
            break;
        case TIME:
            values.put(0, createDefaultDateValueFor(objectModel, resourceModel));
            break;
        case OPAQUE:
            values.put(0, createDefaultOpaqueValueFor(objectModel, resourceModel));
            break;
        default:
            // this should not happened
            values = null;
            break;
        }
        if (values != null)
            return LwM2mMultipleResource.newResource(resourceModel.id, values, resourceModel.type);
        else
            return null;
    }

    @Override
    protected String createDefaultStringValueFor(ObjectModel objectModel, ResourceModel resourceModel) {
        return resourceModel.name;
    }

    @Override
    protected long createDefaultIntegerValueFor(ObjectModel objectModel, ResourceModel resourceModel) {
        return (long) (Math.random() * 100 % 101);
    }

    @Override
    protected boolean createDefaultBooleanValueFor(ObjectModel objectModel, ResourceModel resourceModel) {
        return Math.random() * 100 % 2 == 0;
    }

    @Override
    protected Date createDefaultDateValueFor(ObjectModel objectModel, ResourceModel resourceModel) {
        return new Date();
    }

    @Override
    protected double createDefaultFloatValueFor(ObjectModel objectModel, ResourceModel resourceModel) {
        return Math.random() * 100;
    }

    @Override
    protected byte[] createDefaultOpaqueValueFor(ObjectModel objectModel, ResourceModel resourceModel) {
        return ("Default " + resourceModel.name).getBytes();
    }
}
