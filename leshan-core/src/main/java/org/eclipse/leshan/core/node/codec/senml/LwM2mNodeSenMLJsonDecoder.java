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
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.senml.SenMLJson;
import org.eclipse.leshan.senml.SenMLPack;
import org.eclipse.leshan.senml.SenMLRecord;
import org.eclipse.leshan.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeSenMLJsonDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeSenMLJsonDecoder.class);

    public static <T extends LwM2mNode> T decode(byte[] content, LwM2mPath path, LwM2mModel model, Class<T> nodeClass)
            throws CodecException {
        String jsonStrValue = content != null ? new String(content) : "";
        SenMLPack pack = SenMLJson.fromJsonSenML(jsonStrValue);
        return parseSenMLPack(pack, path, model, nodeClass);
    }

    @SuppressWarnings("unchecked")
    private static <T extends LwM2mNode> T parseSenMLPack(SenMLPack pack, LwM2mPath path, LwM2mModel model,
            Class<T> nodeClass) throws CodecException {

        LOG.trace("Parsing SenML JSON object for path {}: {}", path, pack);

        // Extract baseName
        LwM2mPath baseName = extractAndValidateBaseName(pack, path);

        // Group records by instance
        Map<Integer, Collection<SenMLRecord>> recordsByInstanceId = groupRecordsByInstanceId(pack.getRecords());

        // Create lwm2m node
        LwM2mNode node = null;
        if (nodeClass == LwM2mObject.class) {
            Collection<LwM2mObjectInstance> instances = new ArrayList<>();
            for (Entry<Integer, Collection<SenMLRecord>> entryByInstanceId : recordsByInstanceId.entrySet()) {
                Map<Integer, LwM2mResource> resourcesMap = extractLwM2mResources(entryByInstanceId.getValue(), baseName,
                        model);

                instances.add(new LwM2mObjectInstance(entryByInstanceId.getKey(), resourcesMap.values()));
            }

            node = new LwM2mObject(baseName.getObjectId(), instances);
        } else if (nodeClass == LwM2mObjectInstance.class) {
            // validate we have resources for only 1 instance
            if (recordsByInstanceId.size() != 1)
                throw new CodecException("One instance expected in the payload [path:%s]", path);

            // Extract resources
            Entry<Integer, Collection<SenMLRecord>> instanceEntry = recordsByInstanceId.entrySet().iterator().next();
            Map<Integer, LwM2mResource> resourcesMap = extractLwM2mResources(instanceEntry.getValue(), baseName, model);

            // Create instance
            node = new LwM2mObjectInstance(instanceEntry.getKey(), resourcesMap.values());
        } else if (nodeClass == LwM2mResource.class) {
            // validate we have resources for only 1 instance
            if (recordsByInstanceId.size() > 1)
                throw new CodecException("Only one instance expected in the payload [path:%s]", path);

            // Extract resources
            Map<Integer, LwM2mResource> resourcesMap = extractLwM2mResources(
                    recordsByInstanceId.values().iterator().next(), baseName, model);

            // validate there is only 1 resource
            if (resourcesMap.size() != 1)
                throw new CodecException("One resource should be present in the payload [path:%s]", path);

            node = resourcesMap.values().iterator().next();
        } else {
            throw new IllegalArgumentException("invalid node class: " + nodeClass);
        }

        return (T) node;
    }

    /**
     * Extract and validate base name from SenML pack, and update name field with full path for each of SenML Record.
     * 
     * @param pack
     * @param requestPath
     * @return
     * @throws CodecException
     */
    private static LwM2mPath extractAndValidateBaseName(SenMLPack pack, LwM2mPath requestPath) throws CodecException {
        String baseName = null;
        for (SenMLRecord record : pack.getRecords()) {
            if (record.getBaseName() != null && !record.getBaseName().isEmpty()) {
                baseName = record.getBaseName();
                break;
            }
        }

        if (baseName != null && !baseName.isEmpty()) {
            for (SenMLRecord record : pack.getRecords()) {
                if (record.getName() != null && !record.getName().isEmpty()) {
                    record.setName(baseName + record.getName());
                } else {
                    record.setName(baseName);
                }
            }
        }

        // Check baseName is valid
        if (baseName != null && !baseName.isEmpty()) {
            LwM2mPath bnPath = new LwM2mPath(baseName);

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

    /**
     * Group all SenML record by instanceId
     * 
     * @param records
     *
     * @return a map (instanceId => collection of SenML Record)
     */
    private static Map<Integer, Collection<SenMLRecord>> groupRecordsByInstanceId(Collection<SenMLRecord> records)
            throws CodecException {
        Map<Integer, Collection<SenMLRecord>> result = new HashMap<>();

        for (SenMLRecord record : records) {
            // Build resource path
            LwM2mPath nodePath = new LwM2mPath(record.getName());

            // Validate path
            if (!nodePath.isResourceInstance() && !nodePath.isResource()) {
                throw new CodecException(
                        "Invalid path [%s] for resource, it should be a resource or a resource instance path",
                        nodePath);
            }

            // Get SenML records for this instance
            Collection<SenMLRecord> recordForInstance = result.get(nodePath.getObjectInstanceId());
            if (recordForInstance == null) {
                recordForInstance = new ArrayList<>();
                result.put(nodePath.getObjectInstanceId(), recordForInstance);
            }

            // Add it to the list
            recordForInstance.add(record);
        }

        return result;
    }

    private static Map<Integer, LwM2mResource> extractLwM2mResources(Collection<SenMLRecord> records,
            LwM2mPath baseName, LwM2mModel model) throws CodecException {
        if (records == null)
            return Collections.emptyMap();

        // Extract LWM2M resources from JSON resource list
        Map<Integer, LwM2mResource> lwM2mResourceMap = new HashMap<>();
        Map<LwM2mPath, Map<Integer, SenMLRecord>> multiResourceMap = new HashMap<>();

        for (SenMLRecord record : records) {
            // Build resource path
            LwM2mPath nodePath = new LwM2mPath(record.getName());

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

    private static Object parseResourceValue(Object value, Type expectedType, LwM2mPath path) throws CodecException {
        LOG.trace("Parse SenML JSON value for path {} and expected type {}: {}", path, expectedType, value);

        try {
            switch (expectedType) {
            case INTEGER:
                return ((Number) value).longValue();
            case FLOAT:
                return ((Number) value).doubleValue();
            case BOOLEAN:
                return value;
            case TIME:
                return new Date(((Number) value).longValue() * 1000L);
            case OPAQUE:
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

    public static Type getResourceType(LwM2mPath rscPath, LwM2mModel model, SenMLRecord record) {
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
}