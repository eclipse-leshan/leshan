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
package org.eclipse.leshan.core;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.eclipse.leshan.core.util.StringUtils;
import org.eclipse.leshan.core.util.Validate;

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
public class Link {

    private final String url;

    private final Map<String, String> attributes;

    /**
     * Creates a new Link without attributes.
     * 
     * @param url the object link URL
     */
    public Link(String url) {
        this(url, (Map<String, String>) null);
    }

    /**
     * Creates a new link and with its attributes.
     * 
     * @param url the link URL
     * @param attributes the object link attributes or <code>null</code> if the link has no attributes
     */
    public Link(String url, Map<String, String> attributes) {
        Validate.notNull(url);
        this.url = url;
        if (attributes != null) {
            this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
        } else {
            this.attributes = Collections.emptyMap();
        }
    }

    /**
     * Creates a new link and with its attributes.
     * 
     * @param url the link URL
     * @param attributes the object link attributes or <code>null</code> if the link has no attributes
     */
    @SuppressWarnings("unchecked")
    public <T> Link(String url, Map<String, T> attributes, Class<T> clazz) {
        Validate.notNull(url);
        this.url = url;
        if (attributes == null || attributes.isEmpty()) {
            this.attributes = Collections.emptyMap();
        } else {
            if (String.class.equals(clazz)) {
                this.attributes = Collections.unmodifiableMap((Map<String, String>) new HashMap<>(attributes));
            } else {
                HashMap<String, String> attributesMap = new HashMap<>();
                for (Entry<String, T> attr : attributes.entrySet()) {
                    if (attr.getValue() == null) {
                        attributesMap.put(attr.getKey(), null);
                    } else {
                        attributesMap.put(attr.getKey(), attr.getValue().toString());
                    }

                }
                this.attributes = Collections.unmodifiableMap(attributesMap);
            }
        }
    }

    /**
     * Creates a new link and with its attributes.
     * 
     * @param url the link URL
     * @param attributes the object link attributes. The format is attributeKey1, attributeValue1, attributeKey2,
     *        attributeValue2. For empty attributes null value should be used.
     */
    public Link(String url, String... attributes) {
        Validate.notNull(url);
        this.url = url;
        if (attributes == null || attributes.length == 0) {
            this.attributes = Collections.emptyMap();
        } else {
            if (attributes.length % 2 != 0) {
                throw new IllegalArgumentException("Each attributes key must have a value");
            }

            HashMap<String, String> attributesMap = new HashMap<>();
            for (int i = 0; i < attributes.length; i = i + 2) {
                attributesMap.put(attributes[i], attributes[i + 1]);
            }
            this.attributes = Collections.unmodifiableMap(attributesMap);
        }
    }

