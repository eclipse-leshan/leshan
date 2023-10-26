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
package org.eclipse.leshan.transport.javacoap.identity;

import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.peer.LwM2mPeer;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.transport.TransportContext;

public class DefaultCoapIdentityHandler implements IdentityHandler {

    @Override
    public LwM2mPeer getIdentity(Object receivedMessage) {
        // TODO we need a Message Abstraction ?
        if (receivedMessage instanceof CoapRequest) {
            CoapRequest receivedRequest = (CoapRequest) receivedMessage;
            return getIdentity(receivedRequest);
        }
        return null;
    }

    protected LwM2mPeer getIdentity(CoapRequest receivedRequest) {
        return new IpPeer(receivedRequest.getPeerAddress());
    }

    @Override
    public TransportContext createTransportContext(LwM2mPeer client, boolean allowConnectionInitiation) {
        return TransportContext.EMPTY;
    }
}
