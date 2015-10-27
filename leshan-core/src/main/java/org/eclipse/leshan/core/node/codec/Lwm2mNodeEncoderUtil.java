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
    public static Object convertValue(Object value, Type currentType, Type expectedType) {
        if (expectedType == null) {
            // unknown resource, trusted value
            return value;
        }

        if (currentType == expectedType) {
            // expected type
            return value;
        }

        // We received a value with an unexpected type.
        // Let's do some magic to try to convert this value...

        switch (expectedType) {
        case BOOLEAN:
            switch (currentType) {
            case STRING:
                LOG.debug("Trying to convert string value {} to boolean", value);
                if (StringUtils.equalsIgnoreCase((String) value, "true")) {
                    return true;
                } else if (StringUtils.equalsIgnoreCase((String) value, "false")) {
                    return false;
                }
            case INTEGER:
                LOG.debug("Trying to convert int value {} to boolean", value);
                Long val = (Long) value;
                if (val == 1) {
                    return true;
                } else if (val == 0) {
                    return false;
                }
            default:
                break;
            }
            break;
        case TIME:
            switch (currentType) {
            case INTEGER:
                LOG.debug("Trying to convert long value {} to date", value);
                // let's assume we received the millisecond since 1970/1/1
                return new Date((Long) value);
            case STRING:
                LOG.debug("Trying to convert string value {} to date", value);
                // let's assume we received an ISO 8601 format date
                try {
                    return javax.xml.bind.DatatypeConverter.parseDateTime((String) value).getTime();
                } catch (IllegalArgumentException e) {
                    LOG.debug("Unable to convert string to date", e);
                }
            default:
                break;
            }
            break;
        case STRING:
            switch (currentType) {
            case BOOLEAN:
            case INTEGER:
            case FLOAT:
                return String.valueOf(value);
            default:
                break;
            }
            break;
        case OPAQUE:
            if (currentType == Type.STRING) {
                // let's assume we received an hexadecimal string
                LOG.debug("Trying to convert hexadecimal string {} to byte array", value);
                return javax.xml.bind.DatatypeConverter.parseHexBinary((String) value);
            }
            break;
        default:
        }

        throw new IllegalArgumentException("Invalid value type, expected " + expectedType + ", got " + currentType);
    }
}
