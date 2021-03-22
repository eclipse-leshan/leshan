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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mValueConverter;
import org.eclipse.leshan.core.node.codec.MultiNodeEncoder;
import org.eclipse.leshan.core.node.codec.TimestampedNodeEncoder;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.senml.SenMLEncoder;
import org.eclipse.leshan.senml.SenMLException;
import org.eclipse.leshan.senml.SenMLPack;
import org.eclipse.leshan.senml.SenMLRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeSenMLEncoder implements TimestampedNodeEncoder, MultiNodeEncoder {
    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeSenMLEncoder.class);

    private final SenMLEncoder encoder;

    public LwM2mNodeSenMLEncoder(SenMLEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public byte[] encode(LwM2mNode node, LwM2mPath path, LwM2mModel model, LwM2mValueConverter converter) {
        Validate.notNull(node);
        Validate.notNull(path);
        Validate.notNull(model);

        InternalEncoder internalEncoder = new InternalEncoder();
        internalEncoder.objectId = path.getObjectId();
        internalEncoder.model = model;
        internalEncoder.requestPath = path;
        internalEncoder.converter = converter;
        node.accept(internalEncoder);

        SenMLPack pack = new SenMLPack();
        pack.setRecords(internalEncoder.records);

        try {
            return encoder.toSenML(pack);
        } catch (SenMLException e) {
            throw new CodecException(e, "Unable to encode node[path:%s] : %s", path, node);
        }
    }

    @Override
    public byte[] encodeNodes(Map<LwM2mPath, LwM2mNode> nodes, LwM2mModel model, LwM2mValueConverter converter)
            throws CodecException {
        // validate arguments
        Validate.notEmpty(nodes);

        // Encodes nodes to SenML pack
        SenMLPack pack = new SenMLPack();
        for (Entry<LwM2mPath, LwM2mNode> entry : nodes.entrySet()) {
            LwM2mPath path = entry.getKey();
            InternalEncoder internalEncoder = new InternalEncoder();
            internalEncoder.objectId = path.getObjectId();
            internalEncoder.model = model;
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
    public byte[] encodeTimestampedData(List<TimestampedLwM2mNode> timestampedNodes, LwM2mPath path, LwM2mModel model,
            LwM2mValueConverter converter) throws CodecException {
        Validate.notNull(timestampedNodes);
        Validate.notNull(path);
        Validate.notNull(model);

        SenMLPack pack = new SenMLPack();
        for (TimestampedLwM2mNode timestampedLwM2mNode : timestampedNodes) {

            if (timestampedLwM2mNode.getTimestamp() < 268_435_456) {
                // The smallest absolute Time value that can be expressed (2**28) is 1978-07-04 21:24:16 UTC.
                // see https://tools.ietf.org/html/rfc8428#section-4.5.3
                throw new CodecException(
                        "Unable to encode timestamped node[path:%s] : invalid timestamp %s, timestamp should be greater or equals to 268,435,456",
                        path, timestampedLwM2mNode.getTimestamp());
            }

            InternalEncoder internalEncoder = new InternalEncoder();
            internalEncoder.objectId = path.getObjectId();
            internalEncoder.model = model;
            internalEncoder.requestPath = path;
            internalEncoder.converter = converter;
            internalEncoder.records = new ArrayList<>();
            timestampedLwM2mNode.getNode().accept(internalEncoder);
            internalEncoder.records.get(0).setBaseTime(timestampedLwM2mNode.getTimestamp());
            pack.addRecords(internalEncoder.records);
        }

        try {
            return encoder.toSenML(pack);
        } catch (SenMLException e) {
            throw new CodecException(e, "Unable to encode timestamped node[path:%s] : %s", path, timestampedNodes);
        }
    }

    private static class InternalEncoder implements LwM2mNodeVisitor {
        // visitor inputs
        private int objectId;
        private LwM2mModel model;
        private LwM2mPath requestPath;
        private LwM2mValueConverter converter;

        // visitor output
        private ArrayList<SenMLRecord> records = new ArrayList<>();

        @Override
        public void visit(LwM2mObject object) {
            LOG.trace("Encoding Object {} into SenML", object);
            // Validate request path
            if (!requestPath.isObject()) {
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
            // Create SenML record
            SenMLRecord record = new SenMLRecord();

            // Compute baseName and name for SenML record
            String bn = requestPath.toString();
            String n = recordName == null ? "" : recordName;

            if (records.isEmpty()) {
                if (!n.isEmpty()) {
                    bn += "/";
                }
                record.setBaseName(bn);
            }
            record.setName(n);

            // Convert value using expected type
            LwM2mPath lwM2mResourcePath = new LwM2mPath(bn + n);
            Object convertedValue = converter.convertValue(value, valueType, expectedType, lwM2mResourcePath);
            setResourceValue(convertedValue, expectedType, lwM2mResourcePath, record);

            // Add record to the List
            records.add(record);
        }

        private void setResourceValue(Object value, Type type, LwM2mPath resourcePath, SenMLRecord record) {
            LOG.trace("Encoding resource value {} in SenML", value);

            if (type == null || type == Type.NONE) {
                throw new CodecException(
                        "Unable to encode value for resource {} without type(probably a executable one)", resourcePath);
            }

            switch (type) {
            case STRING:
                record.setStringValue((String) value);
                break;
            case INTEGER:
            case UNSIGNED_INTEGER:
            case FLOAT:
                record.setFloatValue((Number) value);
                break;
            case BOOLEAN:
                record.setBooleanValue((Boolean) value);
                break;
            case TIME:
                record.setFloatValue((((Date) value).getTime() / 1000L));
                break;
            case OPAQUE:
                record.setOpaqueValue((byte[]) value);
                break;
            case OBJLNK:
                try {
                    record.setStringValue(((ObjectLink) value).encodeToString());
                } catch (IllegalArgumentException e) {
                    throw new CodecException(e, "Invalid value [%s] for objectLink resource [%s] ", value,
                            resourcePath);
                }
                break;
            default:
                throw new CodecException("Invalid value type %s for %s", type, resourcePath);
            }
        }
    }
}