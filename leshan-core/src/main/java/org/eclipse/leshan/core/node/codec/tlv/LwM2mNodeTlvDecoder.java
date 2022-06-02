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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.link.LinkParser;
import org.eclipse.leshan.core.link.lwm2m.DefaultLwM2mLinkParser;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.InvalidLwM2mPathException;
import org.eclipse.leshan.core.node.LwM2mIncompletePath;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mNodeException;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.NodeDecoder;
import org.eclipse.leshan.core.tlv.Tlv;
import org.eclipse.leshan.core.tlv.Tlv.TlvType;
import org.eclipse.leshan.core.tlv.TlvDecoder;
import org.eclipse.leshan.core.tlv.TlvException;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.datatype.ULong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeTlvDecoder implements NodeDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeTlvDecoder.class);

    // parser used for core link data type
    private final LinkParser linkParser;

    public LwM2mNodeTlvDecoder() {
        this(new DefaultLwM2mLinkParser());
    }

    /**
     * Create a new LwM2mNodeTlvDecoder with a custom {@link LinkParser}.
     *
     * @param linkParser the link parser for core link format resources.
     */
    public LwM2mNodeTlvDecoder(LinkParser linkParser) {
        this.linkParser = linkParser;
    }

    @Override
    public <T extends LwM2mNode> T decode(byte[] content, LwM2mPath path, LwM2mModel model, Class<T> nodeClass)
            throws CodecException {
        try {
            Tlv[] tlvs = TlvDecoder.decode(ByteBuffer.wrap(content != null ? content : new byte[0]));
            return parseTlv(tlvs, path, model, nodeClass);
        } catch (TlvException | LwM2mNodeException | InvalidLwM2mPathException e) {
            throw new CodecException(String.format("Unable to decode tlv for path [%s]", path), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends LwM2mNode> T parseTlv(Tlv[] tlvs, LwM2mPath path, LwM2mModel model, Class<T> nodeClass)
            throws CodecException {
        LOG.trace("Parsing TLV content for path {}: {}", path, tlvs);

        // Object
        if (nodeClass == LwM2mObject.class) {
            Map<Integer, LwM2mObjectInstance> instances = new HashMap<>(tlvs.length);

            // is it an array of TLV resources?
            if (tlvs.length > 0 && //
                    (tlvs[0].getType() == TlvType.MULTIPLE_RESOURCE || tlvs[0].getType() == TlvType.RESOURCE_VALUE)) {

                ObjectModel oModel = model.getObjectModel(path.getObjectId());
                if (oModel == null) {
                    LOG.warn("No model for object {}. The tlv is decoded assuming this is a single instance object",
                            path.getObjectId());
                    instances.put(0, parseObjectInstanceTlv(tlvs, path.getObjectId(), 0, model));
                } else if (!oModel.multiple) {
                    instances.put(0, parseObjectInstanceTlv(tlvs, path.getObjectId(), 0, model));
                } else {
                    // this is strange "create without instance ID" case ...
                    instances.put(LwM2mObjectInstance.UNDEFINED,
                            parseObjectInstanceTlvWithoutId(tlvs, path.getObjectId(), model));
                }
            } else {
                for (Tlv tlv : tlvs) {
                    if (tlv.getType() != TlvType.OBJECT_INSTANCE)
                        throw new CodecException("Expected TLV of type OBJECT_INSTANCE but was %s  [path:%s]",
                                tlv.getType().name(), path);

                    LwM2mObjectInstance objectInstance = parseObjectInstanceTlv(tlv.getChildren(), path.getObjectId(),
                            tlv.getIdentifier(), model);
                    LwM2mObjectInstance previousObjectInstance = instances.put(objectInstance.getId(), objectInstance);
                    if (previousObjectInstance != null) {
                        throw new CodecException(
                                "2 OBJECT_INSTANCE nodes (%s,%s) with the same identifier %d for path %s",
                                previousObjectInstance, objectInstance, objectInstance.getId(), path);
                    }
                }
            }
            return (T) new LwM2mObject(path.getObjectId(), instances.values());
        }

        // Object instance
        else if (nodeClass == LwM2mObjectInstance.class) {

            if (tlvs.length == 1 && tlvs[0].getType() == TlvType.OBJECT_INSTANCE) {
                if (path.isObjectInstance() && tlvs[0].getIdentifier() != path.getObjectInstanceId()) {
                    throw new CodecException("Id conflict between path [%s] and instance TLV [object instance id=%d]",
                            path, tlvs[0].getIdentifier());
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
                        return (T) parseObjectInstanceTlv(tlvs, path.getObjectId(), 0, model);
                    } else {
                        throw new CodecException(
                                "Object instance id is mandatory for multiple instances object [path:%s]", path);
                    }
                } else {
                    return (T) parseObjectInstanceTlv(tlvs, path.getObjectId(), instanceId, model);
                }
            }
        }

        // Resource
        else if (nodeClass == LwM2mResource.class) {
            // The object instance level should not be here, but if it is provided and
            // consistent we tolerate it
            if (tlvs.length == 1 && tlvs[0].getType() == TlvType.OBJECT_INSTANCE) {
                if (tlvs[0].getIdentifier() != path.getObjectInstanceId()) {
                    throw new CodecException("Id conflict between path [%s] and instance TLV [object instance id=%d]",
                            path, tlvs[0].getIdentifier());
                }
                tlvs = tlvs[0].getChildren();
            }

            ResourceModel resourceModel = model.getResourceModel(path.getObjectId(), path.getResourceId());
            if (tlvs.length == 0 && resourceModel != null && !resourceModel.multiple) {
                // If there is no TlV value and we know that this resource is a single resource
                // we raise an exception
                // else we consider this is a multi-instance resource
                throw new CodecException("TLV payload is mandatory for single resource %s", path);
            } else if (tlvs.length == 1 && tlvs[0].getType() != TlvType.RESOURCE_INSTANCE) {
                Tlv tlv = tlvs[0];
                if (tlv.getType() != TlvType.RESOURCE_VALUE && tlv.getType() != TlvType.MULTIPLE_RESOURCE) {
                    throw new CodecException(
                            "Expected TLV of type RESOURCE_VALUE or MUlTIPLE_RESOURCE but was %s [path:%s]",
                            tlv.getType().name(), path);
                }
                if (path.isResource() && path.getResourceId() != tlv.getIdentifier()) {
                    throw new CodecException("Id conflict between path [%s] and resource TLV [resource id=%s]", path,
                            tlv.getIdentifier());
                }
                return (T) parseResourceTlv(tlv, path, model);
            } else {
                Type expectedRscType = getResourceType(path, model);
                return (T) parseResourceInstancesTlv(tlvs, path, expectedRscType);
            }
        }

        // Resource Instance
        else if (nodeClass == LwM2mResourceInstance.class) {
            if (tlvs.length == 0) {
                throw new CodecException("TLV payload is mandatory for resource instance %s", path);
            } else if (tlvs.length > 1) {
                throw new CodecException("TLV payload must contain only 1 resource instance for %s", path);
            } else {
                Tlv tlv = tlvs[0];
                if (path.isResourceInstance() && path.getResourceInstanceId() != tlv.getIdentifier()) {
                    throw new CodecException("Id conflict between path [%s] and resource instance TLV [resource id=%s]",
                            path, tlv.getIdentifier());
                }
                Type expectedType = getResourceType(path, model);
                return (T) parseResourceInstanceTlv(tlv, path, expectedType);
            }
        }
        throw new IllegalArgumentException("invalid node class: " + nodeClass);
    }

    private LwM2mObjectInstance parseObjectInstanceTlv(Tlv[] rscTlvs, int objectId, int instanceId, LwM2mModel model)
            throws CodecException {
        Map<Integer, LwM2mResource> resources = new HashMap<>(rscTlvs.length);
        for (Tlv rscTlv : rscTlvs) {
            LwM2mPath resourcePath = new LwM2mPath(objectId, instanceId, rscTlv.getIdentifier());
            LwM2mResource resource = parseResourceTlv(rscTlv, resourcePath, model);
            LwM2mResource previousResource = resources.put(resource.getId(), resource);
            if (previousResource != null) {
                throw new CodecException("2 RESOURCE nodes (%s,%s) with the same identifier %d for path %s",
                        previousResource, resource, resource.getId(), resourcePath);
            }
        }
        return new LwM2mObjectInstance(instanceId, resources.values());

    }

    private LwM2mObjectInstance parseObjectInstanceTlvWithoutId(Tlv[] rscTlvs, int objectId, LwM2mModel model)
            throws CodecException {
        Map<Integer, LwM2mResource> resources = new HashMap<>(rscTlvs.length);
        for (Tlv rscTlv : rscTlvs) {
            LwM2mPath resourcePath = new LwM2mIncompletePath(objectId, rscTlv.getIdentifier());
            LwM2mResource resource = parseResourceTlv(rscTlv, resourcePath, model);
            LwM2mResource previousResource = resources.put(resource.getId(), resource);
            if (previousResource != null) {
                throw new CodecException("2 RESOURCE nodes (%s,%s) with the same identifier %d for path %s",
                        previousResource, resource, resource.getId(), resourcePath);
            }
        }
        return new LwM2mObjectInstance(resources.values());
    }

    private LwM2mResource parseResourceTlv(Tlv tlv, LwM2mPath resourcePath, LwM2mModel model) throws CodecException {
        Type expectedType = getResourceType(resourcePath, model);
        Integer resourceId = tlv.getIdentifier();
        switch (tlv.getType()) {
            case MULTIPLE_RESOURCE:
                return parseResourceInstancesTlv(tlv.getChildren(), resourcePath, expectedType);
            case RESOURCE_VALUE:
                return LwM2mSingleResource.newResource(resourceId,
                        parseTlvValue(tlv.getValue(), expectedType, resourcePath), expectedType);
            default:
                throw new CodecException("Invalid TLV type %s for resource %s", tlv.getType(), resourcePath);
        }
    }

    private LwM2mMultipleResource parseResourceInstancesTlv(Tlv[] tlvs, LwM2mPath resourcePath, Type expectedType)
            throws CodecException {
        Map<Integer, LwM2mResourceInstance> instances = new HashMap<>(tlvs.length);
        for (Tlv tlvChild : tlvs) {
            if (tlvChild.getType() != TlvType.RESOURCE_INSTANCE)
                throw new CodecException("Expected TLV of type RESOURCE_INSTANCE but was %s for path %s",
                        tlvChild.getType().name(), resourcePath.append(tlvChild.getIdentifier()));

            LwM2mResourceInstance resourceInstance = parseResourceInstanceTlv(tlvChild,
                    resourcePath.append(tlvChild.getIdentifier()), expectedType);
            LwM2mResourceInstance previousResourceInstance = instances.put(tlvChild.getIdentifier(), resourceInstance);
            if (previousResourceInstance != null) {
                throw new CodecException("2 RESOURCE_INSTANCE nodes (%s,%s) with the same identifier %d for path %s",
                        previousResourceInstance, resourceInstance, tlvChild.getIdentifier(), resourcePath);
            }
        }
        return new LwM2mMultipleResource(resourcePath.getResourceId(), expectedType, instances.values());
    }

    private LwM2mResourceInstance parseResourceInstanceTlv(Tlv tlv, LwM2mPath resourceInstancePath, Type expectedType)
            throws CodecException {
        Integer resourceInstanceId = tlv.getIdentifier();
        if (tlv.getType() != TlvType.RESOURCE_INSTANCE) {
            throw new CodecException("Expected TLV of type RESOURCE_INSTANCE but was %s for path %s", tlv.getType(),
                    resourceInstancePath);
        } else {
            return LwM2mResourceInstance.newInstance(resourceInstanceId,
                    parseTlvValue(tlv.getValue(), expectedType, resourceInstancePath), expectedType);
        }

    }

    private Object parseTlvValue(byte[] value, Type expectedType, LwM2mPath path) throws CodecException {
        try {
            LOG.trace("TLV value for path {} and expected type {}: {}", path, expectedType, value);
            switch (expectedType) {
                case STRING:
                    return TlvDecoder.decodeString(value);
                case INTEGER:
                    return TlvDecoder.decodeInteger(value).longValue();
                case UNSIGNED_INTEGER:
                    return ULong.valueOf(TlvDecoder.decodeInteger(value).longValue());
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
                case CORELINK:
                    return linkParser.parseCoreLinkFormat(value);
                default:
                    throw new CodecException("Unsupported type %s for path %s", expectedType, path);
            }
        } catch (TlvException | LinkParseException e) {
            throw new CodecException(e, "Invalid content [%s] for type %s for path %s", Hex.encodeHexString(value),
                    expectedType, path);
        }
    }

    public Type getResourceType(LwM2mPath rscPath, LwM2mModel model) throws CodecException {
        ResourceModel rscDesc = model.getResourceModel(rscPath.getObjectId(), rscPath.getResourceId());
        if (rscDesc == null) {
            LOG.trace("unknown type for resource : {}", rscPath);
            // no resource description... opaque
            return Type.OPAQUE;
        } else {
            return rscDesc.type;
        }
    }
}
