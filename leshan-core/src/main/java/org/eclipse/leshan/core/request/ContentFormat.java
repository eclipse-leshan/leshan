/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.core.request;

/**
 * Data format defined by the LWM2M specification
 */
public enum ContentFormat {

    // TODO: update media type codes once they have been assigned by IANA
    LINK("application/link-format", 40), TEXT("application/vnd.oma.lwm2m+text", 1541), TLV(
            "application/vnd.oma.lwm2m+tlv", 1542), JSON("application/vnd.oma.lwm2m+json", 1543), OPAQUE(
            "application/vnd.oma.lwm2m+opaque", 1544);

    private final String mediaType;
    private final int code;

    private ContentFormat(String mediaType, int code) {
        this.mediaType = mediaType;
        this.code = code;
    }

    public String getMediaType() {
        return this.mediaType;
    }

    public int getCode() {
        return this.code;
    }

    /**
     * Find the {@link ContentFormat} for the given media type (<code>null</code> if not found)
     */
    public static ContentFormat fromMediaType(String mediaType) {
        for (ContentFormat t : ContentFormat.values()) {
            if (t.getMediaType().equals(mediaType)) {
                return t;
            }
        }
        return null;
    }

    /**
     * Finds the {@link ContentFormat} for a given media type code.
     *
     * @return the media type or <code>null</code> if the given code is unknown
     */
    public static ContentFormat fromCode(int code) {
        for (ContentFormat t : ContentFormat.values()) {
            if (t.getCode() == code) {
                return t;
            }
        }
        return null;
    }
}
