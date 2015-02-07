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
package org.eclipse.leshan.client.californium.impl;

import java.util.Objects;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.leshan.client.request.identifier.ClientIdentifier;

public class CaliforniumClientIdentifier implements ClientIdentifier {

    private final String location;
    private final String endpointIdentifier;

    public CaliforniumClientIdentifier(final String location, final String endpointIdentifier) {
        this.location = location;
        this.endpointIdentifier = endpointIdentifier;
    }

    public String getLocation() {
        return location;
    }

    public String getEndpointIdentifier() {
        return endpointIdentifier;
    }

    @Override
    public void accept(final Request coapRequest) {
        final String[] locationPaths = location.split("/");
        for (final String location : locationPaths) {
            if (location.length() != 0) {
                coapRequest.getOptions().addUriPath(location);
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ClientIdentifier[").append(getEndpointIdentifier()).append("|").append(getLocation())
                .append("]");

        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpointIdentifier, location);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof CaliforniumClientIdentifier)) {
            return false;
        }

        CaliforniumClientIdentifier other = (CaliforniumClientIdentifier) obj;

        boolean isEqual = true;
        isEqual = isEqual && Objects.equals(location, other.getLocation());
        isEqual = isEqual && Objects.equals(endpointIdentifier, other.getEndpointIdentifier());

        return isEqual;
    }
}
