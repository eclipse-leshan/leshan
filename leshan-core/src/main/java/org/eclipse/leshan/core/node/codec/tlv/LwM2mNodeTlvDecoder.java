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

import java.nio.ByteBuffer;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.Value;
import org.eclipse.leshan.core.node.codec.InvalidValueException;
import org.eclipse.leshan.tlv.Tlv;
import org.eclipse.leshan.tlv.Tlv.TlvType;
import org.eclipse.leshan.tlv.TlvDecoder;
import org.eclipse.leshan.tlv.TlvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeTlvDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeTlvDecoder.class);

    public static LwM2mNode decode(byte[] content, LwM2mPath path, LwM2mModel model) throws InvalidValueException {
        try {
            Tlv[] tlvs = TlvDecoder.decode(ByteBuffer.wrap(content));
            return parseTlv(tlvs, path, model);
        } catch (TlvException e) {
            throw new InvalidValueException("Unable to decode tlv.", path, e);
        }
    }

    private static LwM2mNode parseTlv(Tlv[] tlvs, LwM2mPath path, LwM2mModel model) throws InvalidValueException {
        LOG.trace("Parsing TLV content for path {}: {}", path, tlvs);

        if (path.isObject()) {
            // object level request
            final LwM2mObjectInstance[] instances;

            // is it an array of resource TLV?
            if (tlvs.length > 0 && //
                    (tlvs[0].getType() == TlvType.MULTIPLE_RESOURCE || tlvs[0].getType() == TlvType.RESOURCE_VALUE)) {
                LwM2mResource[] resources = new LwM2mResource[tlvs.length];
                for (int i = 0; i < tlvs.length; i++) {
                    resources[i] = parseResourceTlv(tlvs[i], path.getObjectId(), 0, model);
                }
                instances = new LwM2mObjectInstance[] { new LwM2mObjectInstance(0, resources) };
            } else {
                instances = new LwM2mObjectInstance[tlvs.length];
                for (int i = 0; i < tlvs.length; i++) {
                    instances[i] = parseObjectInstancesTlv(tlvs[i], path.getObjectId(), model);
                }
            }
            return new LwM2mObject(path.getObjectId(), instances);

        } else if (path.isObjectInstance()) {
            // object instance level request
            LwM2mResource[] resources = new LwM2mResource[tlvs.length];
            for (int i = 0; i < tlvs.length; i++) {
                resources[i] = parseResourceTlv(tlvs[i], path.getObjectId(), path.getObjectInstanceId(), model);
            }
            return new LwM2mObjectInstance(path.getObjectInstanceId(), resources);

        } else {
            // resource level request
            if (tlvs.length == 1) {
                switch (tlvs[0].getType()) {
                case RESOURCE_VALUE:
                    // single value
                    return new LwM2mResource(tlvs[0].getIdentifier(), parseTlvValue(tlvs[0].getValue(), path, model));
                case MULTIPLE_RESOURCE:
                    // supported but not compliant with the TLV specification
                    return parseResourceTlv(tlvs[0], path.getObjectId(), path.getObjectInstanceId(), model);

                default:
                    throw new InvalidValueException("Invalid TLV type: " + tlvs[0].getType(), path);
                }
            } else {
                // array of values
                Value<?>[] values = new Value[tlvs.length];
                for (int j = 0; j < tlvs.length; j++) {
                    values[j] = parseTlvValue(tlvs[j].getValue(), path, model);
                }
                return new LwM2mResource(path.getResourceId(), values);
            }
        }
    }

    private static LwM2mObjectInstance parseObjectInstancesTlv(Tlv tlv, int objectId, LwM2mModel model)
            throws InvalidValueException {
        // read resources
        LwM2mResource[] resources = new LwM2mResource[tlv.getChildren().length];
        for (int i = 0; i < tlv.getChildren().length; i++) {
            resources[i] = parseResourceTlv(tlv.getChildren()[i], objectId, tlv.getIdentifier(), model);
        }
        return new LwM2mObjectInstance(tlv.getIdentifier(), resources);
    }

    private static LwM2mResource parseResourceTlv(Tlv tlv, int objectId, int objectInstanceId, LwM2mModel model)
            throws InvalidValueException {
        LwM2mPath rscPath = new LwM2mPath(objectId, objectInstanceId, tlv.getIdentifier());
        switch (tlv.getType()) {
        case MULTIPLE_RESOURCE:
            // read values
            Value<?>[] values = new Value[tlv.getChildren().length];
            for (int j = 0; j < tlv.getChildren().length; j++) {
                values[j] = parseTlvValue(tlv.getChildren()[j].getValue(), rscPath, model);
            }
            return new LwM2mResource(tlv.getIdentifier(), values);
        case RESOURCE_VALUE:
            return new LwM2mResource(tlv.getIdentifier(), parseTlvValue(tlv.getValue(), rscPath, model));
        default:
            throw new InvalidValueException("Invalid TLV value", rscPath);
        }
    }

    private static Value<?> parseTlvValue(byte[] value, LwM2mPath rscPath, LwM2mModel model)
            throws InvalidValueException {

        ResourceModel rscDesc = model.getResourceModel(rscPath.getObjectId(), rscPath.getResourceId());
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
