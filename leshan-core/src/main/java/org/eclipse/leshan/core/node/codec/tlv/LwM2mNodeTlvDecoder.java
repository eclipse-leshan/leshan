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
import org.eclipse.leshan.core.model.ObjectModel;
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

    public static <T extends LwM2mNode> T decode(byte[] content, LwM2mPath path, LwM2mModel model, Class<T> nodeClass)
            throws InvalidValueException {
        try {
            Tlv[] tlvs = TlvDecoder.decode(ByteBuffer.wrap(content != null ? content : new byte[0]));
            return parseTlv(tlvs, path, model, nodeClass);
        } catch (TlvException e) {
            throw new InvalidValueException("Unable to decode tlv.", path, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends LwM2mNode> T parseTlv(Tlv[] tlvs, LwM2mPath path, LwM2mModel model, Class<T> nodeClass)
            throws InvalidValueException {
        LOG.trace("Parsing TLV content for path {}: {}", path, tlvs);

        // Object
        if (nodeClass == LwM2mObject.class) {
            final List<LwM2mObjectInstance> instances = new ArrayList<>();

            // is it an array of TLV resources?
            if (tlvs.length > 0 && //
                    (tlvs[0].getType() == TlvType.MULTIPLE_RESOURCE || tlvs[0].getType() == TlvType.RESOURCE_VALUE)) {

                ObjectModel oModel = model.getObjectModel(path.getObjectId());
                if (oModel == null) {
                    LOG.warn("No model for object {}. The tlv is decoded assuming this is a single instance object",
                            path.getObjectId());
                    instances.add(parseObjectInstanceTlv(tlvs, path.getObjectId(), 0, model));
                } else if (!oModel.multiple) {
                    instances.add(parseObjectInstanceTlv(tlvs, path.getObjectId(), 0, model));
                } else {
                    throw new InvalidValueException("Object instance TLV is mandatory for multiple instances object",
                            path);
                }

            } else {
                for (int i = 0; i < tlvs.length; i++) {
                    if (tlvs[i].getType() != TlvType.OBJECT_INSTANCE)
                        throw new InvalidValueException(String.format("Expected TLV of type OBJECT_INSTANCE but was %s",
                                tlvs[i].getType().name()), path);

                    instances.add(parseObjectInstanceTlv(tlvs[i].getChildren(), path.getObjectId(),
                            tlvs[i].getIdentifier(), model));
                }
            }
            return (T) new LwM2mObject(path.getObjectId(), instances);
        }

        // Object instance
        else if (nodeClass == LwM2mObjectInstance.class) {

            if (tlvs.length == 1 && tlvs[0].getType() == TlvType.OBJECT_INSTANCE) {
                if (path.isObjectInstance() && tlvs[0].getIdentifier() != path.getObjectInstanceId()) {
                    throw new InvalidValueException("Id conflict between path and instance TLV", path);
                }
                // object instance TLV
                return (T) parseObjectInstanceTlv(tlvs[0].getChildren(), path.getObjectId(), tlvs[0].getIdentifier(),
                        model);
            } else {
                // array of TLV resources
                // try to retrieve the instanceId from the path or the model
                Integer instanceId = path.getObjectInstanceId();
                if (instanceId == null) {
                    // single instance object?
                    ObjectModel oModel = model.getObjectModel(path.getObjectId());
                    if (oModel != null && !oModel.multiple) {
                        instanceId = 0;
                    } else {
                        instanceId = LwM2mObjectInstance.UNDEFINED;
                    }
                }
                return (T) parseObjectInstanceTlv(tlvs, path.getObjectId(), instanceId, model);
            }
        }

        // Resource
        else if (nodeClass == LwM2mResource.class) {

            if (tlvs.length == 1 && tlvs[0].getType() != TlvType.RESOURCE_INSTANCE) {
                if (path.isResource() && path.getResourceId() != tlvs[0].getIdentifier()) {
                    throw new InvalidValueException("Id conflict between path and resource TLV", path);
                }
                return (T) parseResourceTlv(tlvs[0], path.getObjectId(), path.getObjectInstanceId(), model);
            } else {
                Type expectedRscType = getResourceType(path, model);
                return (T) LwM2mMultipleResource.newResource(path.getResourceId(),
                        parseTlvValues(tlvs, expectedRscType, path), expectedRscType);
            }
        } else {
            throw new IllegalArgumentException("invalid node class: " + nodeClass);
        }

    }

    private static LwM2mObjectInstance parseObjectInstanceTlv(Tlv[] rscTlvs, int objectId, int instanceId,
            LwM2mModel model) throws InvalidValueException {
        // read resources
        List<LwM2mResource> resources = new ArrayList<>(rscTlvs.length);
        for (int i = 0; i < rscTlvs.length; i++) {
            resources.add(parseResourceTlv(rscTlvs[i], objectId, instanceId, model));
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
                throw new InvalidValueException(
                        String.format("Expected TLV of type RESOURCE_INSTANCE but was %s", tlvChild.getType().name()),
                        path);

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
            case OBJLNK:
                return TlvDecoder.decodeObjlnk(value);
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
