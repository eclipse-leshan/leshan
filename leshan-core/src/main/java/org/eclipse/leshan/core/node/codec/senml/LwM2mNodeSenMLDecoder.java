/*******************************************************************************
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
 *     Boya Zhang - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.node.codec.senml;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.eclipse.leshan.core.link.LinkParser;
import org.eclipse.leshan.core.link.lwm2m.DefaultLwM2mLinkParser;
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
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.MultiNodeDecoder;
import org.eclipse.leshan.core.node.codec.TimestampedMultiNodeDecoder;
import org.eclipse.leshan.core.node.codec.TimestampedNodeDecoder;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.TimestampUtil;
import org.eclipse.leshan.core.util.datatype.NumberUtil;
import org.eclipse.leshan.core.util.datatype.ULong;
import org.eclipse.leshan.senml.SenMLDecoder;
import org.eclipse.leshan.senml.SenMLException;
import org.eclipse.leshan.senml.SenMLPack;
import org.eclipse.leshan.senml.SenMLRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeSenMLDecoder implements TimestampedNodeDecoder, MultiNodeDecoder, TimestampedMultiNodeDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeSenMLDecoder.class);

    private final SenMLDecoder decoder;
    private boolean permissiveNumberConversion;
    // parser used for core link data type
    private final LinkParser linkParser;

    public LwM2mNodeSenMLDecoder(SenMLDecoder decoder, boolean permissiveNumberConversion) {
        this(decoder, new DefaultLwM2mLinkParser(), permissiveNumberConversion);
    }

    public LwM2mNodeSenMLDecoder(SenMLDecoder decoder, LinkParser linkParser, boolean permissiveNumberConversion) {
        this.decoder = decoder;
        this.permissiveNumberConversion = permissiveNumberConversion;
        this.linkParser = linkParser;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends LwM2mNode> T decode(byte[] content, LwM2mPath path, LwM2mModel model, Class<T> nodeClass)
            throws CodecException {
        try {
            // Decode SenML pack
            SenMLPack pack = decoder.fromSenML(content);
            List<SenMLRecord> records = pack.getRecords();

            // Resolve records
            LwM2mSenMLResolver resolver = new LwM2mSenMLResolver();
            Collection<LwM2mResolvedSenMLRecord> resolvedRecords = new ArrayList<>(records.size());
            for (SenMLRecord record : records) {
                LwM2mResolvedSenMLRecord resolvedRecord = resolver.resolve(record);
                // Validate SenML resolved name
                if (!resolvedRecord.getPath().isResourceInstance() && !resolvedRecord.getPath().isResource()) {
                    throw new CodecException(
                            "Invalid path [%s] for resource, it should be a resource or a resource instance path",
                            resolvedRecord.getName());
                }
                if (!resolvedRecord.getPath().startWith(path)) {
                    throw new CodecException("Invalid path [%s] for resource, it should start by %s",
                            resolvedRecord.getPath(), path);
                }
                if (resolvedRecord.getTimeStamp() != null) {
                    throw new CodecException("Unable to decode node[path:%s] : value should not be timestamped", path);
                }
                resolvedRecords.add(resolvedRecord);
            }

            // Parse records and create node
            return (T) parseRecords(resolvedRecords, path, model, nodeClass);
        } catch (SenMLException e) {
            String hexValue = content != null ? Hex.encodeHexString(content) : "";
            throw new CodecException(e, "Unable to decode node[path:%s] : %s", path, hexValue, e);
        }
    }

    @Override
    public Map<LwM2mPath, LwM2mNode> decodeNodes(byte[] content, List<LwM2mPath> paths, LwM2mModel model)
            throws CodecException {
        try {
            // Decode SenML pack
            SenMLPack pack = decoder.fromSenML(content);

            Map<LwM2mPath, LwM2mNode> nodes = new HashMap<>();
            if (paths != null) {
                // Resolve records & Group it by time-stamp
                Map<LwM2mPath, Collection<LwM2mResolvedSenMLRecord>> recordsByPath = groupByPath(pack.getRecords(),
                        paths);

                for (LwM2mPath path : paths) {
                    Collection<LwM2mResolvedSenMLRecord> records = recordsByPath.get(path);
                    if (records.isEmpty()) {
                        // Node can be null as the LWM2M specification says that "Read-Composite operation is
                        // treated as non-atomic and handled as best effort by the client. That is, if any of the
                        // requested
                        // resources do not have a valid value to return, they will not be included in the response".
                        // Meaning that a given path could have no corresponding value.
                        nodes.put(path, null);
                    } else {
                        LwM2mNode node = parseRecords(recordsByPath.get(path), path, model,
                                DefaultLwM2mDecoder.nodeClassFromPath(path));
                        nodes.put(path, node);
                    }
                }
            } else {
                // Paths are not given so we given so we can not regroup by path
                // let's assume that each path refer to a single resource or single resource instances.
                LwM2mSenMLResolver resolver = new LwM2mSenMLResolver();
                for (SenMLRecord record : pack.getRecords()) {
                    LwM2mResolvedSenMLRecord resolvedRecord = resolver.resolve(record);
                    LwM2mPath path = resolvedRecord.getPath();
                    LwM2mNode node = parseRecords(Arrays.asList(resolvedRecord), path, model,
                            DefaultLwM2mDecoder.nodeClassFromPath(path));
                    nodes.put(path, node);
                }
            }
            return nodes;
        } catch (SenMLException e) {
            String hexValue = content != null ? Hex.encodeHexString(content) : "";
            throw new CodecException(e, "Unable to decode nodes[path:%s] : %s", paths, hexValue, e);
        }
    }

    @Override
    public List<TimestampedLwM2mNode> decodeTimestampedData(byte[] content, LwM2mPath path, LwM2mModel model,
            Class<? extends LwM2mNode> nodeClass) throws CodecException {
        try {
            // Decode SenML pack
            SenMLPack pack = decoder.fromSenML(content);

            // Resolve records & Group it by time-stamp
            Map<BigDecimal, Collection<LwM2mResolvedSenMLRecord>> recordsByTimestamp = groupRecordByTimestamp(
                    pack.getRecords(), path);

            // Fill time-stamped nodes collection
            List<TimestampedLwM2mNode> timestampedNodes = new ArrayList<>();
            for (Entry<BigDecimal, Collection<LwM2mResolvedSenMLRecord>> entryByTimestamp : recordsByTimestamp
                    .entrySet()) {
                LwM2mNode node = parseRecords(entryByTimestamp.getValue(), path, model, nodeClass);
                // add time-stamped node
                timestampedNodes
                        .add(new TimestampedLwM2mNode(TimestampUtil.fromSeconds(entryByTimestamp.getKey()), node));
            }
            return timestampedNodes;
        } catch (SenMLException e) {
            String hexValue = content != null ? Hex.encodeHexString(content) : "";
            throw new CodecException(e, "Unable to decode node[path:%s] : %s", path, hexValue, e);
        }
    }

    @Override
    public TimestampedLwM2mNodes decodeTimestampedNodes(byte[] content, LwM2mModel model) throws CodecException {
        try {
            // Decode SenML pack
            SenMLPack pack = decoder.fromSenML(content);

            TimestampedLwM2mNodes.Builder nodes = TimestampedLwM2mNodes.builder();

            LwM2mSenMLResolver resolver = new LwM2mSenMLResolver();
            for (SenMLRecord record : pack.getRecords()) {
                LwM2mResolvedSenMLRecord resolvedRecord = resolver.resolve(record);
                LwM2mPath path = resolvedRecord.getPath();
                LwM2mNode node = parseRecords(Arrays.asList(resolvedRecord), path, model,
                        DefaultLwM2mDecoder.nodeClassFromPath(path));
                nodes.put(TimestampUtil.fromSeconds(resolvedRecord.getTimeStamp()), path, node);
            }

            return nodes.build();
        } catch (SenMLException | IllegalArgumentException e) {
            String hexValue = content != null ? Hex.encodeHexString(content) : "";
            throw new CodecException(e, "Unable to decode nodes : %s", hexValue, e);
        }
    }

    /**
     * Parse records for a given LWM2M path.
     */
    private LwM2mNode parseRecords(Collection<LwM2mResolvedSenMLRecord> records, LwM2mPath path, LwM2mModel model,
            Class<? extends LwM2mNode> nodeClass) throws CodecException {

        LOG.trace("Parsing SenML records for path {}: {}", path, records);

        // Group records by instance
        Map<Integer, Collection<LwM2mResolvedSenMLRecord>> recordsByInstanceId = groupRecordsByInstanceId(records);

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

            // handle empty multi instance resource ?
            if (recordsByInstanceId.size() == 0) {
                ResourceModel resourceModel = model.getResourceModel(path.getObjectId(), path.getResourceId());
                if (resourceModel == null || !resourceModel.multiple) {
                    throw new CodecException(
                            "One resource should be present in the payload [path:%s] for single instance resource",
                            path);
                }

                node = new LwM2mMultipleResource(path.getResourceId(), resourceModel.type);
            } else {
                // Extract resources
                Map<Integer, LwM2mResource> resourcesMap = extractLwM2mResources(
                        recordsByInstanceId.values().iterator().next(), path, model);

                // validate there is only 1 resource
                if (resourcesMap.size() != 1)
                    throw new CodecException("One resource should be present in the payload [path:%s]", path);

                node = resourcesMap.values().iterator().next();
            }
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
        return node;
    }

    /**
     * Resolved record then group it by LwM2mPath
     */
    private Map<LwM2mPath, Collection<LwM2mResolvedSenMLRecord>> groupByPath(List<SenMLRecord> records,
            List<LwM2mPath> paths) throws SenMLException {

        // Prepare map result
        Map<LwM2mPath, Collection<LwM2mResolvedSenMLRecord>> result = new HashMap<>(paths.size());
        for (LwM2mPath path : paths) {
            result.put(path, new ArrayList<LwM2mResolvedSenMLRecord>());
        }

        // Resolve record and add it to the map
        LwM2mSenMLResolver resolver = new LwM2mSenMLResolver();
        for (SenMLRecord record : records) {
            LwM2mResolvedSenMLRecord resolvedRecord = resolver.resolve(record);

            // Find the corresponding path for this record.
            LwM2mPath selectedPath = selectPath(resolvedRecord.getPath(), paths);
            if (selectedPath == null) {
                throw new CodecException("Invalid path [%s] for resource, it should start by one of %s",
                        resolvedRecord.getPath(), paths);
            }

            result.get(selectedPath).add(resolvedRecord);
        }
        return result;
    }

    /**
     * Search in the list <code>paths<code> which one is a "start" for the given path.
     * <p>
     * E.g. for recordPath="/3/0/1" and paths=["/1/0","/2/0/","/3"] result will be "/3"
     */
    private LwM2mPath selectPath(LwM2mPath recordPath, List<LwM2mPath> paths) {
        for (LwM2mPath path : paths) {
            if (recordPath.startWith(path)) {
                return path;
            }
        }
        return null;
    }

    /**
     * Resolved record then group it by time-stamp
     *
     * @return a sorted map (timestamp => collection of record) order by descending time-stamp (most recent one at first
     *         place). If null time-stamp (meaning no time information) exists it always at first place.
     */
    private SortedMap<BigDecimal, Collection<LwM2mResolvedSenMLRecord>> groupRecordByTimestamp(
            List<SenMLRecord> records, LwM2mPath requestPath) throws SenMLException {
        SortedMap<BigDecimal, Collection<LwM2mResolvedSenMLRecord>> result = new TreeMap<>(
                new Comparator<BigDecimal>() {
                    @Override
                    public int compare(BigDecimal o1, BigDecimal o2) {
                        // null at first place
                        if (o1 == null) {
                            return o2 == null ? 0 : 1;
                        } else if (o2 == null) {
                            return -1;
                        } else {
                            return o2.compareTo(o1);
                        }
                    }
                });

        LwM2mSenMLResolver resolver = new LwM2mSenMLResolver();
        for (SenMLRecord record : records) {
            LwM2mResolvedSenMLRecord resolvedRecord = resolver.resolve(record);

            // Validate SenML resolved name
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
            result.put(null, emptylist);
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
                return numberToLong((Number) value, permissiveNumberConversion);
            case UNSIGNED_INTEGER:
                return numberToULong((Number) value, permissiveNumberConversion);
            case BOOLEAN:
                return value;
            case FLOAT:
                return numberToDouble((Number) value, permissiveNumberConversion);
            case TIME:
                return new Date(numberToLong((Number) value, permissiveNumberConversion) * 1000L);
            case OPAQUE:
                return value;
            case STRING:
                return value;
            case OBJLNK:
                return ObjectLink.decodeFromString((String) value);
            case CORELINK:
                return linkParser.parseCoreLinkFormat(((String) value).getBytes());
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

        // TODO not sure that guessing type is a good idea...

        // Then senml type
        if (record != null) {
            Type type = guessTypeFromRecord(record);
            if (type != null)
                return type;
        }

        // Else use String as default
        LOG.trace("unknown type for resource use string as default: {}", rscPath);
        return Type.STRING;
    }

    private Type guessTypeFromRecord(SenMLRecord record) {
        SenMLRecord.Type type = record.getType();

        switch (type) {
        case STRING:
            return Type.STRING;
        case OPAQUE:
            return Type.OPAQUE;
        case BOOLEAN:
            return Type.BOOLEAN;
        case OBJLNK:
            return Type.OBJLNK;
        case NUMBER:
            Number numberValue = record.getNumberValue();
            if (numberValue instanceof ULong) {
                return Type.UNSIGNED_INTEGER;
            } else if (numberValue instanceof BigInteger) {
                if (((BigInteger) numberValue).signum() <= 0) {
                    return Type.INTEGER;
                } else {
                    return Type.UNSIGNED_INTEGER;
                }
            } else if (numberValue instanceof Byte || numberValue instanceof Short || numberValue instanceof Integer
                    || numberValue instanceof Long) {
                return Type.INTEGER;
            } else if (numberValue instanceof Float || numberValue instanceof Double
                    || numberValue instanceof BigDecimal) {
                return Type.FLOAT;
            }
        default:
            return null;
        }
    }

    protected Long numberToLong(Number number, boolean permissiveNumberConvertion) {
        return NumberUtil.numberToLong(number, permissiveNumberConvertion);
    }

    protected ULong numberToULong(Number number, boolean permissiveNumberConvertion) {
        return NumberUtil.numberToULong(number, permissiveNumberConvertion);
    }

    protected Double numberToDouble(Number number, boolean permissiveNumberConvertion) {
        return NumberUtil.numberToDouble(number, permissiveNumberConvertion);
    }
}
