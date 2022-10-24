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

import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.link.LinkParser;
import org.eclipse.leshan.core.link.lwm2m.DefaultLwM2mLinkParser;
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
import org.eclipse.leshan.core.util.base64.Base64Decoder;
import org.eclipse.leshan.core.util.base64.DefaultBase64Decoder;
import org.eclipse.leshan.core.util.base64.DefaultBase64Decoder.DecoderAlphabet;
import org.eclipse.leshan.core.util.base64.DefaultBase64Decoder.DecoderPadding;
import org.eclipse.leshan.core.util.base64.InvalidBase64Exception;
import org.eclipse.leshan.core.util.datatype.ULong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeTextDecoder implements NodeDecoder {
    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeTextDecoder.class);

    // parser used for core link data type
    private final LinkParser linkParser;
    private final Base64Decoder base64Decoder;

    public LwM2mNodeTextDecoder() {
        this(new DefaultLwM2mLinkParser(), new DefaultBase64Decoder(DecoderAlphabet.BASE64, DecoderPadding.REQUIRED));
    }

    /**
     * Create a new LwM2mNodeTextDecoder with a custom {@link LinkParser}
     *
     * @param linkParser the link parser for core link format resources.
     */
    public LwM2mNodeTextDecoder(LinkParser linkParser, Base64Decoder base64Decoder) {
        this.linkParser = linkParser;
        this.base64Decoder = base64Decoder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends LwM2mNode> T decode(byte[] content, LwM2mPath path, LwM2mModel model, Class<T> nodeClass)
            throws CodecException {
        if (!path.isResource() && !path.isResourceInstance())
            throw new CodecException("Invalid path %s : TextDecoder decodes resource OR resource instance only", path);

        ResourceModel rDesc = model.getResourceModel(path.getObjectId(), path.getResourceId());

        String strValue = content != null ? new String(content, StandardCharsets.UTF_8) : "";

        if (path.isResource()) {
            if (rDesc != null) {
                return (T) LwM2mSingleResource.newResource(path.getResourceId(),
                        parseTextValue(strValue, rDesc.type, path), rDesc.type);
            }

            // unknown resource, returning a default string value
            return (T) LwM2mSingleResource.newStringResource(path.getResourceId(), strValue);
        }

        if (rDesc != null) {
            return (T) LwM2mResourceInstance.newInstance(path.getResourceInstanceId(),
                    parseTextValue(strValue, rDesc.type, path), rDesc.type);
        }
        // unknown resource, returning a default string value
        return (T) LwM2mResourceInstance.newStringInstance(path.getResourceInstanceId(), strValue);
    }

    private Object parseTextValue(String value, Type type, LwM2mPath path) throws CodecException {
        LOG.trace("TEXT value for path {} and expected type {}: {}", path, type, value);

        switch (type) {
        case STRING:
            return value;
        case INTEGER:
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException e) {
                throw new CodecException("Invalid value [%s] for integer resource [%s]", value, path);
            }
        case UNSIGNED_INTEGER:
            try {
                return ULong.valueOf(value);
            } catch (NumberFormatException e) {
                throw new CodecException("Invalid value [%s] for unsigned integer resource [%s]", value, path);
            }
        case BOOLEAN:
            switch (value) {
            case "0":
                return false;
            case "1":
                return true;
            default:
                throw new CodecException("Invalid value [%s] for boolean resource [%s]", value, path);
            }
        case FLOAT:
            try {
                return Double.valueOf(value);
            } catch (NumberFormatException e) {
                throw new CodecException("Invalid value [%s] for float resource [%s]", value, path);
            }
        case TIME:
            // number of seconds since 1970/1/1
            try {
                return new Date(Long.valueOf(value) * 1000L);
            } catch (NumberFormatException e) {
                throw new CodecException("Invalid value [%s] for date resource [%s]", value, path);
            }
        case OBJLNK:
            try {
                return ObjectLink.decodeFromString(value);
            } catch (IllegalArgumentException e) {
                throw new CodecException(e, "Invalid value [%s] for objectLink resource [%s]", value, path);
            }
        case CORELINK:
            try {
                return linkParser.parseCoreLinkFormat(value.getBytes(StandardCharsets.UTF_8));
            } catch (LinkParseException e) {
                throw new CodecException(e, "Invalid value [%s] for CoreLink resource [%s]", value, path);
            }
        case OPAQUE:
            try {
                return base64Decoder.decode(value);
            } catch (InvalidBase64Exception e) {
                throw new CodecException(e, "Invalid value for opaque resource [%s], base64 expected", path);
            }

        default:
            throw new CodecException("Could not handle %s value with TEXT encoder for resource %s", type, path);
        }
    }
}
