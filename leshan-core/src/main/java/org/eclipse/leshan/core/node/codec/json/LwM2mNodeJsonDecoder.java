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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.Value;
import org.eclipse.leshan.core.node.codec.InvalidValueException;
import org.eclipse.leshan.json.JsonArrayEntry;
import org.eclipse.leshan.json.JsonRootObject;
import org.eclipse.leshan.json.LwM2mJson;
import org.eclipse.leshan.json.LwM2mJsonException;
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
            LwM2mResource[] resources = new LwM2mResource[resourceMap.size()];
            int k = 0;
            for (Entry<Integer, LwM2mResource> entry : resourceMap.entrySet()) {
                LwM2mResource resource = entry.getValue();
                resources[k] = resource;
                k++;
            }
            return new LwM2mObjectInstance(path.getObjectInstanceId(), resources);

        } else {
            // resource level request
            Map<Integer, LwM2mResource> resourceMap = parseJsonPayLoadLwM2mResources(jsonObject, path, model);
            return resourceMap.values().iterator().next();
        }
    }

    private static Map<Integer, LwM2mResource> parseJsonPayLoadLwM2mResources(JsonRootObject jsonObject,
            LwM2mPath path, LwM2mModel model) throws InvalidValueException {
        Map<Integer, LwM2mResource> lwM2mResourceMap = new HashMap<>();
        Map<Integer, List<Object>> multiResourceMap = new HashMap<>();

        if (jsonObject.getBaseName() != null && !jsonObject.getBaseName().isEmpty()) {
            throw new UnsupportedOperationException("Basename support is not implemented.");
        }

        for (int i = 0; i < jsonObject.getResourceList().size(); i++) {
            JsonArrayEntry resourceElt = jsonObject.getResourceList().get(i);
            String[] resourcePath = resourceElt.getName().split("/");
            Integer resourceId = Integer.valueOf(resourcePath[0]);

            if (!multiResourceMap.isEmpty() && multiResourceMap.get(resourceId) != null) {
                multiResourceMap.get(resourceId).add(resourceElt.getResourceValue());
                continue;
            }
            if (resourcePath.length > 1) {
                // multi resource
                // store multi resource values in a map
                List<Object> list = new ArrayList<>();
                list.add(resourceElt.getResourceValue());
                multiResourceMap.put(resourceId, list);
            } else {
                // single resource
                LwM2mPath rscPath = new LwM2mPath(path.getObjectId(), path.getObjectInstanceId(), resourceId);
                LwM2mResource res = new LwM2mResource(resourceId, parseJsonValue(resourceElt.getResourceValue(),
                        rscPath, model));
                lwM2mResourceMap.put(resourceId, res);
            }
        }

        for (Map.Entry<Integer, List<Object>> entry : multiResourceMap.entrySet()) {
            Integer key = entry.getKey();
            List<Object> valueList = entry.getValue();

            if (valueList != null && !valueList.isEmpty()) {
                Value<?>[] values = new Value[valueList.size()];
                for (int j = 0; j < valueList.size(); j++) {
                    LwM2mPath rscPath = new LwM2mPath(path.getObjectId(), path.getObjectInstanceId(), key);
                    values[j] = parseJsonValue(valueList.get(j), rscPath, model);
                }
                LwM2mResource res = new LwM2mResource(key, values);
                lwM2mResourceMap.put(key, res);
            }
        }
        return lwM2mResourceMap;
    }

    private static Value<?> parseJsonValue(Object value, LwM2mPath rscPath, LwM2mModel model)
            throws InvalidValueException {

        ResourceModel rscDesc = model.getResourceModel(rscPath.getObjectId(), rscPath.getResourceId());

        LOG.trace("JSON value for path {} and expected type {}: {}", rscPath, rscDesc.type, value);

        try {
            switch (rscDesc.type) {
            case INTEGER:
                // JSON format specs said v = integer or float
                return Value.newIntegerValue(((Number) value).intValue());
            case BOOLEAN:
                return Value.newBooleanValue((Boolean) value);
            case FLOAT:
                // JSON format specs said v = integer or float
                return Value.newFloatValue(((Number) value).floatValue());
            case TIME:
                // TODO Specs page 44, Resource 13 (current time) of device object represented as Float value
                return Value.newDateValue(new Date(((Number) value).longValue() * 1000L));
            case OPAQUE:
                // If the Resource data type is opaque the string value
                // holds the Base64 encoded representation of the Resource
                return Value.newBinaryValue(javax.xml.bind.DatatypeConverter.parseHexBinary((String) value));
            default:
                // Default is Strung
                return Value.newStringValue((String) value);

            }
        } catch (Exception e) {
            throw new InvalidValueException("Invalid content for type " + rscDesc.type, rscPath, e);
        }
    }
}
