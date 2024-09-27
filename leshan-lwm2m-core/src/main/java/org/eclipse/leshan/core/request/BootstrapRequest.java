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
import java.util.Objects;

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
        implements UplinkBootstrapRequest<BootstrapResponse> {

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
    public void accept(UplinkBootstrapRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(UplinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BootstrapRequest))
            return false;
        BootstrapRequest that = (BootstrapRequest) o;
        return that.canEqual(this) && Objects.equals(endpointName, that.endpointName)
                && Objects.equals(additionalAttributes, that.additionalAttributes)
                && Objects.equals(preferredContentFormat, that.preferredContentFormat);
    }

    public boolean canEqual(Object o) {
        return (o instanceof BootstrapRequest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpointName, additionalAttributes, preferredContentFormat);
    }
}
