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
 *     Gemalto M2M GmbH
 *******************************************************************************/
package org.eclipse.leshan.core.node.codec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.eclipse.leshan.core.node.Value.DataType;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.json.JsonArrayEntry;
import org.eclipse.leshan.json.LwM2mJson;
import org.eclipse.leshan.json.JsonRootObject;
import org.eclipse.leshan.tlv.Tlv;
import org.eclipse.leshan.tlv.Tlv.TlvType;
import org.eclipse.leshan.tlv.TlvEncoder;
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
     * @param model the collection of supported object models
     * @return the encoded node as a byte array
     */
    public static byte[] encode(LwM2mNode node, ContentFormat format, LwM2mPath path, LwM2mModel model) {
        Validate.notNull(node);
        Validate.notNull(format);

        LOG.debug("Encoding node {} for path {} and format {}", node, path, format);

        byte[] encoded = null;
        switch (format) {
        case TLV:
            NodeTlvEncoder tlvEncoder = new NodeTlvEncoder();
            tlvEncoder.objectId = path.getObjectId();
            tlvEncoder.model = model;
            node.accept(tlvEncoder);
            encoded = tlvEncoder.out.toByteArray();
            break;
        case TEXT:
            NodeTextEncoder textEncoder = new NodeTextEncoder();
            textEncoder.objectId = path.getObjectId();
            textEncoder.model = model;
            node.accept(textEncoder);
            encoded = textEncoder.encoded;
            break;
        case OPAQUE:
            NodeOpaqueEncoder opaqueEncoder = new NodeOpaqueEncoder();
            opaqueEncoder.objectId = path.getObjectId();
            opaqueEncoder.model = model;
            node.accept(opaqueEncoder);
            encoded = opaqueEncoder.encoded;
            break;
        case JSON:
            NodeJsonEncoder jsonEncoder = new NodeJsonEncoder();
            jsonEncoder.objectId = path.getObjectId();
            jsonEncoder.model = model;
            node.accept(jsonEncoder);
            encoded = jsonEncoder.encoded;
            break;
        default:
            throw new IllegalArgumentException("Cannot encode " + node + " with format " + format);
        }

        LOG.trace("Encoded node {}: {}", node, Arrays.toString(encoded));
        return encoded;
    }

    private static class NodeTlvEncoder implements LwM2mNodeVisitor {

        int objectId;
        LwM2mModel model;

        ByteArrayOutputStream out = new ByteArrayOutputStream();

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
                    instances[i] = new Tlv(TlvType.RESOURCE_INSTANCE, null, this.encodeTlvValue(convertValue(
                            resource.getValues()[i], expectedType)), i);
                }
                rTlv = new Tlv(TlvType.MULTIPLE_RESOURCE, instances, null, resource.getId());
            } else {
                rTlv = new Tlv(TlvType.RESOURCE_VALUE, null, this.encodeTlvValue(convertValue(resource.getValue(),
                        expectedType)), resource.getId());
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

    private static class NodeTextEncoder implements LwM2mNodeVisitor {

        int objectId;
        LwM2mModel model;

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

            ResourceModel rSpec = model.getResourceModel(objectId, resource.getId());
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

    private static class NodeOpaqueEncoder implements LwM2mNodeVisitor {

        int objectId;
        LwM2mModel model;

        byte[] encoded = null;

        @Override
        public void visit(LwM2mObject object) {
            throw new IllegalArgumentException("Object cannot be encoded in opaque format");
        }

        @Override
        public void visit(LwM2mObjectInstance instance) {
            throw new IllegalArgumentException("Object instance cannot be encoded in opaque format");
        }

        @Override
        public void visit(LwM2mResource resource) {
            if (resource.isMultiInstances()) {
                throw new IllegalArgumentException("Mulitple instances resource cannot be encoded in opaque format");
            }
            ResourceModel rSpec = model.getResourceModel(objectId, resource.getId());
            if (rSpec != null && rSpec.type != Type.OPAQUE) {
                throw new IllegalArgumentException("Only single opaque resource can be encoded in opaque format");
            }
            LOG.trace("Encoding resource {} into text", resource);
            Value<?> val = convertValue(resource.getValue(), Type.OPAQUE);
            encoded = (byte[]) val.value;
        }
    }

    private static class NodeJsonEncoder implements LwM2mNodeVisitor {

        int objectId;
        LwM2mModel model;

        byte[] encoded = null;

        @Override
        public void visit(LwM2mObject object) {
            // TODO needed to encode multiple instances
            // How to deal with ObjLink
            throw new UnsupportedOperationException("Object JSON encoding not supported");
        }

        @Override
        public void visit(LwM2mObjectInstance instance) {
            LOG.trace("Encoding object instance {} into JSON", instance);
            JsonRootObject jsonObject = null;
            ArrayList<JsonArrayEntry> resourceList = new ArrayList<>();
            for (Entry<Integer, LwM2mResource> resource : instance.getResources().entrySet()) {
                LwM2mResource res = resource.getValue();
                resourceList.addAll(lwM2mResourceToJsonArrayEntry(res));
            }
            jsonObject = new JsonRootObject(resourceList);
            String json = LwM2mJson.toJsonLwM2m(jsonObject);
            encoded = json.getBytes();
        }

        @Override
        public void visit(LwM2mResource resource) {
            LOG.trace("Encoding resource {} into JSON", resource);
            JsonRootObject jsonObject = null;
            jsonObject = new JsonRootObject(lwM2mResourceToJsonArrayEntry(resource));
            String json = LwM2mJson.toJsonLwM2m(jsonObject);
            encoded = json.getBytes();
        }

        private ArrayList<JsonArrayEntry> lwM2mResourceToJsonArrayEntry(LwM2mResource resource) {
            ResourceModel rSpec = model.getResourceModel(objectId, resource.getId());
            Type expectedType = rSpec != null ? rSpec.type : null;
            ArrayList<JsonArrayEntry> resourcesList = new ArrayList<>();
            if (resource.isMultiInstances()) {
                for (int i = 0; i < resource.getValues().length; i++) {
                    JsonArrayEntry jsonResourceElt = new JsonArrayEntry();
                    jsonResourceElt.setName(resource.getId() + "/" + i);
                    this.setResourceValue(convertValue(resource.getValues()[i], expectedType), jsonResourceElt);
                    resourcesList.add(jsonResourceElt);
                }
            } else {
                JsonArrayEntry jsonResourceElt = new JsonArrayEntry();
                jsonResourceElt.setName(new StringBuffer().append(resource.getId()).toString());
                this.setResourceValue(convertValue(resource.getValue(), expectedType), jsonResourceElt);
                resourcesList.add(jsonResourceElt);
            }
            return resourcesList;
        }

        private void setResourceValue(Value<?> value, JsonArrayEntry jsonResource) {
            LOG.trace("Encoding value {} in JSON", value);
            // Following table 20 in the Specs
            switch (value.type) {
            case STRING:
                jsonResource.setStringValue((String) value.value);
                break;
            case INTEGER:
            case FLOAT:
                jsonResource.setFloatValue((Number) value.value);
                break;
            case LONG:
            case DOUBLE:
                jsonResource.setStringValue(String.valueOf(value.value));
                break;
            case BOOLEAN:
                jsonResource.setBooleanValue((Boolean) value.value);
                break;
            case TIME:
                // Specs device object example page 44, rec 13 is Time
                // represented as float?
                jsonResource.setFloatValue((((Date) value.value).getTime() / 1000L));
                break;
            case OPAQUE:
                jsonResource.setStringValue(javax.xml.bind.DatatypeConverter.printBase64Binary((byte[]) value.value));
            default:
                throw new IllegalArgumentException("Invalid value type: " + value.type);
            }
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
