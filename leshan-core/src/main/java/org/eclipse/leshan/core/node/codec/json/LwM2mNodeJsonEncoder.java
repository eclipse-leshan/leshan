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
package org.eclipse.leshan.core.node.codec.json;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map.Entry;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mNodeVisitor;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.codec.Lwm2mNodeEncoderUtil;
import org.eclipse.leshan.json.JsonArrayEntry;
import org.eclipse.leshan.json.JsonRootObject;
import org.eclipse.leshan.json.LwM2mJson;
import org.eclipse.leshan.util.Base64;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeJsonEncoder {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeJsonEncoder.class);

    public static byte[] encode(LwM2mNode node, LwM2mPath path, LwM2mModel model) {
        Validate.notNull(node);
        Validate.notNull(path);
        Validate.notNull(model);

        InternalEncoder internalEncoder = new InternalEncoder();
        internalEncoder.objectId = path.getObjectId();
        internalEncoder.model = model;
        node.accept(internalEncoder);
        return internalEncoder.encoded;
    }

    private static class InternalEncoder implements LwM2mNodeVisitor {
        // visitor inputs
        private int objectId;
        private LwM2mModel model;

        // visitor output
        private byte[] encoded = null;

        @Override
        public void visit(LwM2mObject object) {
            // TODO needed to encode multiple instances
            // How to deal with ObjLink
            throw new UnsupportedOperationException("Object JSON encoding not supported");
        }

        @Override
        public void visit(LwM2mObjectInstance instance) {
            LOG.trace("Encoding object instance {} into JSON", instance);
            JsonRootObject jsonObject = null;
            ArrayList<JsonArrayEntry> resourceList = new ArrayList<>();
            for (Entry<Integer, LwM2mResource> resource : instance.getResources().entrySet()) {
                LwM2mResource res = resource.getValue();
                resourceList.addAll(lwM2mResourceToJsonArrayEntry(res));
            }
            jsonObject = new JsonRootObject(resourceList);
            String json = LwM2mJson.toJsonLwM2m(jsonObject);
            encoded = json.getBytes();
        }

        @Override
        public void visit(LwM2mResource resource) {
            LOG.trace("Encoding resource {} into JSON", resource);
            JsonRootObject jsonObject = null;
            jsonObject = new JsonRootObject(lwM2mResourceToJsonArrayEntry(resource));
            String json = LwM2mJson.toJsonLwM2m(jsonObject);
            encoded = json.getBytes();
        }

        private ArrayList<JsonArrayEntry> lwM2mResourceToJsonArrayEntry(LwM2mResource resource) {
            ResourceModel rSpec = model.getResourceModel(objectId, resource.getId());
            Type expectedType = rSpec != null ? rSpec.type : resource.getType();
            ArrayList<JsonArrayEntry> resourcesList = new ArrayList<>();
            if (resource.isMultiInstances()) {
                for (Entry<Integer, ?> entry : resource.getValues().entrySet()) {
                    JsonArrayEntry jsonResourceElt = new JsonArrayEntry();
                    jsonResourceElt.setName(resource.getId() + "/" + entry.getKey());
                    Object convertedValue = Lwm2mNodeEncoderUtil.convertValue(entry.getValue(), resource.getType(),
                            expectedType);
                    this.setResourceValue(convertedValue, expectedType, jsonResourceElt);
                    resourcesList.add(jsonResourceElt);
                }
            } else {
                JsonArrayEntry jsonResourceElt = new JsonArrayEntry();
                jsonResourceElt.setName(new StringBuffer().append(resource.getId()).toString());
                this.setResourceValue(
                        Lwm2mNodeEncoderUtil.convertValue(resource.getValue(), resource.getType(), expectedType),
                        expectedType, jsonResourceElt);
                resourcesList.add(jsonResourceElt);
            }
            return resourcesList;
        }

        private void setResourceValue(Object value, Type type, JsonArrayEntry jsonResource) {
            LOG.trace("Encoding value {} in JSON", value);
            // Following table 20 in the Specs
            switch (type) {
            case STRING:
                jsonResource.setStringValue((String) value);
                break;
            case INTEGER:
            case FLOAT:
                jsonResource.setFloatValue((Number) value);
                break;
            case BOOLEAN:
                jsonResource.setBooleanValue((Boolean) value);
                break;
            case TIME:
                // Specs device object example page 44, rec 13 is Time
                // represented as float?
                jsonResource.setFloatValue((((Date) value).getTime() / 1000L));
                break;
            case OPAQUE:
                jsonResource.setStringValue(Base64.encodeBase64String((byte[]) value));
            default:
                throw new IllegalArgumentException("Invalid value type: " + type);
            }
        }
    }
}