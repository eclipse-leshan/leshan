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
package org.eclipse.leshan;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.leshan.util.StringUtils;

/**
 * A Link as defined in http://tools.ietf.org/html/rfc6690.
 */
// TODO this class has several problems :
// 1) Parsing is too permissive...
// e.g. it should not accept URI-Reference which are not between < >.
// e.g URI-reference should be parsed to respect RFC3986.
//
// 2) we should have Parser/Serializer class instead of static method to allow custom implementation.
// 3) attributes is not well named, it does not respect rfc naming : url => uriRef
//
// Maybe we could look at existing implementation :
// https://github.com/google/coapblaster/blob/master/src/main/java/com/google/iot/coap/LinkFormat.java
// https://github.com/eclipse/californium/blob/2.0.x/californium-core/src/main/java/org/eclipse/californium/core/coap/LinkFormat.java
// https://github.com/ARMmbed/java-coap/blob/master/coap-core/src/main/java/com/mbed/coap/linkformat/LinkFormat.java
public class Link implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String url;

    private final Map<String, Object> attributes;

    /**
     * Creates a new Link without attributes.
     * 
     * @param url the object link URL
     */
    public Link(String url) {
        this(url, null);
    }

    /**
     * Creates a new link and with its attributes.
     * 
     * @param url the link URL
     * @param attributes the object link attributes or <code>null</code> if the link has no attributes
     */
    public Link(String url, Map<String, ?> attributes) {
        this.url = url;
        if (attributes != null) {
            this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
        } else {
            this.attributes = Collections.emptyMap();
        }
    }

    public String getUrl() {
        return url;
    }

    /**
     * Gets the link attributes
     * 
     * @return an unmodifiable map containing the link attributes
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('<');
        builder.append(getUrl());
        builder.append('>');

        Map<String, Object> attributes = getAttributes();
        if (attributes != null && !attributes.isEmpty()) {
            for (Entry<String, Object> entry : attributes.entrySet()) {
                builder.append(";");
                builder.append(entry.getKey());
                if (entry.getValue() != null) {
                    builder.append("=");
                    if (entry.getValue() instanceof String) {
                        builder.append("\"").append(entry.getValue()).append("\"");
                    } else {
                        builder.append(entry.getValue());
                    }
                }
            }
        }
        return builder.toString();
    }

    /**
     * Parse a byte arrays representation of a {@code String} encoding with UTF_8 {@link Charset}.
     * 
     * @param a byte arrays representing {@code String} encoding with UTF_8 {@link Charset}.
     * @return an array of {@code Link}
     */
    public static Link[] parse(byte[] content) {
        if (content == null) {
            return new Link[] {};
        }
        String s = new String(content, StandardCharsets.UTF_8);
        String[] links = s.split(",");
        Link[] linksResult = new Link[links.length];
        int index = 0;
        for (String link : links) {
            String[] linkParts = link.split(";");

            // clean URL
            String url = StringUtils.trim(linkParts[0]);
            url = StringUtils.removeStart(StringUtils.removeEnd(url, ">"), "<");

            // parse attributes
            Map<String, Object> attributes = new HashMap<>();

            if (linkParts.length > 1) {
                for (int i = 1; i < linkParts.length; i++) {
                    String[] attParts = linkParts[i].split("=");
                    if (attParts.length > 0) {
                        String key = attParts[0];
                        Object value = null;
                        if (attParts.length > 1) {
                            String rawvalue = attParts[1];
                            try {
                                value = Integer.valueOf(rawvalue);
                            } catch (NumberFormatException e) {

                                value = rawvalue.replaceFirst("^\"(.*)\"$", "$1");
                            }
                        }
                        attributes.put(key, value);
                    }
                }
            }
            linksResult[index] = new Link(url, attributes);
            index++;
        }
        return linksResult;
    }

    private static final String TRAILER = ",";

    /***
     * Serialize severals {@code Link} to {@code String} as defined in http://tools.ietf.org/html/rfc6690.
     * 
     * @param linkObjects links to serialize.
     * 
     * @return a {@code String} representation like defined in http://tools.ietf.org/html/rfc6690. If LinkObjects is
     *         empty return an empty {@code String};
     */
    public static String serialize(Link... linkObjects) {
        StringBuilder builder = new StringBuilder();
        if (linkObjects.length != 0) {
            builder.append(linkObjects[0].toString());
            for (int i = 1; i < linkObjects.length; i++) {
                builder.append(TRAILER).append(linkObjects[i].toString());
            }
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
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
        Link other = (Link) obj;
        if (attributes == null) {
            if (other.attributes != null)
                return false;
        } else if (!attributes.equals(other.attributes))
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        return true;
    }
}