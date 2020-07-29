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
 *******************************************************************************/
package org.eclipse.leshan.core.node.codec.json;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.leshan.core.json.JsonArrayEntry;
import org.eclipse.leshan.core.json.JsonRootObject;
import org.eclipse.leshan.core.json.LwM2mJson;
import org.eclipse.leshan.core.json.LwM2mJsonException;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mNodeVisitor;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mValueConverter;
import org.eclipse.leshan.core.util.Base64;
import org.eclipse.leshan.core.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeJsonEncoder {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeJsonEncoder.class);

    public static byte[] encode(LwM2mNode node, LwM2mPath path, LwM2mModel model, LwM2mValueConverter converter)
            throws CodecException {
        Validate.notNull(node);
        Validate.notNull(path);
        Validate.notNull(model);

        InternalEncoder internalEncoder = new InternalEncoder();
        internalEncoder.objectId = path.getObjectId();
        internalEncoder.model = model;
        internalEncoder.requestPath = path;
        internalEncoder.converter = converter;
        node.accept(internalEncoder);
        JsonRootObject jsonObject = new JsonRootObject();
        jsonObject.setResourceList(internalEncoder.resourceList);
        jsonObject.setBaseName(internalEncoder.baseName);
        try {
            return LwM2mJson.toJsonLwM2m(jsonObject).getBytes();
        } catch (LwM2mJsonException e) {
            throw new CodecException(e, "Unable to encode node[path:%s] : %s", path, node);
        }
    }

    public static byte[] encodeTimestampedData(List<TimestampedLwM2mNode> timestampedNodes, LwM2mPath path,
            LwM2mModel model, LwM2mValueConverter converter) {
        Validate.notNull(timestampedNodes);
        Validate.notNull(path);
        Validate.notNull(model);

        InternalEncoder internalEncoder = new InternalEncoder();
        ArrayList<JsonArrayEntry> entries = new ArrayList<>();
        String baseName = null;
        for (TimestampedLwM2mNode timestampedLwM2mNode : timestampedNodes) {
            internalEncoder.objectId = path.getObjectId();
            internalEncoder.model = model;
            internalEncoder.requestPath = path;
            internalEncoder.converter = converter;
            internalEncoder.resourceList = null;
            internalEncoder.timestamp = timestampedLwM2mNode.getTimestamp();
            timestampedLwM2mNode.getNode().accept(internalEncoder);
            entries.addAll(internalEncoder.resourceList);
            if (baseName != null) {
                if (!baseName.equals(internalEncoder.baseName)) {
                    throw new CodecException(
                            "Unexpected baseName %s (%s expected) when encoding timestamped nodes for request %s",
                            internalEncoder.baseName, baseName, path);
                }
            } else {
                baseName = internalEncoder.baseName;
            }
        }
        JsonRootObject jsonObject = new JsonRootObject();
        jsonObject.setResourceList(entries);
        jsonObject.setBaseName(internalEncoder.baseName);
        try {
            return LwM2mJson.toJsonLwM2m(jsonObject).getBytes();
        } catch (LwM2mJsonException e) {
            throw new CodecException(e, "Unable to encode timestamped nodes[path:%s] : %s", path, timestampedNodes);
        }

    }

    private static class InternalEncoder implements LwM2mNodeVisitor {
        // visitor inputs
        private int objectId;
        private LwM2mModel model;
        private LwM2mPath requestPath;
        private Long timestamp;
        private LwM2mValueConverter converter;

        // visitor output
        private ArrayList<JsonArrayEntry> resourceList = null;
        private String baseName = null;

        @Override
        public void visit(LwM2mObject object) {
            LOG.trace("Encoding Object {} into JSON", object);
            // Validate request path
            if (!requestPath.isObject()) {
                throw new CodecException("Invalid request path %s for JSON object encoding", requestPath);
            }
            baseName = requestPath.toString() + "/";

            // Create resources
            resourceList = new ArrayList<>();
            for (LwM2mObjectInstance instance : object.getInstances().values()) {
                for (LwM2mResource resource : instance.getResources().values()) {
                    String prefixPath = Integer.toString(instance.getId()) + "/" + Integer.toString(resource.getId());
                    resourceList.addAll(lwM2mResourceToJsonArrayEntry(prefixPath, timestamp, resource));
                }
            }
        }

        @Override
        public void visit(LwM2mObjectInstance instance) {
            LOG.trace("Encoding object instance {} into JSON", instance);
            resourceList = new ArrayList<>();
            if (instance.getId() == LwM2mObjectInstance.UNDEFINED) {
                throw new CodecException("Unable to use JSON format without to give the object instance Id");
            }
            for (LwM2mResource resource : instance.getResources().values()) {
                // Validate request path & compute resource path
                String prefixPath;
                if (requestPath.isObject()) {
                    prefixPath = instance.getId() + "/" + resource.getId();
                } else if (requestPath.isObjectInstance()) {
                    prefixPath = Integer.toString(resource.getId());
                } else {
                    throw new CodecException("Invalid request path %s for JSON instance encoding", requestPath);
                }
                baseName = requestPath + "/";
                // Create resources
                resourceList.addAll(lwM2mResourceToJsonArrayEntry(prefixPath, timestamp, resource));
            }
        }

        @Override
        public void visit(LwM2mResource resource) {
            LOG.trace("Encoding resource {} into JSON", resource);
            if (!requestPath.isResource()) {
                throw new CodecException("Invalid request path %s for JSON resource encoding", requestPath);
            }
            if (resource.isMultiInstances()) {
                baseName = requestPath.toString() + "/";
            } else {
                baseName = requestPath.toString();
            }
            resourceList = lwM2mResourceToJsonArrayEntry(null, timestamp, resource);
        }

        private ArrayList<JsonArrayEntry> lwM2mResourceToJsonArrayEntry(String resourcePath, Long timestamp,
                LwM2mResource resource) {
            // get type for this resource
            ResourceModel rSpec = model.getResourceModel(objectId, resource.getId());
            Type expectedType = rSpec != null ? rSpec.type : resource.getType();
            ArrayList<JsonArrayEntry> resourcesList = new ArrayList<>();

            // create JSON resource element
            if (resource.isMultiInstances()) {
                for (Entry<Integer, ?> entry : resource.getValues().entrySet()) {
                    // compute resource instance path
                    String resourceInstancePath;
                    if (resourcePath == null || resourcePath.isEmpty()) {
                        resourceInstancePath = Integer.toString(entry.getKey());
                    } else {
                        resourceInstancePath = resourcePath + "/" + entry.getKey();
                    }

                    // Create resource element
                    JsonArrayEntry jsonResourceElt = new JsonArrayEntry();
                    jsonResourceElt.setName(resourceInstancePath);
                    jsonResourceElt.setTime(timestamp);

                    // Convert value using expected type
                    LwM2mPath lwM2mResourceInstancePath = new LwM2mPath(resourceInstancePath);
                    Object convertedValue = converter.convertValue(entry.getValue(), resource.getType(), expectedType,
                            lwM2mResourceInstancePath);
                    this.setResourceValue(convertedValue, expectedType, jsonResourceElt, lwM2mResourceInstancePath);

                    // Add it to the List
                    resourcesList.add(jsonResourceElt);
                }
            } else {
                // Create resource element
                JsonArrayEntry jsonResourceElt = new JsonArrayEntry();
                jsonResourceElt.setName(resourcePath);
                jsonResourceElt.setTime(timestamp);

                // Convert value using expected type
                LwM2mPath lwM2mResourcePath = resourcePath != null ? new LwM2mPath(resourcePath) : null;
                this.setResourceValue(converter.convertValue(resource.getValue(), resource.getType(), expectedType,
                        lwM2mResourcePath), expectedType, jsonResourceElt, lwM2mResourcePath);

                // Add it to the List
                resourcesList.add(jsonResourceElt);
            }
            return resourcesList;
        }

        private void setResourceValue(Object value, Type type, JsonArrayEntry jsonResource, LwM2mPath resourcePath) {
            LOG.trace("Encoding value {} in JSON", value);

            if (type == null || type == Type.NONE) {
                throw new CodecException(
                        "Unable to encode value for resource {} without type(probably a executable one)", resourcePath);
            }

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
                break;
            default:
                throw new CodecException("Invalid value type %s for %s", type, resourcePath);
            }
        }
    }
}