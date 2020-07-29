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
import java.util.Arrays;
import java.util.Date;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeTextDecoder {
    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeTextDecoder.class);

    public static LwM2mNode decode(byte[] content, LwM2mPath path, LwM2mModel model) throws CodecException {
        if (!path.isResource())
            throw new CodecException("Invalid path %s : TextDecoder decodes resource only", path);

        ResourceModel rDesc = model.getResourceModel(path.getObjectId(), path.getResourceId());

        String strValue = content != null ? new String(content, StandardCharsets.UTF_8) : "";
        if (rDesc != null) {
            return LwM2mSingleResource.newResource(path.getResourceId(), parseTextValue(strValue, rDesc.type, path),
                    rDesc.type);
        } else {
            // unknown resource, returning a default string value
            return LwM2mSingleResource.newStringResource(path.getResourceId(), strValue);
        }

    }

    private static Object parseTextValue(String value, Type type, LwM2mPath path) throws CodecException {
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
            String[] intArr = value.split(":");
            if (intArr.length != 2)
                throw new CodecException("Invalid value %s for objectLink resource [%s]", Arrays.toString(intArr),
                        path);
            try {
                return new ObjectLink(Integer.parseInt(intArr[0]), Integer.parseInt(intArr[1]));
            } catch (NumberFormatException e) {
                throw new CodecException("Invalid value %s for objectLink resource [%s] ", Arrays.toString(intArr),
                        path);
            }
        case OPAQUE:
            if (!Base64.isBase64(value)) {
                throw new CodecException("Invalid value for opaque resource [%s], base64 expected", path);
            }
            return Base64.decodeBase64(value);
        default:
            throw new CodecException("Could not handle %s value with TEXT encoder for resource %s", type, path);
        }
    }
}
