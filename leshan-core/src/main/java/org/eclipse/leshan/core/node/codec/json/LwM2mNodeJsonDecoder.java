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

import org.eclipse.leshan.core.json.JsonArrayEntry;
import org.eclipse.leshan.core.json.JsonRootObject;
import org.eclipse.leshan.core.json.LwM2mJson;
import org.eclipse.leshan.core.json.LwM2mJsonException;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mNodeException;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeJsonDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeJsonDecoder.class);

    @SuppressWarnings("unchecked")
    public static <T extends LwM2mNode> T decode(byte[] content, LwM2mPath path, LwM2mModel model, Class<T> nodeClass)
            throws CodecException {
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
        } catch (LwM2mJsonException | LwM2mNodeException e) {
            throw new CodecException(e, "Unable to deserialize json [path:%s]", path);
        }
    }

    public static List<TimestampedLwM2mNode> decodeTimestamped(byte[] content, LwM2mPath path, LwM2mModel model,
            Class<? extends LwM2mNode> nodeClass) throws CodecException {
        try {
            String jsonStrValue = new String(content);
            JsonRootObject json = LwM2mJson.fromJsonLwM2m(jsonStrValue);
            return parseJSON(json, path, model, nodeClass);
        } catch (LwM2mJsonException e) {
            throw new CodecException(e, "Unable to deserialize json [path:%s]", path);
        }
    }

    private static List<TimestampedLwM2mNode> parseJSON(JsonRootObject jsonObject, LwM2mPath requestPath,
            LwM2mModel model, Class<? extends LwM2mNode> nodeClass) throws CodecException {

        LOG.trace("Parsing JSON content for path {}: {}", requestPath, jsonObject);

        // Group JSON entry by time-stamp
        Map<Long, Collection<JsonArrayEntry>> jsonEntryByTimestamp = groupJsonEntryByTimestamp(jsonObject);

        // Extract baseName
        String baseName = jsonObject.getBaseName() == null ? "" : jsonObject.getBaseName();

        // fill time-stamped nodes collection
        List<TimestampedLwM2mNode> timestampedNodes = new ArrayList<>();
        for (Entry<Long, Collection<JsonArrayEntry>> entryByTimestamp : jsonEntryByTimestamp.entrySet()) {

            // Group JSON entry by instance
            Map<Integer, Collection<JsonArrayEntry>> jsonEntryByInstanceId = groupJsonEntryByInstanceId(
                    entryByTimestamp.getValue(), baseName, requestPath);

            // Create lwm2m node
            LwM2mNode node;
            if (nodeClass == LwM2mObject.class) {
                Collection<LwM2mObjectInstance> instances = new ArrayList<>();
                for (Entry<Integer, Collection<JsonArrayEntry>> entryByInstanceId : jsonEntryByInstanceId.entrySet()) {
                    Map<Integer, LwM2mResource> resourcesMap = extractLwM2mResources(entryByInstanceId.getValue(),
                            baseName, model, requestPath);

                    instances.add(new LwM2mObjectInstance(entryByInstanceId.getKey(), resourcesMap.values()));
                }

                node = new LwM2mObject(requestPath.getObjectId(), instances);
            } else if (nodeClass == LwM2mObjectInstance.class) {
                // validate we have resources for only 1 instance
                if (jsonEntryByInstanceId.size() != 1)
                    throw new CodecException("One instance expected in the payload [path:%s]", requestPath);

                // Extract resources
                Entry<Integer, Collection<JsonArrayEntry>> instanceEntry = jsonEntryByInstanceId.entrySet().iterator()
                        .next();
                Map<Integer, LwM2mResource> resourcesMap = extractLwM2mResources(instanceEntry.getValue(), baseName,
                        model, requestPath);

                // Create instance
                node = new LwM2mObjectInstance(instanceEntry.getKey(), resourcesMap.values());
            } else if (nodeClass == LwM2mResource.class) {
                // validate we have resources for only 1 instance
                if (jsonEntryByInstanceId.size() > 1)
                    throw new CodecException("Only one instance expected in the payload [path:%s]", requestPath);

                // Extract resources
                Map<Integer, LwM2mResource> resourcesMap = extractLwM2mResources(
                        jsonEntryByInstanceId.values().iterator().next(), baseName, model, requestPath);

                // validate there is only 1 resource
                if (resourcesMap.size() != 1)
                    throw new CodecException("One resource should be present in the payload [path:%s]", requestPath);

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
                jsonArray = new ArrayList<>();
                result.put(time, jsonArray);
            }

            // Add it to the list
            jsonArray.add(e);
        }

        // Ensure there is at least one entry for null timestamp
        if (result.isEmpty()) {
            result.put((Long) null, new ArrayList<JsonArrayEntry>());
        }

        return result;
    }

    /**
     * Group all JsonArrayEntry by instanceId
     * 
     * @param jsonEntries
     * @param baseName
     *
     * @return a map (instanceId => collection of JsonArrayEntry)
     */
    private static Map<Integer, Collection<JsonArrayEntry>> groupJsonEntryByInstanceId(
            Collection<JsonArrayEntry> jsonEntries, String baseName, LwM2mPath requestPath) throws CodecException {
        Map<Integer, Collection<JsonArrayEntry>> result = new HashMap<>();

        for (JsonArrayEntry e : jsonEntries) {
            // Build resource path
            LwM2mPath nodePath = extractAndValidatePath(baseName, e.getName() == null ? "" : e.getName(), requestPath);

            // Validate path
            if (!nodePath.isResourceInstance() && !nodePath.isResource()) {
                throw new CodecException(
                        "Invalid path [%s] for resource, it should be a resource or a resource instance path",
                        nodePath);
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

        // Create an entry for an empty instance if possible
        if (result.isEmpty()) {
            if (baseName.isEmpty()) {
                // search object instance id in request path
                if (requestPath.getObjectInstanceId() != null) {
                    result.put(requestPath.getObjectInstanceId(), new ArrayList<JsonArrayEntry>());
                }
            } else {
                // search object instance id in basename
                LwM2mPath basePath = extractAndValidatePath(baseName, "", requestPath);
                if (basePath.getObjectInstanceId() != null)
                    result.put(basePath.getObjectInstanceId(), new ArrayList<JsonArrayEntry>());
            }
        }
        return result;
    }

    private static LwM2mPath extractAndValidatePath(String baseName, String name, LwM2mPath requestPath)
            throws CodecException {
        LwM2mPath path = new LwM2mPath(baseName + name);

        // check returned path is under requested path
        if (requestPath.getObjectId() != null && path.getObjectId() != null) {
            if (!path.getObjectId().equals(requestPath.getObjectId())) {
                throw new CodecException("resource path [%s] does not match requested path [%s].", path, requestPath);
            }
            if (requestPath.getObjectInstanceId() != null && path.getObjectInstanceId() != null) {
                if (!path.getObjectInstanceId().equals(requestPath.getObjectInstanceId())) {
                    throw new CodecException("Basename path [%s] does not match requested path [%s].", path,
                            requestPath);
                }
                if (requestPath.getResourceId() != null && path.getResourceId() != null) {
                    if (!path.getResourceId().equals(requestPath.getResourceId())) {
                        throw new CodecException("Basename path [%s] does not match requested path [%s].", path,
                                requestPath);
                    }
                }
            }
        }
        return path;
    }

    private static Map<Integer, LwM2mResource> extractLwM2mResources(Collection<JsonArrayEntry> jsonArrayEntries,
            String baseName, LwM2mModel model, LwM2mPath requestPath) throws CodecException {
        if (jsonArrayEntries == null)
            return Collections.emptyMap();

        // Extract LWM2M resources from JSON resource list
        Map<Integer, LwM2mResource> lwM2mResourceMap = new HashMap<>();
        Map<LwM2mPath, Map<Integer, JsonArrayEntry>> multiResourceMap = new HashMap<>();
        for (JsonArrayEntry resourceElt : jsonArrayEntries) {

            // Build resource path (path validation was already done in groupJsonEntryByInstanceId
            LwM2mPath nodePath;
            if (resourceElt.getName() == null) {
                nodePath = new LwM2mPath(baseName);
            } else {
                nodePath = new LwM2mPath(baseName + resourceElt.getName());
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
                JsonArrayEntry previousResInstance = multiResource.put(nodePath.getResourceInstanceId(), resourceElt);
                if (previousResInstance != null) {
                    throw new CodecException(
                            "2 RESOURCE_INSTANCE nodes (%s,%s) with the same identifier %d for path %s",
                            previousResInstance, resourceElt, nodePath.getResourceInstanceId(), nodePath);
                }
            } else if (nodePath.isResource()) {
                // Single resource
                Type expectedType = getResourceType(nodePath, model, resourceElt);
                LwM2mResource res = LwM2mSingleResource.newResource(nodePath.getResourceId(),
                        parseJsonValue(resourceElt.getResourceValue(), expectedType, nodePath), expectedType);
                LwM2mResource previousRes = lwM2mResourceMap.put(nodePath.getResourceId(), res);
                if (previousRes != null) {
                    throw new CodecException("2 RESOURCE nodes (%s,%s) with the same identifier %d for path %s",
                            previousRes, res, res.getId(), nodePath);
                }
            } else {
                throw new CodecException(
                        "Invalid path [%s] for resource, it should be a resource or a resource instance path",
                        nodePath);
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
                LwM2mResource previousRes = lwM2mResourceMap.put(resourcePath.getResourceId(), resource);
                if (previousRes != null) {
                    throw new CodecException("2 RESOURCE nodes (%s,%s) with the same identifier %d for path %s",
                            previousRes, resource, resource.getId(), resourcePath);
                }
            }
        }

        // If we found nothing, we try to create an empty multi-instance resource
        if (lwM2mResourceMap.isEmpty()) {
            LwM2mPath path;
            if (baseName.isEmpty()) {
                path = requestPath;
            } else {
                path = extractAndValidatePath(baseName, "", requestPath);
            }
            if (path.getObjectId() != null && path.getResourceId() != null) {
                ResourceModel resourceModel = model.getResourceModel(path.getObjectId(), path.getResourceId());
                // We create it only if this respect the model
                if (resourceModel == null || resourceModel.multiple) {
                    Type resourceType = getResourceType(path, model, null);
                    lwM2mResourceMap.put(path.getResourceId(), LwM2mMultipleResource.newResource(path.getResourceId(),
                            new HashMap<Integer, Object>(), resourceType));
                }
            }
        }

        return lwM2mResourceMap;
    }

    private static Object parseJsonValue(Object value, Type expectedType, LwM2mPath path) throws CodecException {

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
                return new Date(((Number) value).longValue() * 1000L);
            case OPAQUE:
                // If the Resource data type is opaque the string value
                // holds the Base64 encoded representation of the Resource
                return Base64.decodeBase64((String) value);
            case STRING:
                return value;
            default:
                throw new CodecException("Unsupported type %s for path %s", expectedType, path);
            }
        } catch (Exception e) {
            throw new CodecException(e, "Invalid content [%s] for type %s for path %s", value, expectedType, path);
        }
    }

    public static Type getResourceType(LwM2mPath rscPath, LwM2mModel model, JsonArrayEntry resourceElt) {
        // Use model type in priority
        ResourceModel rscDesc = model.getResourceModel(rscPath.getObjectId(), rscPath.getResourceId());
        if (rscDesc != null)
            return rscDesc.type;

        // Then json type
        if (resourceElt != null) {
            Type type = resourceElt.getType();
            if (type != null)
                return type;
        }

        // Else use String as default
        LOG.trace("unknown type for resource use string as default: {}", rscPath);
        return Type.STRING;
    }
}