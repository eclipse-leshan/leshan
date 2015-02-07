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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.util;

import static org.eclipse.californium.core.coap.LinkFormat.CONTENT_TYPE;
import static org.eclipse.californium.core.coap.LinkFormat.INTERFACE_DESCRIPTION;
import static org.eclipse.californium.core.coap.LinkFormat.MAX_SIZE_ESTIMATE;
import static org.eclipse.californium.core.coap.LinkFormat.OBSERVABLE;
import static org.eclipse.californium.core.coap.LinkFormat.RESOURCE_TYPE;

import java.util.Map;

import org.eclipse.leshan.LinkObject;

public class LinkFormatUtils {
    public static final String INVALID_LINK_PAYLOAD = "<>";

    private static final String TRAILER = ", ";

    public static String payloadize(final LinkObject... linkObjects) {
        try {
            final StringBuilder builder = new StringBuilder();
            for (final LinkObject link : linkObjects) {
                builder.append(payloadizeLink(link)).append(TRAILER);
            }

            builder.delete(builder.length() - TRAILER.length(), builder.length());

            return builder.toString();
        } catch (final Exception e) {
            return INVALID_LINK_PAYLOAD;
        }
    }

    private static String payloadizeLink(final LinkObject link) {
        final StringBuilder builder = new StringBuilder();
        builder.append('<');
        builder.append(link.getUrl());
        builder.append('>');

        final Map<String, Object> attributes = link.getAttributes();

        if (hasPayloadAttributes(attributes)) {
            builder.append(";");
            if (attributes.containsKey(RESOURCE_TYPE)) {
                builder.append(RESOURCE_TYPE).append("=\"").append(attributes.get(RESOURCE_TYPE)).append("\"");
            }
            if (attributes.containsKey(INTERFACE_DESCRIPTION)) {
                builder.append(INTERFACE_DESCRIPTION).append("=\"").append(attributes.get(INTERFACE_DESCRIPTION))
                        .append("\"");
            }
            if (attributes.containsKey(CONTENT_TYPE)) {
                builder.append(CONTENT_TYPE).append("=\"").append(attributes.get(CONTENT_TYPE)).append("\"");
            }
            if (attributes.containsKey(MAX_SIZE_ESTIMATE)) {
                builder.append(MAX_SIZE_ESTIMATE).append("=\"").append(attributes.get(MAX_SIZE_ESTIMATE)).append("\"");
            }
            if (attributes.containsKey(OBSERVABLE)) {
                builder.append(OBSERVABLE);
            }
        }

        return builder.toString();
    }

    private static boolean hasPayloadAttributes(final Map<String, Object> attributes) {
        return attributes.containsKey(RESOURCE_TYPE) || //
                attributes.containsKey(INTERFACE_DESCRIPTION) || //
                attributes.containsKey(CONTENT_TYPE) || //
                attributes.containsKey(MAX_SIZE_ESTIMATE) || //
                attributes.containsKey(OBSERVABLE);
    }
}