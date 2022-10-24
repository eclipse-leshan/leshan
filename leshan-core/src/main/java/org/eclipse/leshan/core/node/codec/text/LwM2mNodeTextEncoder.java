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
package org.eclipse.leshan.core.node.codec.text;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.eclipse.leshan.core.link.DefaultLinkSerializer;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.LinkSerializer;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mNodeVisitor;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mValueConverter;
import org.eclipse.leshan.core.node.codec.NodeEncoder;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.core.util.base64.Base64Encoder;
import org.eclipse.leshan.core.util.base64.DefaultBase64Encoder;
import org.eclipse.leshan.core.util.base64.DefaultBase64Encoder.EncoderAlphabet;
import org.eclipse.leshan.core.util.base64.DefaultBase64Encoder.EncoderPadding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeTextEncoder implements NodeEncoder {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeTextEncoder.class);

    private final LinkSerializer linkSerializer;
    private final Base64Encoder base64Encoder;

    public LwM2mNodeTextEncoder() {
        this(new DefaultLinkSerializer(), new DefaultBase64Encoder(EncoderAlphabet.BASE64, EncoderPadding.WITH));
    }

    public LwM2mNodeTextEncoder(LinkSerializer linkSerializer, Base64Encoder base64Encoder) {
        this.linkSerializer = linkSerializer;
        this.base64Encoder = base64Encoder;
    }

    @Override
    public byte[] encode(LwM2mNode node, LwM2mPath path, LwM2mModel model, LwM2mValueConverter converter)
            throws CodecException {
        Validate.notNull(node);
        Validate.notNull(path);
        Validate.notNull(model);

        InternalEncoder internalEncoder = new InternalEncoder();
        internalEncoder.path = path;
        internalEncoder.model = model;
        internalEncoder.converter = converter;
        node.accept(internalEncoder);
        return internalEncoder.encoded;
    }

    private class InternalEncoder implements LwM2mNodeVisitor {

        // visitor inputs
        private LwM2mPath path;
        private LwM2mModel model;
        private LwM2mValueConverter converter;

        // visitor output
        private byte[] encoded = null;

        @Override
        public void visit(LwM2mObject object) {
            throw new CodecException("Object %s cannot be encoded in text format", path);
        }

        @Override
        public void visit(LwM2mObjectInstance instance) {
            throw new CodecException("Object instance %s cannot be encoded in text format", path);
        }

        @Override
        public void visit(LwM2mResource resource) {
            if (resource.isMultiInstances()) {
                throw new CodecException("Multiple instances resource %s cannot be encoded in text format", path);
            }
            LOG.trace("Encoding resource {} into text", resource);

            ResourceModel rSpec = model.getResourceModel(path.getObjectId(), resource.getId());
            Type expectedType = rSpec != null ? rSpec.type : resource.getType();
            Object val = converter.convertValue(resource.getValue(), resource.getType(), expectedType, path);

            if (expectedType == null) {
                throw new CodecException(
                        "Unable to encode value for resource {} without type(probably a executable one)", path);
            }

            String strValue = getStringValue(expectedType, val);

            encoded = strValue.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void visit(LwM2mResourceInstance instance) {
            LOG.trace("Encoding resource instance {} into text", instance);

            ResourceModel rSpec = model.getResourceModel(path.getObjectId(), path.getResourceId());
            Type expectedType = rSpec != null ? rSpec.type : instance.getType();
            Object val = converter.convertValue(instance.getValue(), instance.getType(), expectedType, path);

            if (expectedType == null) {
                throw new CodecException(
                        "Unable to encode value for resource {} without type(probably a executable one)", path);
            }

            String strValue = getStringValue(expectedType, val);

            encoded = strValue.getBytes(StandardCharsets.UTF_8);
        }

        private String getStringValue(Type expectedType, Object val) {
            String strValue;
            switch (expectedType) {
            case INTEGER:
            case FLOAT:
            case STRING:
            case UNSIGNED_INTEGER:
                strValue = String.valueOf(val);
                break;
            case BOOLEAN:
                strValue = ((Boolean) val) ? "1" : "0";
                break;
            case TIME:
                // number of seconds since 1970/1/1
                strValue = String.valueOf(((Date) val).getTime() / 1000L);
                break;
            case OBJLNK:
                ObjectLink objlnk = (ObjectLink) val;
                strValue = objlnk.encodeToString();
                break;
            case CORELINK:
                Link[] links = (Link[]) val;
                strValue = linkSerializer.serializeCoreLinkFormat(links);
                break;
            case OPAQUE:
                byte[] binaryValue = (byte[]) val;
                strValue = base64Encoder.encode(binaryValue);
                break;
            default:
                throw new CodecException("Cannot encode %s in text format for %s", val, path);
            }
            return strValue;
        }
    }
}
