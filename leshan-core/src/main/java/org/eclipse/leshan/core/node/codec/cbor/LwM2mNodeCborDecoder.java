/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.core.node.codec.cbor;

import java.util.Date;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.NodeDecoder;
import org.eclipse.leshan.core.util.datatype.ULong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.upokecenter.cbor.CBORException;
import com.upokecenter.cbor.CBORNumber;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;

public class LwM2mNodeCborDecoder implements NodeDecoder {
    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeCborDecoder.class);

    @Override
    @SuppressWarnings("unchecked")
    public <T extends LwM2mNode> T decode(byte[] content, LwM2mPath path, LwM2mModel model, Class<T> nodeClass)
            throws CodecException {

        // Support only single value
        if (!path.isResource() && !path.isResourceInstance())
            throw new CodecException("Invalid path %s : CborDecoder decodes resource OR resource instance only", path);

        if (content == null)
            return null;

        // Parse CBOR
        CBORObject cborObject;
        try {
            cborObject = CBORObject.DecodeFromBytes(content);
        } catch (CBORException e) {
            throw new CodecException(e, "Unable to parse CBORD value %s for resource %s", content, path);
        }

        // Find model to know expected type
        Type expectedType;
        ResourceModel rDesc = model.getResourceModel(path.getObjectId(), path.getResourceId());
        if (rDesc != null) {
            expectedType = rDesc.type;
        } else {
            // try to guess type from CBOR ?
            expectedType = guessTypeFromCbor(cborObject, path);
            LOG.debug("Decoding CBOR resource or resource instance without model, type guessed {}", expectedType);
        }

        // Get Node Value
        Object nodeValue = parseCborValue(cborObject, expectedType, path);

        // Create Node
        if (path.isResource()) {
            return (T) LwM2mSingleResource.newResource(path.getResourceId(), nodeValue, expectedType);
        } else {
            return (T) LwM2mResourceInstance.newInstance(path.getResourceInstanceId(), nodeValue, expectedType);
        }
    }

    private Type guessTypeFromCbor(CBORObject cborObject, LwM2mPath path) {
        switch (cborObject.getType()) {
        case Boolean:
            return Type.BOOLEAN;
        case FloatingPoint:
            if (cborObject.HasTag(1))
                return Type.TIME;
            else
                return Type.FLOAT;
        case Integer:
            if (cborObject.HasTag(1))
                return Type.TIME;
            else
                return Type.FLOAT;
        case TextString:
            if (cborObject.HasTag(0))
                return Type.TIME;
            else
                return Type.STRING;
        case ByteString:
            return Type.OPAQUE;
        default:
            throw new CodecException("Unable to guess LWM2M type for resource %s, cbor type is {}", path,
                    cborObject.getType());
        }
    }

    private Object parseCborValue(CBORObject cborObject, Type type, LwM2mPath path) throws CodecException {
        LOG.trace("CBOR value for path {} and expected type {}: {}", path, type, cborObject.toString());

        try {
            if (cborObject.isNull())
                return null;

            switch (type) {
            case STRING:
                if (cborObject.getType() == CBORType.TextString) {
                    return cborObject.AsString();
                }
                break;
            case INTEGER:
                if (cborObject.getType() == CBORType.Integer) {
                    return cborObject.AsInt64Value();
                }
                break;
            case UNSIGNED_INTEGER:
                if (cborObject.getType() == CBORType.Integer) {
                    CBORNumber number = cborObject.AsNumber();
                    if (number.IsInteger() && !number.IsNegative()
                            && number.ToEIntegerIfExact().GetUnsignedBitLengthAsInt64() <= 64) {
                        return ULong.valueOf(number.ToInt64Unchecked());
                    }
                }
                break;
            case BOOLEAN:
                if (cborObject.getType() == CBORType.Boolean) {
                    return cborObject.AsBoolean();
                }
            case FLOAT:
                if (cborObject.getType() == CBORType.FloatingPoint) {
                    return cborObject.AsDoubleValue();
                }
                break;
            case TIME:
                return cborObject.ToObject(Date.class);
            case OBJLNK:
                if (cborObject.getType() == CBORType.TextString) {
                    return ObjectLink.decodeFromString(cborObject.AsString());
                }
            case OPAQUE:
                if (cborObject.getType() == CBORType.ByteString) {
                    return cborObject.GetByteString();
                }
                break;
            default:
                throw new CodecException("Unsupported type %s for resource %s", type, path);
            }
        } catch (IllegalStateException | ArithmeticException | NumberFormatException e) {
            throw new CodecException(e, "Unable to convert CBOR value %s of type %s in type %s for resource %s",
                    cborObject.toString(), cborObject.getType(), type, path);
        }
        throw new CodecException("Unable to convert CBOR value %s of type %s in type %s for resource %s",
                cborObject.toString(), cborObject.getType(), type, path);
    }
}
