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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            final List<LwM2mObjectInstance> instances = new ArrayList<>();

            // is it an array of resource TLV?
            if (tlvs.length > 0 && //
                    (tlvs[0].getType() == TlvType.MULTIPLE_RESOURCE || tlvs[0].getType() == TlvType.RESOURCE_VALUE)) {
                instances.add(parseObjectInstancesTlv(tlvs, path.getObjectId(), 0, model));
            } else {
                for (int i = 0; i < tlvs.length; i++) {
                    if (tlvs[i].getType() != TlvType.OBJECT_INSTANCE)
                        throw new InvalidValueException(String.format(
                                "Expected TLV of type OBJECT_INSTANCE but was %s", tlvs[i].getType().name()), path);

                    instances.add(parseObjectInstancesTlv(tlvs[i].getChildren(), path.getObjectId(),
                            tlvs[i].getIdentifier(), model));
                }
            }
            return new LwM2mObject(path.getObjectId(), instances);

        } else if (path.isObjectInstance()) {
            // object instance level request
            return parseObjectInstancesTlv(tlvs, path.getObjectId(), path.getObjectInstanceId(), model);
        } else {
            // resource level request
            if (tlvs.length == 1 && tlvs[0].getType() != TlvType.RESOURCE_INSTANCE) {
                return parseResourceTlv(tlvs[0], path.getObjectId(), path.getObjectInstanceId(), model);
            } else {
                Type expectedType = getResourceType(path, model);
                return LwM2mMultipleResource.newResource(path.getResourceId(),
                        parseTlvValues(tlvs, expectedType, path), expectedType);
            }
        }
    }

    private static LwM2mObjectInstance parseObjectInstancesTlv(Tlv[] tlvs, int objectId, int instanceId,
            LwM2mModel model) throws InvalidValueException {
        // read resources
        List<LwM2mResource> resources = new ArrayList<>(tlvs.length);
        for (int i = 0; i < tlvs.length; i++) {
            resources.add(parseResourceTlv(tlvs[i], objectId, instanceId, model));
        }
        return new LwM2mObjectInstance(instanceId, resources);
    }

    private static LwM2mResource parseResourceTlv(Tlv tlv, int objectId, int objectInstanceId, LwM2mModel model)
            throws InvalidValueException {
        LwM2mPath resourcePath = new LwM2mPath(objectId, objectInstanceId, tlv.getIdentifier());
        Type expectedType = getResourceType(resourcePath, model);
        Integer resourceId = tlv.getIdentifier();
        switch (tlv.getType()) {
        case MULTIPLE_RESOURCE:
            return LwM2mMultipleResource.newResource(resourceId,
                    parseTlvValues(tlv.getChildren(), expectedType, resourcePath), expectedType);
        case RESOURCE_VALUE:
            return LwM2mSingleResource.newResource(resourceId,
                    parseTlvValue(tlv.getValue(), expectedType, resourcePath), expectedType);
        default:
            throw new InvalidValueException("Invalid TLV value", resourcePath);
        }
    }

    private static Map<Integer, Object> parseTlvValues(Tlv[] tlvs, Type expectedType, LwM2mPath path)
            throws InvalidValueException {
        Map<Integer, Object> values = new HashMap<Integer, Object>();
        for (int j = 0; j < tlvs.length; j++) {
            Tlv tlvChild = tlvs[j];

            if (tlvChild.getType() != TlvType.RESOURCE_INSTANCE)
                throw new InvalidValueException(String.format("Expected TLV of type RESOURCE_INSTANCE but was %s",
                        tlvChild.getType().name()), path);

            values.put(tlvChild.getIdentifier(), parseTlvValue(tlvChild.getValue(), expectedType, path));
        }
        return values;
    }

    private static Object parseTlvValue(byte[] value, Type expectedType, LwM2mPath path) throws InvalidValueException {
        try {
            LOG.trace("TLV value for path {} and expected type {}: {}", path, expectedType, value);
            switch (expectedType) {
            case STRING:
                return TlvDecoder.decodeString(value);
            case INTEGER:
                return TlvDecoder.decodeInteger(value).longValue();
            case FLOAT:
                return TlvDecoder.decodeFloat(value).doubleValue();
            case BOOLEAN:
                return TlvDecoder.decodeBoolean(value);
            case TIME:
                return TlvDecoder.decodeDate(value);
            case OPAQUE:
                return value;
            default:
                throw new InvalidValueException("Unsupported type " + expectedType, path);
            }
        } catch (TlvException e) {
            throw new InvalidValueException("Invalid content for type " + expectedType, path, e);
        }
    }

    public static Type getResourceType(LwM2mPath rscPath, LwM2mModel model) throws InvalidValueException {
        ResourceModel rscDesc = model.getResourceModel(rscPath.getObjectId(), rscPath.getResourceId());
        if (rscDesc == null || rscDesc.type == null) {
            LOG.trace("unknown type for resource : {}", rscPath);
            // no resource description... opaque
            return Type.OPAQUE;
        } else {
            return rscDesc.type;
        }
    }
}
