/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.core.observation;

import java.util.Arrays;
import java.util.Objects;

import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.util.Hex;

/**
 * An Observation Identifier.
 */
public class ObservationIdentifier {

    private final byte[] bytes;
    private final EndpointUri endpointUri;

    public ObservationIdentifier(EndpointUri uri, byte[] bytes) {
        this.endpointUri = uri;
        this.bytes = Arrays.copyOf(bytes, bytes.length);
    }

    public final byte[] getBytes() {
        return Arrays.copyOf(this.bytes, length());
    }

    public EndpointUri getEndpointUri() {
        return endpointUri;
    }

    public String getAsHexString() {
        return Hex.encodeHexString(bytes);
    }

    public boolean isEmpty() {
        return bytes.length == 0;
    }

    public int length() {
        return bytes.length;
    }

    @Override
    public String toString() {
        return String.format("Ox%s[%s]", getAsHexString(), getEndpointUri());
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(bytes);
        result = prime * result + Objects.hash(endpointUri);
        return result;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof ObservationIdentifier))
            return false;
        ObservationIdentifier other = (ObservationIdentifier) obj;
        return Arrays.equals(bytes, other.bytes) && Objects.equals(endpointUri, other.endpointUri);
    }
}
