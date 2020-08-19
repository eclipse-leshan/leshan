/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.server.californium.request;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.stack.ReliabilityLayerParameters;
import org.eclipse.leshan.server.request.LowerLayerConfig;

public class CoapRequestSetter implements LowerLayerConfig {

    private final ReliabilityLayerParameters reliabilityLayerParameters;

    private CoapRequestSetter(ReliabilityLayerParameters reliabilityLayerParameters) {
        this.reliabilityLayerParameters = reliabilityLayerParameters;
    }

    @Override
    public void apply(Object lowerRequest) {
        if (lowerRequest instanceof Request) {
            applySetting((Request) lowerRequest);
        }
    }

    protected void applySetting(Request coapRequest) {
        if (reliabilityLayerParameters != null)
            coapRequest.setReliabilityLayerParameters(reliabilityLayerParameters);
    }

    public static CoapRequestSetter reliabilitySetter(ReliabilityLayerParameters.Builder reliabilityParametersBuilder) {
        return builder().setReliabilityLayerParameters(reliabilityParametersBuilder.build()).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ReliabilityLayerParameters reliabilityLayerParameters;

        private Builder() {
        }

        public Builder setReliabilityLayerParameters(ReliabilityLayerParameters reliabilityLayerParameters) {
            this.reliabilityLayerParameters = reliabilityLayerParameters;
            return this;
        }

        public CoapRequestSetter build() {
            return new CoapRequestSetter(reliabilityLayerParameters);
        }
    }
}
