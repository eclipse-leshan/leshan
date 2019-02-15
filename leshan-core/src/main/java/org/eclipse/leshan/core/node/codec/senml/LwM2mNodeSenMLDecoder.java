package org.eclipse.leshan.core.node.codec.senml;

import java.io.IOException;
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
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.json.LwM2mNodeJsonDecoder;
import org.eclipse.leshan.json.JsonRootObject;
import org.eclipse.leshan.json.LwM2mJson;
import org.eclipse.leshan.json.LwM2mJsonException;
import org.eclipse.leshan.senml.SenMLCborLabel;
import org.eclipse.leshan.senml.SenMLDataPoint;
import org.eclipse.leshan.senml.SenMLJsonLabel;
import org.eclipse.leshan.senml.SenMLLabel;
import org.eclipse.leshan.senml.SenMLRootObject;
import org.eclipse.leshan.util.Base64;
import org.eclipse.leshan.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORParser;

public class LwM2mNodeSenMLDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeJsonDecoder.class);

    private static final SenMLLabel SENML_CBOR_LABEL = new SenMLCborLabel();
    private static final SenMLLabel SENML_JSON_LABEL = new SenMLJsonLabel();

    @SuppressWarnings("unchecked")
    public static <T extends LwM2mNode> T decodeCbor(byte[] content, LwM2mPath path, LwM2mModel model,
            Class<T> nodeClass) throws CodecException {
        SenMLRootObject senMLRoot = parseSenMLCbor(content);
        List<TimestampedLwM2mNode> timestampedNodes = parseLwM2MNodes(senMLRoot, path, model, nodeClass);
        if (timestampedNodes.size() == 0) {
            return null;
        } else {
            return (T) timestampedNodes.get(0).getNode();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends LwM2mNode> T decodeJson(byte[] content, LwM2mPath path, LwM2mModel model,
            Class<T> nodeClass) throws CodecException {
        SenMLRootObject senMLRoot = parseSenMLJson(content);
        List<TimestampedLwM2mNode> timestampedNodes = parseLwM2MNodes(senMLRoot, path, model, nodeClass);
        if (timestampedNodes.size() == 0) {
            return null;
        } else {
            return (T) timestampedNodes.get(0).getNode();
        }
    }

    public static List<TimestampedLwM2mNode> decodeTimestampedCbor(byte[] content, LwM2mPath path, LwM2mModel model,
            Class<? extends LwM2mNode> nodeClassFromPath) {
        return null;
    }

    public static List<TimestampedLwM2mNode> decodeTimestampedJson(byte[] content, LwM2mPath path, LwM2mModel model,
            Class<? extends LwM2mNode> nodeClassFromPath) {
        return null;
    }

    private static SenMLRootObject parseSenMLJson(byte[] content) throws CodecException {
        SenMLRootObject rootObject = new SenMLRootObject();
        try {
            JsonArray array = Json.parse(new String(content)).asArray();

            for (int i = 0; i < array.size(); i++) {
                SenMLDataPoint dataPoint = new SenMLDataPoint();
                JsonObject jo = array.get(i).asObject();

                JsonValue bn = jo.get(SENML_JSON_LABEL.getBaseName());
                if (bn != null && bn.isString())
                    rootObject.setBaseName(bn.asString());

                JsonValue bt = jo.get(SENML_JSON_LABEL.getBaseTime());
                if (bt != null && bt.isNumber())
                    rootObject.setBaseTime(bt.asLong());

                JsonValue n = jo.get(SENML_JSON_LABEL.getName());
                if (n != null && n.isString()) {
                    dataPoint.setName(n.asString());
                }

                JsonValue t = jo.get(SENML_JSON_LABEL.getTime());
                if (t != null && t.isNumber())
                    dataPoint.setTime(t.asLong());

                JsonValue v = jo.get(SENML_JSON_LABEL.getNumberValue());
                if (v != null && v.isNumber())
                    dataPoint.setFloatValue(v.asDouble());

                JsonValue bv = jo.get(SENML_JSON_LABEL.getBooleanValue());
                if (bv != null && bv.isBoolean())
                    dataPoint.setBooleanValue(bv.asBoolean());

                JsonValue dv = jo.get(SENML_JSON_LABEL.getDataValue());
                if (bv != null && bv.isBoolean())
                    dataPoint.setOpaqueValue(Hex.decodeHex(dv.asString().toCharArray()));

                JsonValue sv = jo.get(SENML_JSON_LABEL.getStringValue());
                if (sv != null && sv.isString())
                    dataPoint.setStringValue(sv.asString());

                rootObject.addDataPoint(dataPoint);
            }
        } catch (Exception e) {
            throw new CodecException(e, "Unable to deserialize json [path:%s]");
        }

        return rootObject;
    }

    private static SenMLRootObject parseSenMLCbor(byte[] content) throws CodecException {
        SenMLRootObject rootObject = new SenMLRootObject();
        try {
            CBORFactory factory = new CBORFactory();
            CBORParser parser = factory.createParser(content);

            JsonToken token = null;
            while (true) {
                token = parser.nextToken();
                if (token == JsonToken.START_ARRAY) {
                    continue;
                }

                if (token == JsonToken.START_OBJECT) {
                    SenMLDataPoint dataPoint = parseSenMLDataPoint(rootObject, parser);
                    rootObject.addDataPoint(dataPoint);
                }

                if (token == JsonToken.END_ARRAY) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new CodecException(e, "Unable to deserialize json [path:%s]");
        }

        return rootObject;
    }

    private static SenMLDataPoint parseSenMLDataPoint(SenMLRootObject rootObject, CBORParser parser) {
        SenMLDataPoint dataPoint = new SenMLDataPoint();
        try {
            JsonToken token = null;
            String fileName = null;
            while (true) {
                token = parser.nextToken();
                if (token == JsonToken.END_OBJECT) {
                    return dataPoint;
                } else if (token == JsonToken.FIELD_NAME) {
                    fileName = parser.getCurrentName();
                    if (SENML_CBOR_LABEL.isBaseName(fileName)) {
                        token = parser.nextToken();
                        rootObject.setBaseName(parser.getText());
                        continue;
                    } else if (SENML_CBOR_LABEL.isName(fileName)) {
                        token = parser.nextToken();
                        dataPoint.setName(parser.getText());
                    }
                } else {
                    if (SENML_CBOR_LABEL.isNumberValue(fileName)) {
                        dataPoint.setFloatValue(parser.getFloatValue());
                    } else if (SENML_CBOR_LABEL.isBooleanValue(fileName)) {
                        dataPoint.setBooleanValue(parser.getValueAsBoolean());
                    } else if (SENML_CBOR_LABEL.isStringValue(fileName)) {
                        dataPoint.setStringValue(parser.getValueAsString());
                    } else if (SENML_CBOR_LABEL.isDataValue(fileName)) {
                        dataPoint.setStringValue(parser.getValueAsString());
                    }
                }
            }
        } catch (IOException e) {
            throw new CodecException(e, "Unable to deserialize json [path:%s]");
        }
    }

    public static List<TimestampedLwM2mNode> decodeTimestamped(byte[] content, LwM2mPath path, LwM2mModel model,
            Class<? extends LwM2mNode> nodeClass) throws CodecException {
        try {
            String jsonStrValue = new String(content);
            JsonRootObject json = LwM2mJson.fromJsonLwM2m(jsonStrValue);
            // return parseJSON(json, path, model, nodeClass);
            return null;
        } catch (LwM2mJsonException e) {
            throw new CodecException(e, "Unable to deserialize json [path:%s]", path);
        }
    }

    private static List<TimestampedLwM2mNode> parseLwM2MNodes(SenMLRootObject jsonObject, LwM2mPath path,
            LwM2mModel model, Class<? extends LwM2mNode> nodeClass) throws CodecException {

        LOG.trace("Parsing JSON content for path {}: {}", path, jsonObject);

        // Group JSON entry by time-stamp
        Map<Long, Collection<SenMLDataPoint>> jsonEntryByTimestamp = groupJsonEntryByTimestamp(jsonObject);

        // Extract baseName
        LwM2mPath baseName = extractAndValidateBaseName(jsonObject, path);
        if (baseName == null)
            baseName = path; // if no base name, use request path as base name

        // fill time-stamped nodes collection
        List<TimestampedLwM2mNode> timestampedNodes = new ArrayList<>();
        for (Entry<Long, Collection<SenMLDataPoint>> entryByTimestamp : jsonEntryByTimestamp.entrySet()) {

            // Group JSON entry by instance
            Map<Integer, Collection<SenMLDataPoint>> jsonEntryByInstanceId = groupJsonEntryByInstanceId(
                    entryByTimestamp.getValue(), baseName);

            // Create lwm2m node
            LwM2mNode node;
            if (nodeClass == LwM2mObject.class) {
                Collection<LwM2mObjectInstance> instances = new ArrayList<>();
                for (Entry<Integer, Collection<SenMLDataPoint>> entryByInstanceId : jsonEntryByInstanceId.entrySet()) {
                    Map<Integer, LwM2mResource> resourcesMap = extractLwM2mResources(entryByInstanceId.getValue(),
                            baseName, model);

                    instances.add(new LwM2mObjectInstance(entryByInstanceId.getKey(), resourcesMap.values()));
                }

                node = new LwM2mObject(baseName.getObjectId(), instances);
            } else if (nodeClass == LwM2mObjectInstance.class) {
                // validate we have resources for only 1 instance
                if (jsonEntryByInstanceId.size() != 1)
                    throw new CodecException("One instance expected in the payload [path:%s]", path);

                // Extract resources
                Entry<Integer, Collection<SenMLDataPoint>> instanceEntry = jsonEntryByInstanceId.entrySet().iterator()
                        .next();
                Map<Integer, LwM2mResource> resourcesMap = extractLwM2mResources(instanceEntry.getValue(), baseName,
                        model);

                // Create instance
                node = new LwM2mObjectInstance(instanceEntry.getKey(), resourcesMap.values());
            } else if (nodeClass == LwM2mResource.class) {
                // validate we have resources for only 1 instance
                if (jsonEntryByInstanceId.size() > 1)
                    throw new CodecException("Only one instance expected in the payload [path:%s]", path);

                // Extract resources
                Map<Integer, LwM2mResource> resourcesMap = extractLwM2mResources(
                        jsonEntryByInstanceId.values().iterator().next(), baseName, model);

                // validate there is only 1 resource
                if (resourcesMap.size() != 1)
                    throw new CodecException("One resource should be present in the payload [path:%s]", path);

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
    private static SortedMap<Long, Collection<SenMLDataPoint>> groupJsonEntryByTimestamp(SenMLRootObject jsonObject) {
        SortedMap<Long, Collection<SenMLDataPoint>> result = new TreeMap<>(new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                // comparator which
                // - supports null (time null means 0 if there is a base time)
                // - reverses natural order (most recent value in first)
                return Long.compare(o2 == null ? 0 : o2, o1 == null ? 0 : o1);
            }
        });

        for (SenMLDataPoint e : jsonObject.getDataPoints()) {
            // Get time for this entry
            Long time = e.getTime();

            // Get jsonArray for this time-stamp
            Collection<SenMLDataPoint> jsonArray = result.get(time);
            if (jsonArray == null) {
                jsonArray = new ArrayList<>();
                result.put(time, jsonArray);
            }

            // Add it to the list
            jsonArray.add(e);
        }

        // Ensure there is at least one entry for null timestamp
        if (result.isEmpty()) {
            result.put((Long) null, new ArrayList<SenMLDataPoint>());
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
    private static Map<Integer, Collection<SenMLDataPoint>> groupJsonEntryByInstanceId(
            Collection<SenMLDataPoint> jsonEntries, LwM2mPath baseName) throws CodecException {
        Map<Integer, Collection<SenMLDataPoint>> result = new HashMap<>();

        for (SenMLDataPoint e : jsonEntries) {
            // Build resource path

            LwM2mPath nodePath = e.getName() == null ? baseName : baseName.append(e.getName());

            // Validate path
            if (!nodePath.isResourceInstance() && !nodePath.isResource()) {
                throw new CodecException(
                        "Invalid path [%s] for resource, it should be a resource or a resource instance path",
                        nodePath);
            }

            // Get jsonArray for this instance
            Collection<SenMLDataPoint> jsonArray = result.get(nodePath.getObjectInstanceId());
            if (jsonArray == null) {
                jsonArray = new ArrayList<>();
                result.put(nodePath.getObjectInstanceId(), jsonArray);
            }

            // Add it to the list
            jsonArray.add(e);
        }

        // Create an entry for an empty instance if possible
        if (result.isEmpty() && baseName.getObjectInstanceId() != null) {
            result.put(baseName.getObjectInstanceId(), new ArrayList<SenMLDataPoint>());
        }
        return result;
    }

    private static LwM2mPath extractAndValidateBaseName(SenMLRootObject jsonObject, LwM2mPath requestPath)
            throws CodecException {
        // Check baseName is valid
        if (jsonObject.getBaseName() != null && !jsonObject.getBaseName().isEmpty()) {
            LwM2mPath bnPath = new LwM2mPath(jsonObject.getBaseName());

            // check returned base name path is under requested path
            if (requestPath.getObjectId() != null && bnPath.getObjectId() != null) {
                if (!bnPath.getObjectId().equals(requestPath.getObjectId())) {
                    throw new CodecException("Basename path [%s] does not match requested path [%s].", bnPath,
                            requestPath);
                }
                if (requestPath.getObjectInstanceId() != null && bnPath.getObjectInstanceId() != null) {
                    if (!bnPath.getObjectInstanceId().equals(requestPath.getObjectInstanceId())) {
                        throw new CodecException("Basename path [%s] does not match requested path [%s].", bnPath,
                                requestPath);
                    }
                    if (requestPath.getResourceId() != null && bnPath.getResourceId() != null) {
                        if (!bnPath.getResourceId().equals(requestPath.getResourceId())) {
                            throw new CodecException("Basename path [%s] does not match requested path [%s].", bnPath,
                                    requestPath);
                        }
                    }
                }
            }
            return bnPath;
        }
        return null;

    }

    private static Map<Integer, LwM2mResource> extractLwM2mResources(Collection<SenMLDataPoint> jsonArrayEntries,
            LwM2mPath baseName, LwM2mModel model) throws CodecException {
        if (jsonArrayEntries == null)
            return Collections.emptyMap();

        // Extract LWM2M resources from JSON resource list
        Map<Integer, LwM2mResource> lwM2mResourceMap = new HashMap<>();
        Map<LwM2mPath, Map<Integer, SenMLDataPoint>> multiResourceMap = new HashMap<>();
        for (SenMLDataPoint resourceElt : jsonArrayEntries) {

            // Build resource path
            LwM2mPath nodePath = resourceElt.getName() == null ? baseName : baseName.append(resourceElt.getName());

            // handle LWM2M resources
            if (nodePath.isResourceInstance()) {
                // Multi-instance resource
                // Store multi-instance resource values in a map
                // we will deal with it later
                LwM2mPath resourcePath = new LwM2mPath(nodePath.getObjectId(), nodePath.getObjectInstanceId(),
                        nodePath.getResourceId());
                Map<Integer, SenMLDataPoint> multiResource = multiResourceMap.get(resourcePath);
                if (multiResource == null) {
                    multiResource = new HashMap<>();
                    multiResourceMap.put(resourcePath, multiResource);
                }
                SenMLDataPoint previousResInstance = multiResource.put(nodePath.getResourceInstanceId(), resourceElt);
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
        for (Map.Entry<LwM2mPath, Map<Integer, SenMLDataPoint>> entry : multiResourceMap.entrySet()) {
            LwM2mPath resourcePath = entry.getKey();
            Map<Integer, SenMLDataPoint> jsonEntries = entry.getValue();

            if (jsonEntries != null && !jsonEntries.isEmpty()) {
                Type expectedType = getResourceType(resourcePath, model, jsonEntries.values().iterator().next());
                Map<Integer, Object> values = new HashMap<>();
                for (Entry<Integer, SenMLDataPoint> e : jsonEntries.entrySet()) {
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
        if (lwM2mResourceMap.isEmpty() && baseName.isResource()) {
            ResourceModel resourceModel = model.getResourceModel(baseName.getObjectId(), baseName.getResourceId());
            // We create it only if this respect the model
            if (resourceModel == null || resourceModel.multiple) {
                Type resourceType = getResourceType(baseName, model, null);
                lwM2mResourceMap.put(baseName.getResourceId(), LwM2mMultipleResource
                        .newResource(baseName.getResourceId(), new HashMap<Integer, Object>(), resourceType));
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
            case OBJLNK:
                return value;
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

    public static Type getResourceType(LwM2mPath rscPath, LwM2mModel model, SenMLDataPoint resourceElt) {
        // Use model type in priority
        ResourceModel rscDesc = model.getResourceModel(rscPath.getObjectId(), rscPath.getResourceId());
        if (rscDesc != null && rscDesc.type != null)
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
