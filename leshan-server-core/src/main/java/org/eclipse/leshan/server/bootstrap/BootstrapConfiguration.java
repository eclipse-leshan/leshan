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
package org.eclipse.leshan.server.bootstrap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;

public class BootstrapConfiguration {

    private List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requests;

    public BootstrapConfiguration() {
        this.requests = Collections.emptyList();
    }

    @SafeVarargs
    public BootstrapConfiguration(BootstrapDownlinkRequest<? extends LwM2mResponse>... requests) {
        this.requests = Arrays.asList(requests);
    }

    public BootstrapConfiguration(List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requests) {
        this.requests = requests;
    }

    public List<BootstrapDownlinkRequest<? extends LwM2mResponse>> getRequests() {
        return requests;
    }
}
