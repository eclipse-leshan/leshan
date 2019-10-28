/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
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
package org.eclipse.leshan.core.node;

public class LwM2mNodeUtil {

    public static boolean isUnsignedInt(Integer id) {
        return id != null && 0 <= id && id <= 65535;
    }

    public static boolean isValidObjectId(Integer id) {
        return isUnsignedInt(id);
    }

    public static void validateObjectId(Integer id) {
        if (!isValidObjectId(id)) {
            throw new IllegalArgumentException(String.format("Invalid object id %d, It MUST be an unsigned int.", id));
        }
    }

    public static boolean isValidObjectInstanceId(Integer id) {
        // MAX_ID 65535 is a reserved value and MUST NOT be used for identifying an Object Instance.
        return id != null && 0 <= id && id <= 65534;
    }

    public static void validateObjectInstanceId(Integer id) {
        if (!isValidObjectInstanceId(id)) {
            throw new IllegalArgumentException(String
                    .format("Invalid object instance id %d, It MUST be an unsigned int. (65535 is reserved)", id));
        }
    }

    public static boolean isValidResourceId(Integer id) {
        return isUnsignedInt(id);
    }

    public static void validateResourceId(Integer id) {
        if (!isValidResourceId(id)) {
            throw new IllegalArgumentException(
                    String.format("Invalid resource id %d, It MUST be an unsigned int.", id));
        }
    }

    public static boolean isValidResourceInstanceId(Integer id) {
        return isUnsignedInt(id);
    }

    public static void validateResourceInstanceId(Integer id) {
        if (!isValidResourceInstanceId(id)) {
            throw new IllegalArgumentException(
                    String.format("Invalid resource instance id %d, It MUST be an unsigned int.", id));
        }
    }
}
