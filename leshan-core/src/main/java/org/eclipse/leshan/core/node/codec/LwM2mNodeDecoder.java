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

import java.nio.ByteBuffer;
import java.util.Date;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.Value;
import org.eclipse.leshan.core.objectspec.ResourceSpec;
import org.eclipse.leshan.core.objectspec.Resources;
import org.eclipse.leshan.core.objectspec.ResourceSpec.Type;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.tlv.Tlv;
import org.eclipse.leshan.tlv.TlvDecoder;
import org.eclipse.leshan.tlv.TlvException;
import org.eclipse.leshan.util.Charsets;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeDecoder.class);

    /**
     * Deserializes a binary content into a {@link LwM2mNode}.
     *
     * @param content the content
     * @param format the content format
     * @param path the path of the node to build
     * @return the resulting node
     * @throws InvalidValueException
     */
    public static LwM2mNode decode(byte[] content, ContentFormat format, LwM2mPath path) throws InvalidValueException {
        LOG.debug("Decoding value for path {} and format {}: {}", path, format, content);

        Validate.notNull(path);

        // default to plain/text
        if (format == null) {
            if (path.isResource()) {
                ResourceSpec rDesc = Resources.getResourceSpec(path.getObjectId(), path.getResourceId());
                if (rDesc != null && rDesc.multiple) {
                    format = ContentFormat.TLV;
                } else {
                    format = ContentFormat.TEXT;
                }
            } else {
                // HACK: client should return a content type
                // but specific lwm2m ones are not yet defined
                format = ContentFormat.TLV;
            }
        }

        switch (format) {
        case TEXT:
            // single resource value
            Validate.notNull(path.getResourceId());
            ResourceSpec rDesc = Resources.getResourceSpec(path.getObjectId(), path.getResourceId());

            String strValue = new String(content, Charsets.UTF_8);
            Value<?> value = null;
            if (rDesc != null) {
                value = parseTextValue(strValue, rDesc.type, path);
            } else {
                // unknown resource, returning a default string value
                value = Value.newStringValue(strValue);
            }
            return new LwM2mResource(path.getResourceId(), value);

        case TLV:
            try {
                Tlv[] tlvs = TlvDecoder.decode(ByteBuffer.wrap(content));
                return parseTlv(tlvs, path);
            } catch (TlvException e) {
                throw new InvalidValueException("Unable to decode tlv.", path, e);
            }
        case JSON:
        case LINK:
        case OPAQUE:
            throw new InvalidValueException("Content format " + format + " not yet implemented", path);
        }
        return null;

    }

    private static Value<?> parseTextValue(String value, Type type, LwM2mPath path) throws InvalidValueException {
        LOG.trace("TEXT value for path {} and expected type {}: {}", path, type, value);

        try {
            switch (type) {
            case STRING:
                return Value.newStringValue(value);
            case INTEGER:
                try {
                    Long lValue = Long.valueOf(value);
                    if (lValue >= Integer.MIN_VALUE && lValue <= Integer.MAX_VALUE) {
                        return Value.newIntegerValue(lValue.intValue());
                    } else {
                        return Value.newLongValue(lValue);
                    }
                } catch (NumberFormatException e) {
                    throw new InvalidValueException("Invalid value for integer resource: " + value, path);
                }
            case BOOLEAN:
                switch (value) {
                case "0":
                    return Value.newBooleanValue(false);
                case "1":
                    return Value.newBooleanValue(true);
                default:
                    throw new InvalidValueException("Invalid value for boolean resource: " + value, path);
                }
            case FLOAT:
                try {
                    Double dValue = Double.valueOf(value);
                    if (dValue >= Float.MIN_VALUE && dValue <= Float.MAX_VALUE) {
                        return Value.newFloatValue(dValue.floatValue());
                    } else {
                        return Value.newDoubleValue(dValue);
                    }
                } catch (NumberFormatException e) {
                    throw new InvalidValueException("Invalid value for float resource: " + value, path);
                }
            case TIME:
                // number of seconds since 1970/1/1
                return Value.newDateValue(new Date(Long.valueOf(value) * 1000L));
            case OPAQUE:
                // not specified
            default:
                throw new InvalidValueException("Could not parse opaque value with content format " + type, path);
            }
        } catch (NumberFormatException e) {
            throw new InvalidValueException("Invalid numeric value: " + value, path, e);
        }
    }

    private static LwM2mNode parseTlv(Tlv[] tlvs, LwM2mPath path) throws InvalidValueException {
        LOG.trace("Parsing TLV content for path {}: {}", path, tlvs);

        if (path.isObject()) {
            // object level request
            LwM2mObjectInstance[] instances = new LwM2mObjectInstance[tlvs.length];
            for (int i = 0; i < tlvs.length; i++) {
                instances[i] = parseObjectInstancesTlv(tlvs[i], path.getObjectId());
            }
            return new LwM2mObject(path.getObjectId(), instances);

        } else if (path.isObjectInstance()) {
            // object instance level request
            LwM2mResource[] resources = new LwM2mResource[tlvs.length];
            for (int i = 0; i < tlvs.length; i++) {
                resources[i] = parseResourceTlv(tlvs[i], path.getObjectId(), path.getObjectInstanceId());
            }
            return new LwM2mObjectInstance(path.getObjectInstanceId(), resources);

        } else {
            // resource level request
            if (tlvs.length == 1) {
                switch (tlvs[0].getType()) {
                case RESOURCE_VALUE:
                    // single value
                    return new LwM2mResource(tlvs[0].getIdentifier(), parseTlvValue(tlvs[0].getValue(), path));
                case MULTIPLE_RESOURCE:
                    // supported but not compliant with the TLV specification
                    return parseResourceTlv(tlvs[0], path.getObjectId(), path.getObjectInstanceId());

                default:
                    throw new InvalidValueException("Invalid TLV type: " + tlvs[0].getType(), path);
                }
            } else {
                // array of values
                Value<?>[] values = new Value[tlvs.length];
                for (int j = 0; j < tlvs.length; j++) {
                    values[j] = parseTlvValue(tlvs[j].getValue(), path);
                }
                return new LwM2mResource(path.getResourceId(), values);
            }
        }
    }

    private static LwM2mObjectInstance parseObjectInstancesTlv(Tlv tlv, int objectId) throws InvalidValueException {
        // read resources
        LwM2mResource[] resources = new LwM2mResource[tlv.getChildren().length];
        for (int i = 0; i < tlv.getChildren().length; i++) {
            resources[i] = parseResourceTlv(tlv.getChildren()[i], objectId, tlv.getIdentifier());
        }
        return new LwM2mObjectInstance(tlv.getIdentifier(), resources);
    }

    private static LwM2mResource parseResourceTlv(Tlv tlv, int objectId, int objectInstanceId)
            throws InvalidValueException {
        LwM2mPath rscPath = new LwM2mPath(objectId, objectInstanceId, tlv.getIdentifier());
        switch (tlv.getType()) {
        case MULTIPLE_RESOURCE:
            // read values
            Value<?>[] values = new Value[tlv.getChildren().length];
            for (int j = 0; j < tlv.getChildren().length; j++) {
                values[j] = parseTlvValue(tlv.getChildren()[j].getValue(), rscPath);
            }
            return new LwM2mResource(tlv.getIdentifier(), values);
        case RESOURCE_VALUE:
            return new LwM2mResource(tlv.getIdentifier(), parseTlvValue(tlv.getValue(), rscPath));
        default:
            throw new InvalidValueException("Invalid TLV value", rscPath);
        }
    }

    private static Value<?> parseTlvValue(byte[] value, LwM2mPath rscPath) throws InvalidValueException {

        ResourceSpec rscDesc = Resources.getResourceSpec(rscPath.getObjectId(), rscPath.getResourceId());
        if (rscDesc == null) {
            LOG.trace("TLV value for path {} and unknown type: {}", rscPath, value);
            // no resource description... opaque
            return Value.newBinaryValue(value);
        }

        LOG.trace("TLV value for path {} and expected type {}: {}", rscPath, rscDesc.type, value);
        try {
            switch (rscDesc.type) {
            case STRING:
                return Value.newStringValue(TlvDecoder.decodeString(value));
            case INTEGER:
                Number intNb = TlvDecoder.decodeInteger(value);
                if (value.length < 8) {
                    return Value.newIntegerValue(intNb.intValue());
                } else {
                    return Value.newLongValue(intNb.longValue());
                }

            case BOOLEAN:
                return Value.newBooleanValue(TlvDecoder.decodeBoolean(value));

            case FLOAT:
                Number floatNb = TlvDecoder.decodeFloat(value);
                if (value.length < 8) {
                    return Value.newFloatValue(floatNb.floatValue());
                } else {
                    return Value.newDoubleValue(floatNb.doubleValue());
                }

            case TIME:
                return Value.newDateValue(TlvDecoder.decodeDate(value));

            case OPAQUE:
            default:
                return Value.newBinaryValue(value);
            }
        } catch (TlvException e) {
            throw new InvalidValueException("Invalid content for type " + rscDesc.type, rscPath, e);
        }
    }
}
