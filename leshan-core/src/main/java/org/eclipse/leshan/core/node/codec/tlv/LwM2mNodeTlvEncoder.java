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
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mValueConverter;
import org.eclipse.leshan.core.tlv.Tlv;
import org.eclipse.leshan.core.tlv.Tlv.TlvType;
import org.eclipse.leshan.core.tlv.TlvEncoder;
import org.eclipse.leshan.core.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TLV encoder for {@link LwM2mNode}.
 */
public class LwM2mNodeTlvEncoder {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeTlvEncoder.class);

    public static byte[] encode(LwM2mNode node, LwM2mPath path, LwM2mModel model, LwM2mValueConverter converter)
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
                Tlv[] instances = new Tlv[resource.getValues().size()];
                int i = 0;
                for (Entry<Integer, ?> entry : resource.getValues().entrySet()) {
                    LwM2mPath resourceInstancePath = resourcePath.append(entry.getKey());
                    Object convertedValue = converter.convertValue(entry.getValue(), resource.getType(), expectedType,
                            resourceInstancePath);
                    instances[i] = new Tlv(TlvType.RESOURCE_INSTANCE, null,
                            this.encodeTlvValue(convertedValue, expectedType, resourceInstancePath), entry.getKey());
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