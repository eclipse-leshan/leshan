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
package org.eclipse.leshan.core.node.codec.text;

import java.util.Date;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.Value;
import org.eclipse.leshan.core.node.codec.InvalidValueException;
import org.eclipse.leshan.util.Charsets;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mNodeTextDecoder {
    private static final Logger LOG = LoggerFactory.getLogger(LwM2mNodeTextDecoder.class);

    public static LwM2mNode decode(byte[] content, LwM2mPath path, LwM2mModel model) throws InvalidValueException {
        // single resource value
        Validate.notNull(path.getResourceId());
        ResourceModel rDesc = model.getResourceModel(path.getObjectId(), path.getResourceId());

        String strValue = new String(content, Charsets.UTF_8);
        Value<?> value = null;
        if (rDesc != null) {
            value = parseTextValue(strValue, rDesc.type, path);
        } else {
            // unknown resource, returning a default string value
            value = Value.newStringValue(strValue);
        }
        return new LwM2mResource(path.getResourceId(), value);
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
}
