/*******************************************************************************
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
 *     Boya Zhang - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.node.codec.senml;

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
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.TimestampedNodeDecoder;
import org.eclipse.leshan.core.util.Base64;
import org.eclipse.leshan.core.util.datatype.NumberUtil;
import org.eclipse.leshan.core.util.datatype.ULong;
import org.eclipse.leshan.senml.SenMLDecoder;
import org.eclipse.leshan.senml.SenMLException;
import org.eclipse.leshan.senml.SenMLPack;
import org.eclipse.leshan.senml.SenMLRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeSenMLDecoder implements TimestampedNodeDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeSenMLDecoder.class);

    private final SenMLDecoder decoder;

    public LwM2mNodeSenMLDecoder(SenMLDecoder decoder) {
        this.decoder = decoder;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends LwM2mNode> T decode(byte[] content, LwM2mPath path, LwM2mModel model, Class<T> nodeClass)
            throws CodecException {
        try {
            SenMLPack pack = decoder.fromSenML(content);
            List<TimestampedLwM2mNode> timestampedNodes = parseSenMLPack(pack, path, model, nodeClass);
            if (timestampedNodes.size() == 0) {
                return null;
            } else if (timestampedNodes.size() == 1 && timestampedNodes.get(0).getTimestamp() == null) {
                return (T) timestampedNodes.get(0).getNode();
            } else {
                throw new CodecException("Unable to decode node[path:%s] : value should not be timestamped", path);
            }
        } catch (SenMLException e) {
            String jsonStrValue = content != null ? new String(content) : "";
            throw new CodecException(e, "Unable to decode node[path:%s] : %s", path, jsonStrValue, e);
        }
    }

    @Override
    public List<TimestampedLwM2mNode> decodeTimestampedData(byte[] content, LwM2mPath path, LwM2mModel model,
            Class<? extends LwM2mNode> nodeClass) throws CodecException {
        try {
            SenMLPack pack = decoder.fromSenML(content);
            return parseSenMLPack(pack, path, model, nodeClass);
        } catch (SenMLException e) {
            String jsonStrValue = content != null ? new String(content) : "";
            throw new CodecException(e, "Unable to decode node[path:%s] : %s", path, jsonStrValue, e);
        }
    }

    private List<TimestampedLwM2mNode> parseSenMLPack(SenMLPack pack, LwM2mPath path, LwM2mModel model,
            Class<? extends LwM2mNode> nodeClass) throws CodecException, SenMLException {

        LOG.trace("Parsing SenML JSON object for path {}: {}", path, pack);

        // Group Records by time-stamp
        Map<Long, Collection<LwM2mResolvedSenMLRecord>> recordsByTimestamp = groupRecordByTimestamp(pack.getRecords(),
                path);

        // fill time-stamped nodes collection
        List<TimestampedLwM2mNode> timestampedNodes = new ArrayList<>();
        for (Entry<Long, Collection<LwM2mResolvedSenMLRecord>> entryByTimestamp : recordsByTimestamp.entrySet()) {

            // Group records by instance
            Map<Integer, Collection<LwM2mResolvedSenMLRecord>> recordsByInstanceId = groupRecordsByInstanceId(
                    entryByTimestamp.getValue());

            // Create lwm2m node
            LwM2mNode node = null;
            if (nodeClass == LwM2mObject.class) {
                Collection<LwM2mObjectInstance> instances = new ArrayList<>();
                for (Entry<Integer, Collection<LwM2mResolvedSenMLRecord>> entryByInstanceId : recordsByInstanceId
                        .entrySet()) {
                    Map<Integer, LwM2mResource> resourcesMap = extractLwM2mResources(entryByInstanceId.getValue(), path,
                            model);

                    instances.add(new LwM2mObjectInstance(entryByInstanceId.getKey(), resourcesMap.values()));
                }

                node = new LwM2mObject(path.getObjectId(), instances);
            } else if (nodeClass == LwM2mObjectInstance.class) {
                // validate we have resources for only 1 instance
                if (recordsByInstanceId.size() != 1)
                    throw new CodecException("One instance expected in the payload [path:%s]", path);

                // Extract resources
                Entry<Integer, Collection<LwM2mResolvedSenMLRecord>> instanceEntry = recordsByInstanceId.entrySet()
                        .iterator().next();
                Map<Integer, LwM2mResource> resourcesMap = extractLwM2mResources(instanceEntry.getValue(), path, model);

                // Create instance
                node = new LwM2mObjectInstance(instanceEntry.getKey(), resourcesMap.values());
            } else if (nodeClass == LwM2mResource.class) {
                // validate we have resources for only 1 instance
                if (recordsByInstanceId.size() > 1)
                    throw new CodecException("Only one instance expected in the payload [path:%s]", path);

                // Extract resources
                Map<Integer, LwM2mResource> resourcesMap = extractLwM2mResources(
                        recordsByInstanceId.values().iterator().next(), path, model);

                // validate there is only 1 resource
                if (resourcesMap.size() != 1)
                    throw new CodecException("One resource should be present in the payload [path:%s]", path);

                node = resourcesMap.values().iterator().next();
            } else if (nodeClass == LwM2mResourceInstance.class) {
                // validate we have resources for only 1 instance
                if (recordsByInstanceId.size() > 1)
                    throw new CodecException("Only one instance expected in the payload [path:%s]", path);

                // Extract resources
                Map<Integer, LwM2mResource> resourcesMap = extractLwM2mResources(
                        recordsByInstanceId.values().iterator().next(), path, model);

                // validate there is only 1 resource
                if (resourcesMap.size() != 1)
                    throw new CodecException("One resource should be present in the payload [path:%s]", path);

                LwM2mResource resource = resourcesMap.values().iterator().next();
                if (!resource.isMultiInstances()) {
                    throw new CodecException("Resource should be multi Instances resource [path:%s]", path);
                }

                if (resource.getInstances().isEmpty()) {
                    throw new CodecException("Resource instances should not be not empty [path:%s]", path);
                }

                if (resource.getInstances().size() > 1) {
                    throw new CodecException("Resource instances should not be > 1 [path:%s]", path);
                }

                node = resourcesMap.values().iterator().next().getInstance(path.getResourceInstanceId());
            } else {
                throw new IllegalArgumentException("invalid node class: " + nodeClass);
            }

            // add time-stamped node
            timestampedNodes.add(new TimestampedLwM2mNode(entryByTimestamp.getKey(), node));
        }

        return timestampedNodes;

    }

    /**
     * Resolved record then group it by time-stamp
     * 
     * @return a sorted map (timestamp => collection of record) order by descending time-stamp (most recent one at first
     *         place). If null time-stamp (meaning no time information) exists it always at first place.
     */
    private SortedMap<Long, Collection<LwM2mResolvedSenMLRecord>> groupRecordByTimestamp(List<SenMLRecord> records,
            LwM2mPath requestPath) throws SenMLException {
        SortedMap<Long, Collection<LwM2mResolvedSenMLRecord>> result = new TreeMap<>(new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                // null at first place
                if (o1 == null && o2 == null)
                    return 0;
                if (o1 == null)
                    return -1;
                if (o2 == null)
                    return 1;
                return Long.compare(o2, o1);
            }
        });

        LwM2mSenMLResolver resolver = new LwM2mSenMLResolver();
        for (SenMLRecord record : records) {
            LwM2mResolvedSenMLRecord resolvedRecord = resolver.resolve(record);

            // Validate SenML resolved name (lwm2m node path)
            if (!resolvedRecord.getPath().isResourceInstance() && !resolvedRecord.getPath().isResource()) {
                throw new CodecException(
                        "Invalid path [%s] for resource, it should be a resource or a resource instance path",
                        resolvedRecord.getName());
            }

            if (!resolvedRecord.getPath().startWith(requestPath)) {
                throw new CodecException("Invalid path [%s] for resource, it should start by %s",
                        resolvedRecord.getName(), requestPath);
            }

            // Get record list for this time-stamp
            Collection<LwM2mResolvedSenMLRecord> recordList = result.get(resolvedRecord.getTimeStamp());
            if (recordList == null) {
                recordList = new ArrayList<>();
                result.put(resolvedRecord.getTimeStamp(), recordList);
            }
            // Add it to the list
            recordList.add(resolvedRecord);
        }

        // Ensure there is at least one entry for null timestamp
        if (result.isEmpty()) {
            Collection<LwM2mResolvedSenMLRecord> emptylist = Collections.emptyList();
            result.put((Long) null, emptylist);
        }
        return result;
    }

    /**
     * Group all SenML record by instanceId
     * 
     * @return a map (instanceId => collection of SenML Record)
     */
    private Map<Integer, Collection<LwM2mResolvedSenMLRecord>> groupRecordsByInstanceId(
            Collection<LwM2mResolvedSenMLRecord> records) throws CodecException {
        Map<Integer, Collection<LwM2mResolvedSenMLRecord>> result = new HashMap<>();
        for (LwM2mResolvedSenMLRecord record : records) {
            // Get SenML records for this instance
            Collection<LwM2mResolvedSenMLRecord> recordForInstance = result.get(record.getPath().getObjectInstanceId());
            if (recordForInstance == null) {
                recordForInstance = new ArrayList<>();
                result.put(record.getPath().getObjectInstanceId(), recordForInstance);
            }

            // Add it to the list
            recordForInstance.add(record);
        }
        return result;
    }

    private Map<Integer, LwM2mResource> extractLwM2mResources(Collection<LwM2mResolvedSenMLRecord> records,
            LwM2mPath requestPath, LwM2mModel model) throws CodecException {
        if (records == null)
            return Collections.emptyMap();

        // Extract LWM2M resources from JSON resource list
        Map<Integer, LwM2mResource> lwM2mResourceMap = new HashMap<>();
        Map<LwM2mPath, Map<Integer, SenMLRecord>> multiResourceMap = new HashMap<>();

        for (LwM2mResolvedSenMLRecord resolvedRecord : records) {
            // Build resource path
            LwM2mPath nodePath = resolvedRecord.getPath();
            SenMLRecord record = resolvedRecord.getRecord();

            // handle LWM2M resources
            if (nodePath.isResourceInstance()) {
                // Multi-instance resource
                // Store multi-instance resource values in a map
                // we will deal with it later
                LwM2mPath resourcePath = new LwM2mPath(nodePath.getObjectId(), nodePath.getObjectInstanceId(),
                        nodePath.getResourceId());
                Map<Integer, SenMLRecord> multiResource = multiResourceMap.get(resourcePath);
                if (multiResource == null) {
                    multiResource = new HashMap<>();
                    multiResourceMap.put(resourcePath, multiResource);
                }
                SenMLRecord previousResInstance = multiResource.put(nodePath.getResourceInstanceId(), record);
                if (previousResInstance != null) {
                    throw new CodecException(
                            "2 RESOURCE_INSTANCE nodes (%s,%s) with the same identifier %d for path %s",
                            previousResInstance, record, nodePath.getResourceInstanceId(), nodePath);
                }
            } else if (nodePath.isResource()) {
                // Single resource
                Type expectedType = getResourceType(nodePath, model, record);
                Object resourceValue = parseResourceValue(record.getResourceValue(), expectedType, nodePath);
                LwM2mResource res = LwM2mSingleResource.newResource(nodePath.getResourceId(), resourceValue,
                        expectedType);
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

        // Handle multiple resource instances.
        for (Map.Entry<LwM2mPath, Map<Integer, SenMLRecord>> entry : multiResourceMap.entrySet()) {
            LwM2mPath resourcePath = entry.getKey();
            Map<Integer, SenMLRecord> entries = entry.getValue();

            if (entries != null && !entries.isEmpty()) {
                Type expectedType = getResourceType(resourcePath, model, entries.values().iterator().next());
                Map<Integer, Object> values = new HashMap<>();
                for (Entry<Integer, SenMLRecord> e : entries.entrySet()) {
                    Integer resourceInstanceId = e.getKey();
                    values.put(resourceInstanceId,
                            parseResourceValue(e.getValue().getResourceValue(), expectedType, resourcePath));
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
        if (lwM2mResourceMap.isEmpty() && requestPath.isResource()) {
            ResourceModel resourceModel = model.getResourceModel(requestPath.getObjectId(),
                    requestPath.getResourceId());
            // We create it only if this respect the model
            if (resourceModel == null || resourceModel.multiple) {
                Type resourceType = getResourceType(requestPath, model, null);
                lwM2mResourceMap.put(requestPath.getResourceId(), LwM2mMultipleResource
                        .newResource(requestPath.getResourceId(), new HashMap<Integer, Object>(), resourceType));
            }
        }

        return lwM2mResourceMap;
    }

    private Object parseResourceValue(Object value, Type expectedType, LwM2mPath path) throws CodecException {
        LOG.trace("Parse SenML value for path {} and expected type {}: {}", path, expectedType, value);

        try {
            switch (expectedType) {
            case INTEGER:
                return numberToLong((Number) value);
            case UNSIGNED_INTEGER:
                return numberToULong((Number) value);
            case BOOLEAN:
                return value;
            case FLOAT:
                return numberToDouble((Number) value);
            case TIME:
                return new Date(numberToLong((Number) value) * 1000L);
            case OPAQUE:
                // If the Resource data type is opaque the string value
                // holds the Base64 encoded representation of the Resource
                return Base64.decodeBase64((String) value);
            case STRING:
                return value;
            case OBJLNK:
                return ObjectLink.decodeFromString((String) value);
            default:
                throw new CodecException("Unsupported type %s for path %s", expectedType, path);
            }
        } catch (Exception e) {
            throw new CodecException(e, "Invalid content [%s] for type %s for path %s", value, expectedType, path);
        }
    }

    private Type getResourceType(LwM2mPath rscPath, LwM2mModel model, SenMLRecord record) {
        // Use model type in priority
        ResourceModel rscDesc = model.getResourceModel(rscPath.getObjectId(), rscPath.getResourceId());
        if (rscDesc != null && rscDesc.type != null)
            return rscDesc.type;

        // Then json type
        if (record != null) {
            Type type = record.getType();
            if (type != null)
                return type;
        }

        // Else use String as default
        LOG.trace("unknown type for resource use string as default: {}", rscPath);
        return Type.STRING;
    }

    protected Long numberToLong(Number number) {
        return NumberUtil.numberToLong(number);
    }

    protected ULong numberToULong(Number number) {
        return NumberUtil.numberToULong(number);
    }

    protected Double numberToDouble(Number number) {
        // we get the better approximate value, meaning we can get precision loss
        return number.doubleValue();
    }
}