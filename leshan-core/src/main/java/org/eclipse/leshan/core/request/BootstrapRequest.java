/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.BootstrapResponse;

/**
 * The request to send to start a bootstrap session
 */
public class BootstrapRequest implements UplinkRequest<BootstrapResponse> {

    private final String endpointName;
    private final Map<String, String> additionalAttributes;

    public BootstrapRequest(String endpointName) throws InvalidRequestException {
        this(endpointName, null);
    }

    /** @since 1.1 */
    public BootstrapRequest(String endpointName, Map<String, String> additionalAttributes)
            throws InvalidRequestException {
        if (endpointName == null || endpointName.isEmpty())
            throw new InvalidRequestException("endpoint is mandatory");

        this.endpointName = endpointName;
        if (additionalAttributes == null)
            this.additionalAttributes = Collections.emptyMap();
        else
            this.additionalAttributes = Collections.unmodifiableMap(new HashMap<>(additionalAttributes));
    }

    public String getEndpointName() {
        return endpointName;
    }

    /** @since 1.1 */
    public Map<String, String> getAdditionalAttributes() {
        return additionalAttributes;
    }

    @Override
    public void accept(UplinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((endpointName == null) ? 0 : endpointName.hashCode());
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
        BootstrapRequest other = (BootstrapRequest) obj;
        if (endpointName == null) {
            if (other.endpointName != null)
                return false;
        } else if (!endpointName.equals(other.endpointName))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("BootstrapRequest [endpointName=%s, additionalAttributes=%s]", endpointName,
                additionalAttributes);
    }
}