    /**
     * Parse a byte arrays representation of a {@code String} encoding with UTF_8 {@link Charset}.
     *
     * @param bytes a byte arrays representing {@code String} encoding with UTF_8 {@link Charset}.
     * @return an array of {@code Link}
     */
    public static Link[] parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new Link[] {};
        }

        List<String> linkValueList = splitIgnoringEscaped(new String(bytes, StandardCharsets.UTF_8), ',');

        Link[] result = new Link[linkValueList.size()];
        for (int i = 0; i < linkValueList.size(); i++) {
            String linkValue = linkValueList.get(i);
            result[i] = Link.parsePart(linkValue);
        }

        return result;
    }

    private static List<String> splitIgnoringEscaped(String content, char delimiter) {
        List<String> linkValueList = new ArrayList<>();
        StringBuffer sb = new StringBuffer();
        boolean quote = false;
        boolean escape = false;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == '\\') {
                escape = !escape;
                sb.append(ch);
                continue;
            }

            if (ch == delimiter && !quote) {
                linkValueList.add(sb.toString());
                sb = new StringBuffer();
                escape = false;
                continue;
            }
            if (ch == '"' && !escape) {
                quote = !quote;
            }
            sb.append(ch);
            escape = false;
        }

        linkValueList.add(sb.toString());

        return linkValueList;
    }

    public String getUrl() {
        return url;
    }

    /**
     * Gets the link attributes
     * 
     * @return an unmodifiable map containing the link attributes
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('<');
        builder.append(getUrl());
        builder.append('>');

        Map<String, String> attributes = getAttributes();
        if (attributes != null && !attributes.isEmpty()) {
            for (Entry<String, String> entry : attributes.entrySet()) {
                builder.append(";");
                builder.append(entry.getKey());
                if (entry.getValue() != null) {
                    builder.append("=");
                    builder.append(entry.getValue());
                }
            }
        }
        return builder.toString();
    }

    public static Link parsePart(String linkValue) {

        List<String> parts = splitIgnoringEscaped(linkValue, ';');

        String uriReferenceDecorated = parts.get(0);

        validateUriReferenceDecorated(uriReferenceDecorated);

        Map<String, String> attributes = new HashMap<>();

        if (parts.size() > 1) {
            for (int i = 1; i < parts.size(); i++) {
                String[] attParts = parts.get(i).split("=", 2);

                if (attParts.length > 0) {
                    String key = attParts[0];

                    validateParmname(key);

                    String value = null;
                    if (attParts.length > 1) {
                        value = attParts[1];
                        validateValue(value);
                    }
                    attributes.put(key, applyCharEscaping(value));
                }
            }
        }

        return new Link(trimUriReference(uriReferenceDecorated), attributes);
    }

    private static String trimUriReference(String uriReferenceDecorated) {
        return StringUtils.removeStart(StringUtils.removeEnd(uriReferenceDecorated, ">"), "<");
    }

    private static void validateUriReferenceDecorated(String uriReferenceDecorated) {
        if (uriReferenceDecorated.length() <= 2) {
            throw new IllegalArgumentException("Invalid link-value");
        }
        if (uriReferenceDecorated.charAt(0) != '<' || uriReferenceDecorated.charAt(uriReferenceDecorated.length()-1) != '>') {
            throw new IllegalArgumentException("Invalid link-value");
        }
        String UriReference = trimUriReference(uriReferenceDecorated);
        URI.create(UriReference);
    }

    private static String applyCharEscaping(String value) {
        if (value == null) {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        boolean escape = false;
        for( int i = 0; i < value.length(); i++ ) {
            char ch = value.charAt(i);
            if (ch == '\\') {
                escape = !escape;
                if (escape) {
                    continue;
                }
            }

            if (escape && ch > 127) {
                sb.append("\\");
            }

            sb.append(ch);
        }
        return sb.toString();
    }

    // ( ptoken / quoted-string )
    private static void validateValue(String value) {
        if (value.length() == 0) {
            throw new IllegalArgumentException("Invalid link-extension value");
        }

        if (value.charAt(0) == '\"') {
            validateQuotedValue(value);
        } else {
            validatePtoken(value);
        }
    }

    private static void validatePtoken(String value) {
        Pattern pattern = Pattern.compile("[!#$%&'()*+\\-.:<=>?@\\[\\]^_`{|}~a-zA-Z0-9]+");
        if (!pattern.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid link-extension value");
        }
    }

    private static String removeEscapedChars(String value) {
        StringBuffer sb = new StringBuffer();
        boolean escape = false;
        for( int i = 0; i < value.length(); i++ ) {
            char ch = value.charAt(i);
            if (ch == '\\') {
                escape = !escape;
                continue;
            }

            if (!escape) {
                sb.append(ch);
            } else {
                escape = false;
            }
        }
        return sb.toString();
    }

    private static void validateQuotedValue(String value) {
        value = removeEscapedChars(value);
        if (value.charAt(value.length()-1) != '\"') {
            throw new IllegalArgumentException("Invalid link-extension value");
        }
    }

    // https://datatracker.ietf.org/doc/html/rfc5987
    private static void validateParmname(String parmname) {
        Pattern pattern = Pattern.compile("[!#$&+\\-.^_`|~a-zA-Z0-9]+");
        if (!pattern.matcher(parmname).matches()) {
            throw new IllegalArgumentException("Invalid link-extension parmname");
        }
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

    /**
     * remove quote from string, only if it begins and ends by a quote.
     * 
     * @param string to unquote
     * @return unquoted string or the original string if there no quote to remove.
     */
    public static String unquote(String string) {
        if (string != null && string.length() >= 2 && string.charAt(0) == '"'
                && string.charAt(string.length() - 1) == '"') {
            string = string.substring(1, string.length() - 1);
        }
        return string;
    }
}