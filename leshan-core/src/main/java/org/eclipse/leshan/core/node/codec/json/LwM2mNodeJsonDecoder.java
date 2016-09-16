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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

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
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
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

    @SuppressWarnings("unchecked")
    public static <T extends LwM2mNode> T decode(byte[] content, LwM2mPath path, LwM2mModel model, Class<T> nodeClass)
            throws InvalidValueException {
        try {
            String jsonStrValue = content != null ? new String(content) : "";
            JsonRootObject json = LwM2mJson.fromJsonLwM2m(jsonStrValue);
            List<TimestampedLwM2mNode> timestampedNodes = parseJSON(json, path, model, nodeClass);
            if (timestampedNodes.size() == 0) {
                return null;
            } else {
                // return the most recent value
                return (T) timestampedNodes.get(0).getNode();
            }
        } catch (LwM2mJsonException e) {
            throw new InvalidValueException("Unable to deSerialize json", path, e);
        }
    }

    public static List<TimestampedLwM2mNode> decodeTimestamped(byte[] content, LwM2mPath path, LwM2mModel model,
            Class<? extends LwM2mNode> nodeClass) throws InvalidValueException {
        try {
            String jsonStrValue = new String(content);
            JsonRootObject json = LwM2mJson.fromJsonLwM2m(jsonStrValue);
            return parseJSON(json, path, model, nodeClass);
        } catch (LwM2mJsonException e) {
            throw new InvalidValueException("Unable to deSerialize json", path, e);
        }
    }

    private static List<TimestampedLwM2mNode> parseJSON(JsonRootObject jsonObject, LwM2mPath path, LwM2mModel model,
            Class<? extends LwM2mNode> nodeClass) throws InvalidValueException {

        LOG.trace("Parsing JSON content for path {}: {}", path, jsonObject);

        // Group JSON entry by time-stamp
        Map<Long, Collection<JsonArrayEntry>> jsonEntryByTimestamp = groupJsonEntryByTimestamp(jsonObject);

        // Extract baseName
        LwM2mPath baseName = extractAndValidateBaseName(jsonObject, path);
        if (baseName == null)
            baseName = path; // if no base name, use request path as base name

        // fill time-stamped nodes collection
        List<TimestampedLwM2mNode> timestampedNodes = new ArrayList<>();
        for (Entry<Long, Collection<JsonArrayEntry>> entryByTimestamp : jsonEntryByTimestamp.entrySet()) {

            // Group JSON entry by instance
            Map<Integer, Collection<JsonArrayEntry>> jsonEntryByInstanceId = groupJsonEntryByInstanceId(
                    entryByTimestamp.getValue(), baseName);

            // Create lwm2m node
            LwM2mNode node = null;
            if (nodeClass == LwM2mObject.class) {
                Collection<LwM2mObjectInstance> instances = new ArrayList<>();
                for (Entry<Integer, Collection<JsonArrayEntry>> entryByInstanceId : jsonEntryByInstanceId.entrySet()) {
                    Map<Integer, LwM2mResource> resourcesMap = extractLwM2mResources(entryByInstanceId.getValue(),
                            baseName, model);

                    instances.add(new LwM2mObjectInstance(entryByInstanceId.getKey(), resourcesMap.values()));
                }

                node = new LwM2mObject(baseName.getObjectId(), instances);
            } else if (nodeClass == LwM2mObjectInstance.class) {
                // validate we have resources for only 1 instance
                if (jsonEntryByInstanceId.size() > 1)
                    throw new InvalidValueException("Only one instance expected in the payload", path);

                // Extract resources
                Entry<Integer, Collection<JsonArrayEntry>> instanceEntry = jsonEntryByInstanceId.entrySet().iterator()
                        .next();
                Map<Integer, LwM2mResource> resourcesMap = extractLwM2mResources(instanceEntry.getValue(), baseName,
                        model);

                // Create instance
                node = new LwM2mObjectInstance(instanceEntry.getKey(), resourcesMap.values());
            } else if (nodeClass == LwM2mResource.class) {
                // validate we have resources for only 1 instance
                if (jsonEntryByInstanceId.size() > 1)
                    throw new InvalidValueException("Only one instance expected in the payload", path);

                // Extract resources
                Map<Integer, LwM2mResource> resourcesMap = extractLwM2mResources(
                        jsonEntryByInstanceId.values().iterator().next(), baseName, model);

                // validate there is only 1 resource
                if (resourcesMap.size() != 1)
                    throw new InvalidValueException("Only one resource should be present in the payload", path);

                node = resourcesMap.values().iterator().next();
            } else {
                throw new IllegalArgumentException("invalid node class: " + nodeClass);
            }

            // compute time-stamp
            Long timestamp = computeTimestamp(jsonObject.getBaseTime(), entryByTimestamp.getKey());

            // add time-stamped node
            timestampedNodes.add(new TimestampedLwM2mNode(timestamp, node));
        }

        return timestampedNodes;

    }

    private static Long computeTimestamp(Long baseTime, Long time) {
        Long timestamp;
        if (baseTime != null) {
            if (time != null) {
                timestamp = baseTime + time;
            } else {
                timestamp = baseTime;
            }
        } else {
            if (time != null) {
                timestamp = time;
            } else {
                timestamp = null;
            }
        }
        return timestamp;
    }

    /**
     * Group all JsonArrayEntry by time-stamp
     * 
     * @return a map (relativeTimestamp => collection of JsonArrayEntry)
     */
    private static SortedMap<Long, Collection<JsonArrayEntry>> groupJsonEntryByTimestamp(JsonRootObject jsonObject) {
        SortedMap<Long, Collection<JsonArrayEntry>> result = new TreeMap<>(new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                // comparator which
                // - supports null (time null means 0 if there is a base time)
                // - reverses natural order (most recent value in first)
                return Long.compare(o2 == null ? 0 : o2, o1 == null ? 0 : o1);
            }
        });

        for (JsonArrayEntry e : jsonObject.getResourceList()) {
            // Get time for this entry
            Long time = e.getTime();

            // Get jsonArray for this time-stamp
            Collection<JsonArrayEntry> jsonArray = result.get(time);
            if (jsonArray == null) {
                jsonArray = new ArrayList<JsonArrayEntry>();
                result.put(time, jsonArray);
            }

            // Add it to the list
            jsonArray.add(e);
        }

        return result;
    }

    /**
     * Group all JsonArrayEntry by instanceId
     * 
     * @param requestPath
     * @param baseName
     * @param jsonEntries
     * 
     * @return a map (instanceId => collection of JsonArrayEntry)
     */
    private static Map<Integer, Collection<JsonArrayEntry>> groupJsonEntryByInstanceId(
            Collection<JsonArrayEntry> jsonEntries, LwM2mPath baseName) throws InvalidValueException {
        Map<Integer, Collection<JsonArrayEntry>> result = new HashMap<>();

        for (JsonArrayEntry e : jsonEntries) {
            // Build resource path
            LwM2mPath nodePath = baseName.append(e.getName());

            // Validate path
            if (!nodePath.isResourceInstance() && !nodePath.isResource()) {
                throw new InvalidValueException(
                        "Invalid path for resource, it should be a resource or a resource instance path", nodePath);
            }

            // Get jsonArray for this instance
            Collection<JsonArrayEntry> jsonArray = result.get(nodePath.getObjectInstanceId());
            if (jsonArray == null) {
                jsonArray = new ArrayList<>();
                result.put(nodePath.getObjectInstanceId(), jsonArray);
            }

            // Add it to the list
            jsonArray.add(e);
        }

        return result;
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

    private static Map<Integer, LwM2mResource> extractLwM2mResources(Collection<JsonArrayEntry> jsonArrayEntries,
            LwM2mPath baseName, LwM2mModel model) throws InvalidValueException {
        if (jsonArrayEntries == null)
            return Collections.emptyMap();

        // Extract LWM2M resources from JSON resource list
        Map<Integer, LwM2mResource> lwM2mResourceMap = new HashMap<>();
        Map<LwM2mPath, Map<Integer, JsonArrayEntry>> multiResourceMap = new HashMap<>();
        for (JsonArrayEntry resourceElt : jsonArrayEntries) {

            // Build resource path
            LwM2mPath nodePath = baseName.append(resourceElt.getName());

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