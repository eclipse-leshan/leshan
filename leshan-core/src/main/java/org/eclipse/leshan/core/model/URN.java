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
package org.eclipse.leshan.core.model;

/**
 * Some utility methods about object URN field of LWM2M Object Model
 * 
 * @since 1.1
 */
public class URN {
    public static final String OMA_LABEL = "oma";
    public static final String EXT_LABEL = "ext";
    public static final String X_LABEL = "x";
    public static final String INVALID_LABEL = "invalid";
    public static final String RESERVED_LABEL = "reserved";

    /**
     * Generate URN from object Id and Object version.
     */
    public static String generateURN(int objectId, String objectVersion) {
        StringBuilder urn = new StringBuilder("urn:oma:lwm2m");
        urn.append(":").append(getUrnKind(objectId));
        urn.append(":").append(objectId);
        if (objectVersion != null && !objectVersion.isEmpty() && !objectVersion.equals(ObjectModel.DEFAULT_VERSION))
            urn.append(":").append(objectVersion);
        return urn.toString();
    }

    /**
     * Return URN "kind" from object id.
     * 
     * @param objectId
     * @return {@link #OMA_LABEL}, {@link #EXT_LABEL} or {@link #X_LABEL} for valid object id. {@link #INVALID_LABEL} is
     *         returned for invalid id and {@link #RESERVED_LABEL} for reserved range.
     * 
     * @see <a href="http://www.openmobilealliance.org/wp/OMNA/LwM2M/LwM2MRegistry.html">OMA LightweightM2M (LwM2M)
     *      Object and Resource Registry </a>
     */
    public static String getUrnKind(int objectId) {
        if (0 <= objectId && objectId <= 1023)
            return OMA_LABEL;
        if (1024 <= objectId && objectId <= 2047)
            return RESERVED_LABEL;
        if (2048 <= objectId && objectId <= 10240)
            return EXT_LABEL;
        if (10241 <= objectId && objectId <= 42768)
            return X_LABEL;
        return INVALID_LABEL;
    }
}
