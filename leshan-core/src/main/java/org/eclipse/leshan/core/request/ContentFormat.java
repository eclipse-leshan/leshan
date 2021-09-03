/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.core.request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;

/**
 * Data format defined by the LWM2M specification
 */
public class ContentFormat implements Comparable<ContentFormat> {
    public static final int TLV_CODE = 11542;
    public static final int JSON_CODE = 11543;
    public static final int TEXT_CODE = 0;
    public static final int OPAQUE_CODE = 42;
    public static final int LINK_CODE = 40;
    public static final int CBOR_CODE = 60;

    // Keep old code for backward-compatibility
    public static final int OLD_JSON_CODE = 1543;
    public static final int OLD_TLV_CODE = 1542;

    public static final int SENML_JSON_CODE = 110;
    public static final int SENML_CBOR_CODE = 112;

    public static final ContentFormat TLV = new ContentFormat("TLV", "application/vnd.oma.lwm2m+tlv", TLV_CODE,
            Arrays.asList(LwM2mVersion.V1_0));
    public static final ContentFormat JSON = new ContentFormat("JSON", "application/vnd.oma.lwm2m+json", JSON_CODE);
    public static final ContentFormat TEXT = new ContentFormat("TEXT", "text/plain", TEXT_CODE,
            Arrays.asList(LwM2mVersion.V1_1));
    public static final ContentFormat OPAQUE = new ContentFormat("OPAQUE", "application/octet-stream", OPAQUE_CODE,
            Arrays.asList(LwM2mVersion.V1_1));
    public static final ContentFormat LINK = new ContentFormat("LINK", "application/link-format", LINK_CODE,
            Arrays.asList(LwM2mVersion.V1_1));
    public static final ContentFormat SENML_JSON = new ContentFormat("SENML_JSON", "application/senml+json",
            SENML_JSON_CODE);
    public static final ContentFormat SENML_CBOR = new ContentFormat("SENML_CBOR", "application/senml+cbor",
            SENML_CBOR_CODE);
    public static final ContentFormat CBOR = new ContentFormat("CBOR", "application/cbor", CBOR_CODE);

    public static final ContentFormat DEFAULT = TLV;

    public static final ContentFormat knownContentFormat[] = new ContentFormat[] { TLV, JSON, SENML_JSON, SENML_CBOR,
            TEXT, OPAQUE, CBOR, LINK };

    private final String name;
    private final String mediaType;
    private final int code;
    private Set<LwM2mVersion> mandatoryForClient; // lwm2m version where this content format is mandatory at client side

    public ContentFormat(String name, String mediaType, int code, Collection<LwM2mVersion> mandatory) {
        this.name = name;
        this.mediaType = mediaType;
        this.code = code;
        this.mandatoryForClient = new HashSet<>(mandatory);
    }

    public ContentFormat(String name, String mediaType, int code) {
        this.name = name;
        this.mediaType = mediaType;
        this.code = code;
        this.mandatoryForClient = Collections.emptySet();
    }

    public ContentFormat(int code) {
        this.name = "UNKNOWN";
        this.mediaType = "unknown/unknown";
        this.code = code;
        this.mandatoryForClient = Collections.emptySet();
    }

    public String getName() {
        return this.name;
    }

    public String getMediaType() {
        return this.mediaType;
    }

    public int getCode() {
        return this.code;
    }

    /**
     * @return True is this {@link ContentFormat} is mandatory at client side for the given LWM2M {@link LwM2mVersion}.
     */
    public boolean isMandatoryForClient(LwM2mVersion lwM2mVersion) {
        return this.mandatoryForClient.contains(lwM2mVersion);
    }

    /**
     * Find the {@link ContentFormat} for the given media type (<code>null</code> if not found)
     */
    public static ContentFormat fromMediaType(String mediaType) {
        for (ContentFormat t : knownContentFormat) {
            if (t.getMediaType().equals(mediaType)) {
                return t;
            }
        }
        return null;
    }

    /**
     * Finds the {@link ContentFormat} for a given media type code.
     *
     * @return the media type or <i>unknown/unknown</i> if the given code is unknown
     * @exception NumberFormatException if code can not be parsed as an integer.
     */
    public static ContentFormat fromCode(String code) throws NumberFormatException {
        int iCode = Integer.parseInt(code);
        return fromCode(iCode);
    }

    /**
     * Finds the {@link ContentFormat} for a given media type code.
     *
     * @return the media type or <i>unknown/unknown</i> if the given code is unknown
     */
    public static ContentFormat fromCode(int code) {
        for (ContentFormat t : knownContentFormat) {
            if (t.getCode() == code) {
                return t;
            }
        }
        return new ContentFormat(code);
    }

    /**
     * Finds the {@link ContentFormat} by name.
     *
     * @return the media type or <code>null</code> if the given code is unknown
     */
    public static ContentFormat fromName(String name) {
        for (ContentFormat t : knownContentFormat) {
            if (t.getName().equals(name)) {
                return t;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name, code);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + code;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ContentFormat other = (ContentFormat) obj;
        if (code != other.code)
            return false;
        return true;
    }

    @Override
    public int compareTo(ContentFormat ct) {
        return Integer.compare(this.code, ct.code);
    }

    /**
     * From a list of {@link ContentFormat} of a client, return only the optional ones for a given LWM2M
     * {@link LwM2mVersion}. In other words we remove all {@link ContentFormat} which is considered as Mandatory.
     * 
     * @param contentFormat A list of all supported {@link ContentFormat} for a given device.
     * @param lwm2mVersion The LWM2M version targeted
     * @return only optional {@link ContentFormat}
     */
    public static List<ContentFormat> getOptionalContentFormatForClient(Collection<ContentFormat> contentFormat,
            LwM2mVersion lwm2mVersion) {
        List<ContentFormat> optionalFormat = new ArrayList<>();
        for (ContentFormat supportedFormat : contentFormat) {
            if (!supportedFormat.isMandatoryForClient(lwm2mVersion)) {
                optionalFormat.add(supportedFormat);
            }
        }
        return optionalFormat;
    }
}
