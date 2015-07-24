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
package org.eclipse.leshan.core.node.codec.tlv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Map.Entry;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mNodeVisitor;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.Value;
import org.eclipse.leshan.core.node.codec.Lwm2mNodeEncoderUtil;
import org.eclipse.leshan.tlv.Tlv;
import org.eclipse.leshan.tlv.Tlv.TlvType;
import org.eclipse.leshan.tlv.TlvEncoder;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeTlvEncoder {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeTlvEncoder.class);

    public static byte[] encode(LwM2mNode node, LwM2mPath path, LwM2mModel model) {
        Validate.notNull(node);
        Validate.notNull(path);
        Validate.notNull(model);

        InternalEncoder internalEncoder = new InternalEncoder();
        internalEncoder.objectId = path.getObjectId();
        internalEncoder.model = model;
        node.accept(internalEncoder);
        return internalEncoder.out.toByteArray();
    }

    private static class InternalEncoder implements LwM2mNodeVisitor {

        // visitor inputs
        private int objectId;
        private LwM2mModel model;

        // visitor output
        private ByteArrayOutputStream out = new ByteArrayOutputStream();

        @Override
        public void visit(LwM2mObject object) {
            LOG.trace("Encoding object instances {} into TLV", object);

            Tlv[] tlvs = null;

            ObjectModel objectModel = model.getObjectModel(object.getId());
            if (objectModel != null && !objectModel.multiple) {
                // single instance object, the instance is level is not needed
                tlvs = encodeResources(object.getInstances().get(0).getResources().values());
            } else {
                tlvs = new Tlv[object.getInstances().size()];
                int i = 0;
                for (Entry<Integer, LwM2mObjectInstance> instance : object.getInstances().entrySet()) {
                    Tlv[] resources = encodeResources(instance.getValue().getResources().values());
                    tlvs[i] = new Tlv(TlvType.OBJECT_INSTANCE, resources, null, instance.getKey());
                    i++;
                }
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

            // The instance is encoded as an array of resource TLVs.
            Tlv[] rTlvs = encodeResources(instance.getResources().values());

            try {
                out.write(TlvEncoder.encode(rTlvs).array());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void visit(LwM2mResource resource) {
            LOG.trace("Encoding resource {} into TLV", resource);

            Tlv rTlv = encodeResource(resource);

            try {
                out.write(TlvEncoder.encode(new Tlv[] { rTlv }).array());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private Tlv[] encodeResources(Collection<LwM2mResource> resources) {
            Tlv[] rTlvs = new Tlv[resources.size()];
            int i = 0;
            for (LwM2mResource resource : resources) {
                rTlvs[i] = encodeResource(resource);
                i++;
            }
            return rTlvs;
        }

        private Tlv encodeResource(LwM2mResource resource) {
            ResourceModel rSpec = model.getResourceModel(objectId, resource.getId());
            Type expectedType = rSpec != null ? rSpec.type : null;

            Tlv rTlv = null;
            if (resource.isMultiInstances()) {
                Tlv[] instances = new Tlv[resource.getValues().length];
                for (int i = 0; i < resource.getValues().length; i++) {
                    instances[i] = new Tlv(TlvType.RESOURCE_INSTANCE, null, this.encodeTlvValue(Lwm2mNodeEncoderUtil
                            .convertValue(resource.getValues()[i], expectedType)), i);
                }
                rTlv = new Tlv(TlvType.MULTIPLE_RESOURCE, instances, null, resource.getId());
            } else {
                rTlv = new Tlv(TlvType.RESOURCE_VALUE, null, this.encodeTlvValue(Lwm2mNodeEncoderUtil.convertValue(
                        resource.getValue(), expectedType)), resource.getId());
            }
            return rTlv;
        }

        private byte[] encodeTlvValue(Value<?> value) {
            LOG.trace("Encoding value {} in TLV", value);
            switch (value.type) {
            case STRING:
                return TlvEncoder.encodeString((String) value.value);
            case INTEGER:
            case LONG:
                return TlvEncoder.encodeInteger((Number) value.value);
            case FLOAT:
            case DOUBLE:
                return TlvEncoder.encodeFloat((Number) value.value);
            case BOOLEAN:
                return TlvEncoder.encodeBoolean((Boolean) value.value);
            case TIME:
                return TlvEncoder.encodeDate((Date) value.value);
            case OPAQUE:
                return (byte[]) value.value;
            default:
                throw new IllegalArgumentException("Invalid value type: " + value.type);
            }
        }
    }
}