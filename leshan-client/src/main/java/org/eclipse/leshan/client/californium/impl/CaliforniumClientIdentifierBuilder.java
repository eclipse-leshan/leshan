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

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.client.request.BootstrapRequest;
import org.eclipse.leshan.client.request.DeregisterRequest;
import org.eclipse.leshan.client.request.LwM2mClientRequestVisitor;
import org.eclipse.leshan.client.request.LwM2mIdentifierRequest;
import org.eclipse.leshan.client.request.RegisterRequest;
import org.eclipse.leshan.client.request.UpdateRequest;
import org.eclipse.leshan.client.request.identifier.ClientIdentifier;

public class CaliforniumClientIdentifierBuilder implements LwM2mClientRequestVisitor {

    private ClientIdentifier clientIdentifier;
    private final Response coapResponse;

    public CaliforniumClientIdentifierBuilder(final Response coapResponse) {
        this.coapResponse = coapResponse;
    }

    @Override
    public void visit(final RegisterRequest request) {
        buildClientIdentifier(request);
    }

    @Override
    public void visit(final DeregisterRequest request) {
        clientIdentifier = request.getClientIdentifier();
    }

    @Override
    public void visit(final UpdateRequest request) {
        clientIdentifier = request.getClientIdentifier();
    }

    @Override
    public void visit(final BootstrapRequest request) {
        throw new UnsupportedOperationException(
                "The Bootstrap Interface has not yet been fully implemented on the Leshan Client yet.");
    }

    private void buildClientIdentifier(final LwM2mIdentifierRequest request) {
        clientIdentifier = new CaliforniumClientIdentifier(coapResponse.getOptions().getLocationString(),
                request.getClientEndpointIdentifier());
    }

    public ClientIdentifier getClientIdentifier() {
        return clientIdentifier;
    }

}
