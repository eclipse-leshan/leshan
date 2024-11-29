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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.leshan.core.link.DefaultLinkSerializer;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.LinkSerializer;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mNodeVisitor;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mRoot;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mValueConverter;
import org.eclipse.leshan.core.node.codec.MultiNodeEncoder;
import org.eclipse.leshan.core.node.codec.TimestampedMultiNodeEncoder;
import org.eclipse.leshan.core.node.codec.TimestampedNodeEncoder;
import org.eclipse.leshan.core.util.TimestampUtil;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.senml.SenMLEncoder;
import org.eclipse.leshan.senml.SenMLException;
import org.eclipse.leshan.senml.SenMLPack;
import org.eclipse.leshan.senml.SenMLRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeSenMLEncoder implements TimestampedNodeEncoder, MultiNodeEncoder, TimestampedMultiNodeEncoder {
    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeSenMLEncoder.class);

    private final SenMLEncoder encoder;
    private final LinkSerializer linkSerializer;

    public LwM2mNodeSenMLEncoder(SenMLEncoder encoder) {
        this(encoder, new DefaultLinkSerializer());
    }

    public LwM2mNodeSenMLEncoder(SenMLEncoder encoder, LinkSerializer linkSerializer) {
        this.encoder = encoder;
        this.linkSerializer = linkSerializer;
    }

    @Override
    public byte[] encode(LwM2mNode node, String rootPath, LwM2mPath path, LwM2mModel model,
            LwM2mValueConverter converter) {
        Validate.notNull(node);
        Validate.notNull(path);
        Validate.notNull(model);

        InternalEncoder internalEncoder = new InternalEncoder();
        internalEncoder.objectId = path.getObjectId();
        internalEncoder.model = model;
        internalEncoder.rootPath = rootPath;
        internalEncoder.requestPath = path;
        internalEncoder.converter = converter;
        node.accept(internalEncoder);

        SenMLPack pack = new SenMLPack(internalEncoder.records);

        try {
            return encoder.toSenML(pack);
        } catch (SenMLException e) {
            throw new CodecException(e, "Unable to encode node[path:%s] : %s", path, node);
        }
    }

    @Override
    public byte[] encodeNodes(String rootPath, Map<LwM2mPath, LwM2mNode> nodes, LwM2mModel model,
            LwM2mValueConverter converter) throws CodecException {
        // validate arguments
        Validate.notEmpty(nodes);

        // Encodes nodes to SenML pack
        SenMLPack pack = new SenMLPack();
        for (Entry<LwM2mPath, LwM2mNode> entry : nodes.entrySet()) {
            LwM2mPath path = entry.getKey();
            InternalEncoder internalEncoder = new InternalEncoder();
            internalEncoder.objectId = path.getObjectId();
            internalEncoder.model = model;
            internalEncoder.rootPath = rootPath;
            internalEncoder.requestPath = path;
            internalEncoder.converter = converter;
            internalEncoder.records = new ArrayList<>();
            LwM2mNode node = entry.getValue();
            if (node != null) {
                node.accept(internalEncoder);
                pack.addRecords(internalEncoder.records);
            }
            // else
            // We just ignore null node as the LWM2M specification says that "Read-Composite operation is treated as
            // non-atomic and handled as best effort by the client. That is, if any of the requested resources do not
            // have a valid value to return, they will not be included in the response".
            // Meaning that a given path could have no corresponding value.
        }

        // Encodes SenML pack using internal encoder (it could be SenML-JSON or SenML-CBOR encoder)
        try {
            return encoder.toSenML(pack);
        } catch (SenMLException e) {
            throw new CodecException(e, "Unable to encode multi node[paths:%s] : %s", nodes.keySet(), nodes);
        }
    }

    @Override
    public byte[] encodeTimestampedData(List<TimestampedLwM2mNode> timestampedNodes, String rootPath, LwM2mPath path,
            LwM2mModel model, LwM2mValueConverter converter) throws CodecException {
        Validate.notNull(timestampedNodes);
        Validate.notNull(path);
        Validate.notNull(model);

        SenMLPack pack = new SenMLPack();
        for (TimestampedLwM2mNode timestampedLwM2mNode : timestampedNodes) {

            if (timestampedLwM2mNode.isTimestamped()
                    && timestampedLwM2mNode.getTimestamp().getEpochSecond() < 268_435_456) {
                // The smallest absolute Time value that can be expressed (2**28) is 1978-07-04 21:24:16 UTC.
                // see https://tools.ietf.org/html/rfc8428#section-4.5.3
                throw new CodecException(
                        "Unable to encode timestamped node[path:%s] : invalid timestamp %s, timestamp should be greater or equals to 268,435,456",
                        path, timestampedLwM2mNode.getTimestamp());
            }

            InternalEncoder internalEncoder = new InternalEncoder();
            internalEncoder.objectId = path.getObjectId();
            internalEncoder.model = model;
            internalEncoder.rootPath = rootPath;
            internalEncoder.requestPath = path;
            internalEncoder.converter = converter;
            internalEncoder.records = new ArrayList<>();
            timestampedLwM2mNode.getNode().accept(internalEncoder);
            BigDecimal timestampInSeconds = TimestampUtil.fromInstant(timestampedLwM2mNode.getTimestamp());

            SenMLRecord record = internalEncoder.records.get(0);
            SenMLRecord timestampedrecord = new SenMLRecord(record.getBaseName(), timestampInSeconds, record.getName(),
                    record.getTime(), record.getNumberValue(), record.getBooleanValue(), record.getObjectLinkValue(),
                    record.getStringValue(), record.getOpaqueValue());
            internalEncoder.records.set(0, timestampedrecord);
            pack.addRecords(internalEncoder.records);
        }

        try {
            return encoder.toSenML(pack);
        } catch (SenMLException e) {
            throw new CodecException(e, "Unable to encode timestamped node[path:%s] : %s", path, timestampedNodes);
        }
    }

    @Override
    public byte[] encodeTimestampedNodes(String rootPath, TimestampedLwM2mNodes timestampedNodes, LwM2mModel model,
            LwM2mValueConverter converter) throws CodecException {
        Validate.notEmpty(timestampedNodes.getTimestamps());

        SenMLPack pack = new SenMLPack();
        for (Instant timestamp : timestampedNodes.getTimestamps()) {
            Map<LwM2mPath, LwM2mNode> nodesAtTimestamp = timestampedNodes.getNodesAt(timestamp);
            for (Entry<LwM2mPath, LwM2mNode> entry : nodesAtTimestamp.entrySet()) {
                LwM2mPath path = entry.getKey();
                InternalEncoder internalEncoder = new InternalEncoder();
                internalEncoder.objectId = path.getObjectId();
                internalEncoder.model = model;
                internalEncoder.rootPath = rootPath;
                internalEncoder.requestPath = path;
                internalEncoder.converter = converter;
                internalEncoder.records = new ArrayList<>();
                LwM2mNode node = entry.getValue();
                if (node != null) {
                    node.accept(internalEncoder);

                    List<SenMLRecord> records = internalEncoder.records;
                    if (!records.isEmpty()) {
                        SenMLRecord record = internalEncoder.records.get(0);
                        SenMLRecord timestampedrecord = new SenMLRecord(record.getBaseName(),
                                TimestampUtil.fromInstant(timestamp), record.getName(), record.getTime(),
                                record.getNumberValue(), record.getBooleanValue(), record.getObjectLinkValue(),
                                record.getStringValue(), record.getOpaqueValue());
                        internalEncoder.records.set(0, timestampedrecord);

                        pack.addRecords(records);
                    }
                }
            }
        }

        try {
            return encoder.toSenML(pack);
        } catch (SenMLException e) {
            throw new CodecException(e, "Unable to encode timestamped nodes: %s", timestampedNodes);
        }
    }

    private class InternalEncoder implements LwM2mNodeVisitor {
        // visitor inputs
        private Integer objectId;
        private LwM2mModel model;
        private LwM2mPath requestPath;
        private LwM2mValueConverter converter;
        private String rootPath;

        // visitor output
        private ArrayList<SenMLRecord> records = new ArrayList<>();

        @Override
        public void visit(LwM2mRoot root) {
            LOG.trace("Encoding Root into SenML");
            // Validate request path
            if (!requestPath.isRoot()) {
                throw new CodecException("Invalid request path %s for root encoding", requestPath);
            }

            // Create SenML records
            for (LwM2mObject object : root.getObjects().values()) {
                for (LwM2mObjectInstance instance : object.getInstances().values()) {
                    for (LwM2mResource resource : instance.getResources().values()) {
                        String prefixPath = object.getId() + "/" + instance.getId() + "/" + resource.getId();
                        this.objectId = object.getId();
                        lwM2mResourceToSenMLRecord(prefixPath, resource);
                    }
                }
            }
        }

        @Override
        public void visit(LwM2mObject object) {
            LOG.trace("Encoding Object {} into SenML", object);
            // Validate request path
            if (requestPath.isRoot()) {
                throw new CodecException("Invalid request path %s for root encoding", requestPath);
            } else if (!requestPath.isObject()) {
                throw new CodecException("Invalid request path %s for object encoding", requestPath);
            }

            // Create SenML records
            for (LwM2mObjectInstance instance : object.getInstances().values()) {
                for (LwM2mResource resource : instance.getResources().values()) {
                    String prefixPath = Integer.toString(instance.getId()) + "/" + Integer.toString(resource.getId());
                    lwM2mResourceToSenMLRecord(prefixPath, resource);
                }
            }
        }

        @Override
        public void visit(LwM2mObjectInstance instance) {
            LOG.trace("Encoding object instance {} into SenML", instance);
            for (LwM2mResource resource : instance.getResources().values()) {
                // Validate request path & compute resource path
                String prefixPath;
                if (requestPath.isObject()) {
                    prefixPath = instance.getId() + "/" + resource.getId();
                } else if (requestPath.isObjectInstance()) {
                    prefixPath = Integer.toString(resource.getId());
                } else {
                    throw new CodecException("Invalid request path %s for instance encoding", requestPath);
                }
                // Create SenML records
                lwM2mResourceToSenMLRecord(prefixPath, resource);
            }
        }

        @Override
        public void visit(LwM2mResource resource) {
            LOG.trace("Encoding resource {} into SenML JSON", resource);
            if (!requestPath.isResource()) {
                throw new CodecException("Invalid request path %s for resource encoding", requestPath);
            }

            // Using request path as base name, and record doesn't have name
            lwM2mResourceToSenMLRecord(null, resource);
        }

        @Override
        public void visit(LwM2mResourceInstance resourceInstance) {
            LOG.trace("Encoding resource instance {} into SenML", resourceInstance);
            if (!requestPath.isResourceInstance()) {
                throw new CodecException("Invalid request path %s for resource  instance encoding", requestPath);
            }

            // get type for this resource
            ResourceModel rSpec = model.getResourceModel(objectId, requestPath.getResourceId());
            Type expectedType = rSpec != null ? rSpec.type : resourceInstance.getType();

            // Using request path as base name, and record doesn't have name
            addSenMLRecord(null, resourceInstance.getType(), expectedType, resourceInstance.getValue());
        }

        private void lwM2mResourceToSenMLRecord(String recordName, LwM2mResource resource) {
            // get type for this resource
            ResourceModel rSpec = model.getResourceModel(objectId, resource.getId());
            Type expectedType = rSpec != null ? rSpec.type : resource.getType();

            // create resource element
            if (resource.isMultiInstances()) {
                for (Entry<Integer, LwM2mResourceInstance> entry : resource.getInstances().entrySet()) {
                    // compute record name for resource instance
                    String resourceInstanceRecordName;
                    if (recordName == null || recordName.isEmpty()) {
                        resourceInstanceRecordName = Integer.toString(entry.getKey());
                    } else {
                        resourceInstanceRecordName = recordName + "/" + entry.getKey();
                    }

                    addSenMLRecord(resourceInstanceRecordName, resource.getType(), expectedType,
                            entry.getValue().getValue());
                }
            } else {
                addSenMLRecord(recordName, resource.getType(), expectedType, resource.getValue());
            }
        }

        private void addSenMLRecord(String recordName, Type valueType, Type expectedType, Object value) {
            // Create SenML record attributes
            String recordbasename = null;
            String recordname = null;

            // Compute baseName and name for SenML record
            String bn = requestPath.toString();
            String n = recordName == null ? "" : recordName;

            // Add slash if necessary
            if (!n.isEmpty() && !bn.equals("/")) {
                bn += "/";
            }

            // Set basename only for first record
            if (records.isEmpty()) {
                recordbasename = (rootPath != null ? rootPath + bn : bn);
            }
            recordname = n;

            // Convert value using expected type
            LwM2mPath lwM2mResourcePath = new LwM2mPath(bn + n);
            Object convertedValue = converter.convertValue(value, valueType, expectedType, lwM2mResourcePath);
            SenMLRecord record = setResourceValue(convertedValue, expectedType, lwM2mResourcePath, recordbasename,
                    recordname);

            // Add record to the List
            records.add(record);
        }

        private SenMLRecord setResourceValue(Object value, Type type, LwM2mPath resourcePath, String recordBaseName,
                String recordName) {
            LOG.trace("Encoding resource value {} in SenML", value);

            if (type == null || type == Type.NONE) {
                throw new CodecException(
                        "Unable to encode value for resource {} without type(probably a executable one)", resourcePath);
            }
            String recordStringValue = null;
            Number recordNumberValue = null;
            Boolean recordBooleanValue = null;
            byte[] recordopaqueValue = null;

            switch (type) {
            case STRING:
                recordStringValue = (String) value;
                break;
            case INTEGER:
            case UNSIGNED_INTEGER:
            case FLOAT:
                recordNumberValue = (Number) value;
                break;
            case BOOLEAN:
                recordBooleanValue = (Boolean) value;
                break;
            case TIME:
                recordNumberValue = ((Date) value).getTime() / 1000L;
                break;
            case OPAQUE:
                recordopaqueValue = (byte[]) value;
                break;
            case OBJLNK:
                try {
                    recordStringValue = ((ObjectLink) value).encodeToString();
                } catch (IllegalArgumentException e) {
                    throw new CodecException(e, "Invalid value [%s] for objectLink resource [%s] ", value,
                            resourcePath);
                }
                break;

            case CORELINK:
                recordStringValue = linkSerializer.serializeCoreLinkFormat((Link[]) value);
                break;
            default:
                throw new CodecException("Invalid value type %s for %s", type, resourcePath);
            }
            return new SenMLRecord(recordBaseName, null, recordName, null, recordNumberValue, recordBooleanValue, null,
                    recordStringValue, recordopaqueValue);
        }
    }
}
