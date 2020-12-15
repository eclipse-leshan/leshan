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
 * 
 * @see <a href=
 *      "http://www.openmobilealliance.org/release/LightweightM2M/V1_1_1-20190617-A/HTML-Version/OMA-TS-LightweightM2M_Core-V1_1_1-20190617-A.html#6-1-7-1-0-6171-Bootstrap-Request-Operation">6.1.7.1.
 *      Bootstrap-Request Operation</a>
 */
public class BootstrapRequest extends AbstractLwM2mRequest<BootstrapResponse>
        implements UplinkRequest<BootstrapResponse> {

    private final String endpointName;
    private final Map<String, String> additionalAttributes;
    private final ContentFormat preferredContentFormat;

    public BootstrapRequest(String endpointName) throws InvalidRequestException {
        this(endpointName, null, null);
    }

    public BootstrapRequest(String endpointName, ContentFormat preferredFormat,
            Map<String, String> additionalAttributes) throws InvalidRequestException {
        this(endpointName, preferredFormat, additionalAttributes, null);
    }

    public BootstrapRequest(String endpointName, ContentFormat preferredFormat,
            Map<String, String> additionalAttributes, Object coapRequest) throws InvalidRequestException {
        super(coapRequest);
        if (endpointName == null || endpointName.isEmpty())
            throw new InvalidRequestException("endpoint is mandatory");

        this.endpointName = endpointName;
        if (additionalAttributes == null)
            this.additionalAttributes = Collections.emptyMap();
        else
            this.additionalAttributes = Collections.unmodifiableMap(new HashMap<>(additionalAttributes));

        // handle preferred format
        if (preferredFormat == null) {
            this.preferredContentFormat = null;
        } else {
            // The Content Format MUST be one of the SenML JSON, SenML CBOR, or TLV formats.
            switch (preferredFormat.getCode()) {
            case ContentFormat.TLV_CODE:
            case ContentFormat.SENML_CBOR_CODE:
            case ContentFormat.SENML_JSON_CODE:
            case ContentFormat.OLD_TLV_CODE:
                this.preferredContentFormat = preferredFormat;
                break;
            default:
                throw new InvalidRequestException(
                        "Invalid preferred content format %s, it MUST be SenML JSON, SenML CBOR, or TLV",
                        preferredFormat);
            }
        }
    }

    public String getEndpointName() {
        return endpointName;
    }

    /** @since 1.1 */
    public Map<String, String> getAdditionalAttributes() {
        return additionalAttributes;
    }

    /**
     * The preferred Content Format for bootstrap configuration. The Content Format MUST be one of the SenML JSON, SenML
     * CBOR, or TLV formats.
     */
    public ContentFormat getPreferredContentFormat() {
        return preferredContentFormat;
    }

    @Override
    public void accept(UplinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((additionalAttributes == null) ? 0 : additionalAttributes.hashCode());
        result = prime * result + ((endpointName == null) ? 0 : endpointName.hashCode());
        result = prime * result + ((preferredContentFormat == null) ? 0 : preferredContentFormat.hashCode());
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
        if (additionalAttributes == null) {
            if (other.additionalAttributes != null)
                return false;
        } else if (!additionalAttributes.equals(other.additionalAttributes))
            return false;
        if (endpointName == null) {
            if (other.endpointName != null)
                return false;
        } else if (!endpointName.equals(other.endpointName))
            return false;
        if (preferredContentFormat == null) {
            if (other.preferredContentFormat != null)
                return false;
        } else if (!preferredContentFormat.equals(other.preferredContentFormat))
            return false;
        return true;
    }
}
