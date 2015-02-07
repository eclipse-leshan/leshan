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
package org.eclipse.leshan.client.request;

import java.util.Map;

public class RegisterRequest extends AbstractLwM2mClientRequest implements LwM2mContentRequest, LwM2mIdentifierRequest {
    private final Map<String, String> clientParameters;
    private final String clientEndpointIdentifier;

    public RegisterRequest(final String clientEndpointIdentifier, final Map<String, String> clientParameters) {
        this.clientEndpointIdentifier = clientEndpointIdentifier;
        this.clientParameters = clientParameters;
    }

    @Override
    public void accept(final LwM2mClientRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Map<String, String> getClientParameters() {
        return clientParameters;
    }

    @Override
    public String getClientEndpointIdentifier() {
        return clientEndpointIdentifier;
    }

}
