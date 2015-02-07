/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.core.node.codec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map.Entry;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mNodeVisitor;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.Value;
import org.eclipse.leshan.core.node.Value.DataType;
import org.eclipse.leshan.core.objectspec.ResourceSpec;
import org.eclipse.leshan.core.objectspec.Resources;
import org.eclipse.leshan.core.objectspec.ResourceSpec.Type;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.tlv.Tlv;
import org.eclipse.leshan.tlv.TlvEncoder;
import org.eclipse.leshan.tlv.Tlv.TlvType;
import org.eclipse.leshan.util.Charsets;
import org.eclipse.leshan.util.StringUtils;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeEncoder {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeEncoder.class);

    /**
     * Serializes a {@link LwM2mNode} with the given content format.
     *
     * @param node the object/instance/resource to serialize
     * @param format the content format
     * @param path the path of the node to serialize
     * @return the encoded node as a byte array
     */
    public static byte[] encode(LwM2mNode node, ContentFormat format, LwM2mPath path) {
        Validate.notNull(node);
        Validate.notNull(format);

        LOG.debug("Encoding node {} for path {} and format {}", node, path, format);

        byte[] encoded = null;
        switch (format) {
        case TLV:
            NodeTlvEncoder tlvEncoder = new NodeTlvEncoder();
            tlvEncoder.objectId = path.getObjectId();
            node.accept(tlvEncoder);
            encoded = tlvEncoder.out.toByteArray();
            break;
        case TEXT:
            NodeTextEncoder textEncoder = new NodeTextEncoder();
            textEncoder.objectId = path.getObjectId();
            node.accept(textEncoder);
            encoded = textEncoder.encoded;
            break;
        case JSON:
            throw new IllegalArgumentException("JSON content format not supported");
        default:
            throw new IllegalArgumentException("Cannot encode " + node + " with format " + format);
        }

        LOG.trace("Encoded node {}: {}", node, Arrays.toString(encoded));
        return encoded;
    }

    private static class NodeTlvEncoder implements LwM2mNodeVisitor {

        int objectId;

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        @Override
        public void visit(LwM2mObject object) {
            // Not needed for now
            throw new UnsupportedOperationException("Object TLV encoding not supported");
        }

        @Override
        public void visit(LwM2mObjectInstance instance) {
            LOG.trace("Encoding object instance {} into TLV", instance);
            // The top-level object instance TLV is not required since a single instance is encoded.
            // The instance will be encoded as an array of resource TLVs.
            for (Entry<Integer, LwM2mResource> resource : instance.getResources().entrySet()) {
                resource.getValue().accept(this);
            }
        }

        @Override
        public void visit(LwM2mResource resource) {
            LOG.trace("Encoding resource {} into TLV", resource);

            ResourceSpec rSpec = Resources.getResourceSpec(objectId, resource.getId());
            Type expectedType = rSpec != null ? rSpec.type : null;

            Tlv rTlv = null;
            if (resource.isMultiInstances()) {
                Tlv[] instances = new Tlv[resource.getValues().length];
                for (int i = 0; i < resource.getValues().length; i++) {
                    instances[i] = new Tlv(TlvType.RESOURCE_INSTANCE, null, this.encodeTlvValue(convertValue(
                            resource.getValues()[i], expectedType)), i);
                }
                rTlv = new Tlv(TlvType.MULTIPLE_RESOURCE, instances, null, resource.getId());
            } else {
                rTlv = new Tlv(TlvType.RESOURCE_VALUE, null, this.encodeTlvValue(convertValue(resource.getValue(),
                        expectedType)), resource.getId());
            }

            try {
                out.write(TlvEncoder.encode(new Tlv[] { rTlv }).array());
            } catch (IOException e) {
                // should not occur
                throw new RuntimeException(e);
            }
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

    private static class NodeTextEncoder implements LwM2mNodeVisitor {

        int objectId;

        byte[] encoded = null;

        @Override
        public void visit(LwM2mObject object) {
            throw new IllegalArgumentException("Object cannot be encoded in text format");
        }

        @Override
        public void visit(LwM2mObjectInstance instance) {
            throw new IllegalArgumentException("Object instance cannot be encoded in text format");
        }

        @Override
        public void visit(LwM2mResource resource) {
            if (resource.isMultiInstances()) {
                throw new IllegalArgumentException("Mulitple instances resource cannot be encoded in text format");
            }
            LOG.trace("Encoding resource {} into text", resource);

            ResourceSpec rSpec = Resources.getResourceSpec(objectId, resource.getId());
            Type expectedType = rSpec != null ? rSpec.type : null;
            Value<?> val = convertValue(resource.getValue(), expectedType);

            String strValue = null;
            switch (val.type) {
            case INTEGER:
            case LONG:
            case DOUBLE:
            case FLOAT:
            case STRING:
                strValue = String.valueOf(val.value);
                break;
            case BOOLEAN:
                strValue = ((Boolean) val.value) ? "1" : "0";
                break;
            case TIME:
                // number of seconds since 1970/1/1
                strValue = String.valueOf(((Date) val.value).getTime() / 1000L);
                break;
            default:
                throw new IllegalArgumentException("Cannot encode " + val + " in text format");
            }

            encoded = strValue.getBytes(Charsets.UTF_8);
        }
    }

    private static Value<?> convertValue(Value<?> value, Type expectedType) {
        if (expectedType == null) {
            // unknown resource, trusted value
            return value;
        }

        Type valueType = toResourceType(value.type);
        if (valueType == expectedType) {
            // expected type
            return value;
        }

        // We received a value with an unexpected type.
        // Let's do some magic to try to convert this value...

        switch (expectedType) {
        case BOOLEAN:
            switch (value.type) {
            case STRING:
                LOG.debug("Trying to convert string value {} to boolean", value.value);
                if (StringUtils.equalsIgnoreCase((String) value.value, "true")) {
                    return Value.newBooleanValue(true);
                } else if (StringUtils.equalsIgnoreCase((String) value.value, "false")) {
                    return Value.newBooleanValue(false);
                }
            case INTEGER:
                LOG.debug("Trying to convert int value {} to boolean", value.value);
                Integer val = (Integer) value.value;
                if (val == 1) {
                    return Value.newBooleanValue(true);
                } else if (val == 0) {
                    return Value.newBooleanValue(false);
                }
            default:
                break;
            }
            break;
        case TIME:
            switch (value.type) {
            case LONG:
                LOG.debug("Trying to convert long value {} to date", value.value);
                // let's assume we received the millisecond since 1970/1/1
                return Value.newDateValue(new Date((Long) value.value));
            case STRING:
                LOG.debug("Trying to convert string value {} to date", value.value);
                // let's assume we received an ISO 8601 format date
                try {
                    return Value.newDateValue(javax.xml.bind.DatatypeConverter.parseDateTime((String) value.value)
                            .getTime());
                } catch (IllegalArgumentException e) {
                    LOG.debug("Unable to convert string to date", e);
                }
            default:
                break;
            }
            break;
        case STRING:
            switch (value.type) {
            case BOOLEAN:
            case INTEGER:
            case LONG:
            case DOUBLE:
            case FLOAT:
                return Value.newStringValue(String.valueOf(value.value));
            default:
                break;
            }
            break;
        case OPAQUE:
            if (value.type == DataType.STRING) {
                // let's assume we received an hexadecimal string
                LOG.debug("Trying to convert hexadecimal string {} to byte array", value.value);
                return Value.newBinaryValue(javax.xml.bind.DatatypeConverter.parseHexBinary((String) value.value));
            }
            break;
        default:
        }

        throw new IllegalArgumentException("Invalid value type, expected " + expectedType + ", got " + valueType);
    }

    private static Type toResourceType(DataType type) {
        switch (type) {
        case INTEGER:
        case LONG:
            return Type.INTEGER;
        case FLOAT:
        case DOUBLE:
            return Type.FLOAT;
        case BOOLEAN:
            return Type.BOOLEAN;
        case OPAQUE:
            return Type.OPAQUE;
        case STRING:
            return Type.STRING;
        case TIME:
            return Type.TIME;
        default:
            throw new IllegalArgumentException("Invalid type: " + type);
        }
    }
}
