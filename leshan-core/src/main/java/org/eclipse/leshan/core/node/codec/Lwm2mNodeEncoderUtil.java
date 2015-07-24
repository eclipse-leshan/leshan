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
package org.eclipse.leshan.core.node.codec;

import java.util.Date;

import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.Value;
import org.eclipse.leshan.core.node.Value.DataType;
import org.eclipse.leshan.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper for all the LwM2mNode encoders.
 */
public class Lwm2mNodeEncoderUtil {
    private static final Logger LOG = LoggerFactory.getLogger(Lwm2mNodeEncoderUtil.class);

    private Lwm2mNodeEncoderUtil() {
    }

    /**
     * Convert the given value to the expected type given in parameter.
     * 
     * @throws IllegalArgumentException the value is not convertible.
     */
    public static Value<?> convertValue(Value<?> value, Type expectedType) {
        if (expectedType == null) {
            // unknown resource, trusted value
            return value;
        }

        Type valueType = toResourceType(value.type);
        if (valueType == expectedType) {
            // expected type
            return value;
        }

        // We received a value with an unexpected type.
        // Let's do some magic to try to convert this value...

        switch (expectedType) {
        case BOOLEAN:
            switch (value.type) {
            case STRING:
                LOG.debug("Trying to convert string value {} to boolean", value.value);
                if (StringUtils.equalsIgnoreCase((String) value.value, "true")) {
                    return Value.newBooleanValue(true);
                } else if (StringUtils.equalsIgnoreCase((String) value.value, "false")) {
                    return Value.newBooleanValue(false);
                }
            case INTEGER:
                LOG.debug("Trying to convert int value {} to boolean", value.value);
                Integer val = (Integer) value.value;
                if (val == 1) {
                    return Value.newBooleanValue(true);
                } else if (val == 0) {
                    return Value.newBooleanValue(false);
                }
            default:
                break;
            }
            break;
        case TIME:
            switch (value.type) {
            case LONG:
                LOG.debug("Trying to convert long value {} to date", value.value);
                // let's assume we received the millisecond since 1970/1/1
                return Value.newDateValue(new Date((Long) value.value));
            case STRING:
                LOG.debug("Trying to convert string value {} to date", value.value);
                // let's assume we received an ISO 8601 format date
                try {
                    return Value.newDateValue(javax.xml.bind.DatatypeConverter.parseDateTime((String) value.value)
                            .getTime());
                } catch (IllegalArgumentException e) {
                    LOG.debug("Unable to convert string to date", e);
                }
            default:
                break;
            }
            break;
        case STRING:
            switch (value.type) {
            case BOOLEAN:
            case INTEGER:
            case LONG:
            case DOUBLE:
            case FLOAT:
                return Value.newStringValue(String.valueOf(value.value));
            default:
                break;
            }
            break;
        case OPAQUE:
            if (value.type == DataType.STRING) {
                // let's assume we received an hexadecimal string
                LOG.debug("Trying to convert hexadecimal string {} to byte array", value.value);
                return Value.newBinaryValue(javax.xml.bind.DatatypeConverter.parseHexBinary((String) value.value));
            }
            break;
        default:
        }

        throw new IllegalArgumentException("Invalid value type, expected " + expectedType + ", got " + valueType);
    }

    private static Type toResourceType(DataType type) {
        switch (type) {
        case INTEGER:
        case LONG:
            return Type.INTEGER;
        case FLOAT:
        case DOUBLE:
            return Type.FLOAT;
        case BOOLEAN:
            return Type.BOOLEAN;
        case OPAQUE:
            return Type.OPAQUE;
        case STRING:
            return Type.STRING;
        case TIME:
            return Type.TIME;
        default:
            throw new IllegalArgumentException("Invalid type: " + type);
        }
    }
}
