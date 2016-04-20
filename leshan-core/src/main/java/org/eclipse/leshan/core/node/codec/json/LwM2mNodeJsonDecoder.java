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
import org.eclipse.leshan.core.node.LwM2mObject;
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

    public static <T extends LwM2mNode> T decode(byte[] content, LwM2mPath path, LwM2mModel model, Class<T> nodeClass)
            throws InvalidValueException {
        try {
            String jsonStrValue = new String(content);
            JsonRootObject json = LwM2mJson.fromJsonLwM2m(jsonStrValue);
            return parseJSON(json, path, model, nodeClass);
        } catch (LwM2mJsonException e) {
            throw new InvalidValueException("Unable to deSerialize json", path, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends LwM2mNode> T parseJSON(JsonRootObject jsonObject, LwM2mPath path, LwM2mModel model,
            Class<T> nodeClass) throws InvalidValueException {
        LOG.trace("Parsing JSON content for path {}: {}", path, jsonObject);

        if (nodeClass == LwM2mObject.class) {
            // TODO
            // If bn is present will have multiple object instances in JSON payload
            // If JSON contains ObjLnk -> this method should return List<LwM2mNode> ?
            throw new UnsupportedOperationException("JSON object level decoding is not implemented");
        } else if (nodeClass == LwM2mObjectInstance.class) {
            // object instance level request,
            LwM2mPath baseName = extractAndValidateBaseName(jsonObject, path);
            Map<Integer, LwM2mResource> resourceMap = parseJsonPayLoadLwM2mResources(jsonObject, path, baseName, model);
            // try to find instance Id
            int objectInstanceID = LwM2mObjectInstance.UNDEFINED;
            if (path.getObjectInstanceId() != null) {
                objectInstanceID = path.getObjectInstanceId();
            } else if (baseName != null && baseName.getObjectInstanceId() != null) {
                objectInstanceID = baseName.getObjectInstanceId();
            }
            return (T) new LwM2mObjectInstance(objectInstanceID, resourceMap.values());
        } else if (nodeClass == LwM2mResource.class) {
            // resource level request
            LwM2mPath baseName = extractAndValidateBaseName(jsonObject, path);
            Map<Integer, LwM2mResource> resourceMap = parseJsonPayLoadLwM2mResources(jsonObject, path, baseName, model);
            return (T) resourceMap.values().iterator().next();
        } else {
            throw new IllegalArgumentException("invalid node class: " + nodeClass);
        }
    }

    private static LwM2mPath extractAndValidateBaseName(JsonRootObject jsonObject, LwM2mPath requestPath)
            throws InvalidValueException {
        // Check baseName is valid
        if (jsonObject.getBaseName() != null && !jsonObject.getBaseName().isEmpty()) {
            LwM2mPath bnPath = new LwM2mPath(jsonObject.getBaseName());

            // check returned base name path is under requested path
            if (requestPath.getObjectId() != null && bnPath.getObjectId() != null) {
                if (!bnPath.getObjectId().equals(requestPath.getObjectId())) {
                    throw new InvalidValueException("Basename path does not match requested path.", bnPath);
                }
                if (requestPath.getObjectInstanceId() != null && bnPath.getObjectInstanceId() != null) {
                    if (!bnPath.getObjectInstanceId().equals(requestPath.getObjectInstanceId())) {
                        throw new InvalidValueException("Basename path does not match requested path.", bnPath);
                    }
                    if (requestPath.getResourceId() != null && bnPath.getResourceId() != null) {
                        if (!bnPath.getResourceId().equals(requestPath.getResourceId())) {
                            throw new InvalidValueException("Basename path does not match requested path.", bnPath);
                        }
                    }
                }
            }
            return bnPath;
        }
        return null;

    }

    private static Map<Integer, LwM2mResource> parseJsonPayLoadLwM2mResources(JsonRootObject jsonObject,
            LwM2mPath requestPath, LwM2mPath baseName, LwM2mModel model) throws InvalidValueException {

        // Extract LWM2M resources from JSON resource list
        Map<Integer, LwM2mResource> lwM2mResourceMap = new HashMap<>();
        Map<LwM2mPath, Map<Integer, JsonArrayEntry>> multiResourceMap = new HashMap<>();
        for (int i = 0; i < jsonObject.getResourceList().size(); i++) {
            JsonArrayEntry resourceElt = jsonObject.getResourceList().get(i);

            // Build resource path
            LwM2mPath nodePath;
            if (baseName != null) {
                nodePath = baseName.append(resourceElt.getName());
            } else {
                // we don't have a baseName so use request path
                nodePath = requestPath.append(resourceElt.getName());
            }

            // handle LWM2M resources
            if (nodePath.isResourceInstance()) {
                // Multi-instance resource
                // Store multi-instance resource values in a map
                // we will deal with it later
                LwM2mPath resourcePath = new LwM2mPath(nodePath.getObjectId(), nodePath.getObjectInstanceId(),
                        nodePath.getResourceId());
                Map<Integer, JsonArrayEntry> multiResource = multiResourceMap.get(resourcePath);
                if (multiResource == null) {
                    multiResource = new HashMap<>();
                    multiResourceMap.put(resourcePath, multiResource);
                }
                multiResource.put(nodePath.getResourceInstanceId(), resourceElt);
            } else if (nodePath.isResource()) {
                // Single resource
                Type expectedType = getResourceType(nodePath, model, resourceElt);
                LwM2mResource res = LwM2mSingleResource.newResource(nodePath.getResourceId(),
                        parseJsonValue(resourceElt.getResourceValue(), expectedType, nodePath), expectedType);
                lwM2mResourceMap.put(nodePath.getResourceId(), res);
            } else {
                throw new InvalidValueException(
                        "Invalid path for resource, it should be a resource or a resource instance path", nodePath);
            }
        }

        // Handle multi-instance resource.
        for (Map.Entry<LwM2mPath, Map<Integer, JsonArrayEntry>> entry : multiResourceMap.entrySet()) {
            LwM2mPath resourcePath = entry.getKey();
            Map<Integer, JsonArrayEntry> jsonEntries = entry.getValue();

            if (jsonEntries != null && !jsonEntries.isEmpty()) {
                Type expectedType = getResourceType(resourcePath, model, jsonEntries.values().iterator().next());
                Map<Integer, Object> values = new HashMap<>();
                for (Entry<Integer, JsonArrayEntry> e : jsonEntries.entrySet()) {
                    Integer resourceInstanceId = e.getKey();
                    values.put(resourceInstanceId,
                            parseJsonValue(e.getValue().getResourceValue(), expectedType, resourcePath));
                }
                LwM2mResource resource = LwM2mMultipleResource.newResource(resourcePath.getResourceId(), values,
                        expectedType);
                lwM2mResourceMap.put(resourcePath.getResourceId(), resource);
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
