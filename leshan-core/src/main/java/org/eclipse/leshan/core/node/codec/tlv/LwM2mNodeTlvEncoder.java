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
package org.eclipse.leshan.core.node.codec.tlv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Map.Entry;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mIncompletePath;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mNodeVisitor;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mValueConverter;
import org.eclipse.leshan.core.node.codec.NodeEncoder;
import org.eclipse.leshan.core.tlv.Tlv;
import org.eclipse.leshan.core.tlv.Tlv.TlvType;
import org.eclipse.leshan.core.tlv.TlvEncoder;
import org.eclipse.leshan.core.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TLV encoder for {@link LwM2mNode}.
 */
public class LwM2mNodeTlvEncoder implements NodeEncoder {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeTlvEncoder.class);

    @Override
    public byte[] encode(LwM2mNode node, LwM2mPath path, LwM2mModel model, LwM2mValueConverter converter)
            throws CodecException {
        Validate.notNull(node);
        Validate.notNull(path);
        Validate.notNull(model);

        InternalEncoder internalEncoder = new InternalEncoder();
        internalEncoder.path = path;
        internalEncoder.model = model;
        internalEncoder.converter = converter;
        node.accept(internalEncoder);
        return internalEncoder.out.toByteArray();
    }

    private static class InternalEncoder implements LwM2mNodeVisitor {

        // visitor inputs
        private LwM2mPath path;
        private LwM2mModel model;
        private LwM2mValueConverter converter;

        // visitor output
        private ByteArrayOutputStream out = new ByteArrayOutputStream();

        @Override
        public void visit(LwM2mObject object) {
            LOG.trace("Encoding object {} into TLV", object);

            Tlv[] tlvs;

            // encoded as an array of instances
            tlvs = new Tlv[object.getInstances().size()];
            int i = 0;
            for (Entry<Integer, LwM2mObjectInstance> instance : object.getInstances().entrySet()) {
                Tlv[] resources = encodeResources(instance.getValue().getResources().values(),
                        new LwM2mPath(object.getId(), instance.getKey()));
                tlvs[i] = new Tlv(TlvType.OBJECT_INSTANCE, resources, null, instance.getKey());
                i++;
            }

            try {
                out.write(TlvEncoder.encode(tlvs).array());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void visit(LwM2mObjectInstance instance) {
            LOG.trace("Encoding object instance {} into TLV", instance);

            Tlv[] tlvs;
            if (path.isObjectInstance() || instance.getId() == LwM2mObjectInstance.UNDEFINED) {
                // the instanceId is part of the request path or is undefined
                // so the instance TLV layer is not needed.
                // encoded as an array of resource TLVs
                tlvs = encodeResources(instance.getResources().values(), new LwM2mIncompletePath(path.getObjectId()));
            } else {
                // encoded as an instance TLV
                Tlv[] resources = encodeResources(instance.getResources().values(),
                        new LwM2mPath(path.getObjectId(), instance.getId()));
                tlvs = new Tlv[] { new Tlv(TlvType.OBJECT_INSTANCE, resources, null, instance.getId()) };
            }

            try {
                out.write(TlvEncoder.encode(tlvs).array());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void visit(LwM2mResource resource) {
            LOG.trace("Encoding resource {} into TLV", resource);

            Tlv rTlv = encodeResource(resource, path);

            try {
                out.write(TlvEncoder.encode(new Tlv[] { rTlv }).array());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private Tlv[] encodeResources(Collection<LwM2mResource> resources, LwM2mPath instancePath) {
            Tlv[] rTlvs = new Tlv[resources.size()];
            int i = 0;
            for (LwM2mResource resource : resources) {
                rTlvs[i] = encodeResource(resource, instancePath.append(resource.getId()));
                i++;
            }
            return rTlvs;
        }

        private Tlv encodeResource(LwM2mResource resource, LwM2mPath resourcePath) {
            ResourceModel rSpec = model.getResourceModel(path.getObjectId(), resource.getId());
            Type expectedType = rSpec != null ? rSpec.type : resource.getType();

            Tlv rTlv;
            if (resource.isMultiInstances()) {
                Tlv[] instances = new Tlv[resource.getInstances().size()];
                int i = 0;
                for (LwM2mResourceInstance resourceInstance : resource.getInstances().values()) {
                    LwM2mPath resourceInstancePath = resourcePath.append(resourceInstance.getId());
                    instances[i] = encodeResourceInstance(resourceInstance, resourceInstancePath, expectedType);
                    i++;
                }
                rTlv = new Tlv(TlvType.MULTIPLE_RESOURCE, instances, null, resource.getId());
            } else {
                Object convertedValue = converter.convertValue(resource.getValue(), resource.getType(), expectedType,
                        resourcePath);
                rTlv = new Tlv(TlvType.RESOURCE_VALUE, null,
                        this.encodeTlvValue(convertedValue, expectedType, resourcePath), resource.getId());
            }
            return rTlv;
        }

        @Override
        public void visit(LwM2mResourceInstance resourceInstance) {
            LOG.trace("Encoding resource instance {} into TLV", resourceInstance);

            ResourceModel rSpec = model.getResourceModel(path.getObjectId(), path.getResourceId());
            Type expectedType = rSpec != null ? rSpec.type : resourceInstance.getType();

            Tlv rTlv = encodeResourceInstance(resourceInstance, path, expectedType);

            try {
                out.write(TlvEncoder.encode(new Tlv[] { rTlv }).array());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private Tlv encodeResourceInstance(LwM2mResourceInstance resourceInstance, LwM2mPath resourceInstancePath,
                Type expectedType) {
            Object convertedValue = converter.convertValue(resourceInstance.getValue(), resourceInstance.getType(),
                    expectedType, resourceInstancePath);
            return new Tlv(TlvType.RESOURCE_INSTANCE, null,
                    this.encodeTlvValue(convertedValue, expectedType, resourceInstancePath), resourceInstance.getId());
        }

        private byte[] encodeTlvValue(Object value, Type type, LwM2mPath path) {
            LOG.trace("Encoding value {} in TLV", value);
            if (type == null || type == Type.NONE) {
                throw new CodecException(
                        "Unable to encode value for resource {} without type(probably a executable one)", path);
            }

            try {
                switch (type) {
                case STRING:
                    return TlvEncoder.encodeString((String) value);
                case INTEGER:
                case UNSIGNED_INTEGER:
                    return TlvEncoder.encodeInteger((Number) value);
                case FLOAT:
                    return TlvEncoder.encodeFloat((Number) value);
                case BOOLEAN:
                    return TlvEncoder.encodeBoolean((Boolean) value);
                case TIME:
                    return TlvEncoder.encodeDate((Date) value);
                case OPAQUE:
                    return (byte[]) value;
                case OBJLNK:
                    return TlvEncoder.encodeObjlnk((ObjectLink) value);
                default:
                    throw new CodecException("Invalid value %s for type %s of %s", value, type, path);
                }
            } catch (IllegalArgumentException e) {
                throw new CodecException(e, "Invalid value %s for type %s of %s", value, type, path);
            }
        }
    }
}