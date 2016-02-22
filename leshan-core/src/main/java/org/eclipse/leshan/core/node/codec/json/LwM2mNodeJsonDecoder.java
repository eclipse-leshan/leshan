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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.InvalidValueException;
import org.eclipse.leshan.json.JsonArrayEntry;
import org.eclipse.leshan.json.JsonRootObject;
import org.eclipse.leshan.json.LwM2mJson;
import org.eclipse.leshan.json.LwM2mJsonException;
import org.eclipse.leshan.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeJsonDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeJsonDecoder.class);

    public static LwM2mNode decode(byte[] content, LwM2mPath path, LwM2mModel model) throws InvalidValueException {
        try {
            String jsonStrValue = new String(content);
            JsonRootObject json = LwM2mJson.fromJsonLwM2m(jsonStrValue);
            return parseJSON(json, path, model);
        } catch (LwM2mJsonException e) {
            throw new InvalidValueException("Unable to deSerialize json", path, e);
        }
    }

    private static LwM2mNode parseJSON(JsonRootObject jsonObject, LwM2mPath path, LwM2mModel model)
            throws InvalidValueException {
        LOG.trace("Parsing JSON content for path {}: {}", path, jsonObject);

        if (path.isObject()) {
            // TODO
            // If bn is present will have multiple object instances in JSON payload
            // If JSON contains ObjLnk -> this method should return List<LwM2mNode> ?
            throw new UnsupportedOperationException("JSON object level decoding is not implemented");
        } else if (path.isObjectInstance()) {
            // object instance level request
            Map<Integer, LwM2mResource> resourceMap = parseJsonPayLoadLwM2mResources(jsonObject, path, model);
            return new LwM2mObjectInstance(path.getObjectInstanceId(), resourceMap.values());
        } else {
            // resource level request
            Map<Integer, LwM2mResource> resourceMap = parseJsonPayLoadLwM2mResources(jsonObject, path, model);
            return resourceMap.values().iterator().next();
        }
    }

    private static Map<Integer, LwM2mResource> parseJsonPayLoadLwM2mResources(JsonRootObject jsonObject,
            LwM2mPath path, LwM2mModel model) throws InvalidValueException {
        Map<Integer, LwM2mResource> lwM2mResourceMap = new HashMap<>();
        Map<Integer, Map<Integer, JsonArrayEntry>> multiResourceMap = new HashMap<>();

        // we does not support base name for now
        if (jsonObject.getBaseName() != null && !jsonObject.getBaseName().isEmpty()) {
            throw new UnsupportedOperationException("Basename support is not implemented.");
        }

        for (int i = 0; i < jsonObject.getResourceList().size(); i++) {
            JsonArrayEntry resourceElt = jsonObject.getResourceList().get(i);
            String[] resourcePath = resourceElt.getName().split("/");
            Integer resourceId = Integer.valueOf(resourcePath[0]);

            Map<Integer, JsonArrayEntry> multiResource = multiResourceMap.get(resourceId);
            if (multiResource != null) {
                multiResource.put(Integer.valueOf(resourcePath[1]), resourceElt);
                continue;
            }
            if (resourcePath.length > 1) {
                // multi resource
                // store multi resource values in a map
                Map<Integer, JsonArrayEntry> jsonEntries = new HashMap<>();
                jsonEntries.put(Integer.valueOf(resourcePath[1]), resourceElt);
                multiResourceMap.put(resourceId, jsonEntries);
            } else {
                // single resource
                LwM2mPath rscPath = new LwM2mPath(path.getObjectId(), path.getObjectInstanceId(), resourceId);
                Type expectedType = getResourceType(rscPath, model, resourceElt);
                LwM2mResource res = LwM2mSingleResource.newResource(resourceId,
                        parseJsonValue(resourceElt.getResourceValue(), expectedType, rscPath), expectedType);
                lwM2mResourceMap.put(resourceId, res);
            }
        }

        for (Map.Entry<Integer, Map<Integer, JsonArrayEntry>> entry : multiResourceMap.entrySet()) {
            Integer key = entry.getKey();
            Map<Integer, JsonArrayEntry> jsonEntries = entry.getValue();

            if (jsonEntries != null && !jsonEntries.isEmpty()) {
                LwM2mPath rscPath = new LwM2mPath(path.getObjectId(), path.getObjectInstanceId(), key);
                Type expectedType = getResourceType(rscPath, model, jsonEntries.values().iterator().next());
                Map<Integer, Object> values = new HashMap<>();
                for (Entry<Integer, JsonArrayEntry> e : jsonEntries.entrySet()) {

                    values.put(e.getKey(), parseJsonValue(e.getValue().getResourceValue(), expectedType, rscPath));
                }
                LwM2mResource res = LwM2mMultipleResource.newResource(key, values, expectedType);
                lwM2mResourceMap.put(key, res);
            }
        }
        return lwM2mResourceMap;
    }

    private static Object parseJsonValue(Object value, Type expectedType, LwM2mPath path) throws InvalidValueException {

        LOG.trace("JSON value for path {} and expected type {}: {}", path, expectedType, value);

        try {
            switch (expectedType) {
            case INTEGER:
                // JSON format specs said v = integer or float
                return ((Number) value).longValue();
            case BOOLEAN:
                return value;
            case FLOAT:
                // JSON format specs said v = integer or float
                return ((Number) value).doubleValue();
            case TIME:
                // TODO Specs page 44, Resource 13 (current time) of device object represented as Float value
                return new Date(((Number) value).longValue() * 1000L);
            case OPAQUE:
                // If the Resource data type is opaque the string value
                // holds the Base64 encoded representation of the Resource
                return Base64.decodeBase64((String) value);
            case STRING:
                return value;
            default:
                throw new InvalidValueException("Unsupported type " + expectedType, path);
            }
        } catch (Exception e) {
            throw new InvalidValueException("Invalid content for type " + expectedType, path, e);
        }
    }

    public static Type getResourceType(LwM2mPath rscPath, LwM2mModel model, JsonArrayEntry resourceElt)
            throws InvalidValueException {
        ResourceModel rscDesc = model.getResourceModel(rscPath.getObjectId(), rscPath.getResourceId());
        if (rscDesc == null || rscDesc.type == null) {
            Type type = resourceElt.getType();
            if (type != null)
                return type;

            LOG.trace("unknown type for resource use string as default: {}", rscPath);
            return Type.STRING;
        } else {
            return rscDesc.type;
        }
    }
}
