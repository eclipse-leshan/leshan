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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.leshan.util.Charsets;
import org.eclipse.leshan.util.StringUtils;

/**
 * A LwM2M path description given at the registration time by the client.
 */
public class LinkObject {

    private final String url;

    private final Map<String, Object> attributes;

    private final Integer objectId;

    private final Integer objectInstanceId;

    private final Integer resourceId;

    /**
     * Creates a new link object without attributes.
     * 
     * @param url the object link URL
     */
    public LinkObject(String url) {
        this(url, null);
    }

    /**
     * Creates a new instance from a URL and attributes.
     * 
     * @param url the object link URL
     * @param attributes the object link attributes or <code>null</code> if the link has no attributes
     */
    public LinkObject(String url, Map<String, ?> attributes) {
        this.url = url;
        if (attributes != null) {
            this.attributes = Collections.unmodifiableMap(new HashMap<String, Object>(attributes));
        } else {
            this.attributes = Collections.unmodifiableMap(new HashMap<String, Object>());
        }

        Matcher mat = Pattern.compile("(/(\\d+))(/(\\d+))?(/(\\d+))?").matcher(url);

        if (mat.find()) {
            objectId = mat.group(2) == null ? null : new Integer(mat.group(2));
            objectInstanceId = mat.group(4) == null ? null : new Integer(mat.group(4));
            resourceId = mat.group(6) == null ? null : new Integer(mat.group(6));
        } else {
            objectId = null;
            objectInstanceId = null;
            resourceId = null;
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

    public String getPath() {
        StringBuilder sb = new StringBuilder("/");
        if (objectId != null) {
            sb.append(objectId);
        }

        if (objectInstanceId != null) {
            sb.append("/").append(objectInstanceId);
        }

        if (resourceId != null) {
            sb.append("/").append(resourceId);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("LinkObject [url=%s, attributes=%s]", url, attributes);
    }

    public Integer getObjectId() {
        return objectId;
    }

    public Integer getObjectInstanceId() {
        return objectInstanceId;
    }

    public Integer getResourceId() {
        return resourceId;
    }

    public static LinkObject[] parse(byte[] content) {
        String s = new String(content, Charsets.UTF_8);
        String[] links = s.split(",");
        LinkObject[] linksResult = new LinkObject[links.length];
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
            linksResult[index] = new LinkObject(url, attributes);
            index++;
        }
        return linksResult;
    }
}