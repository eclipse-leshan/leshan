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

import java.net.InetSocketAddress;

import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.peer.LwM2mPeer;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.SeparateResponse;
import com.mbed.coap.transport.TransportContext;

public class DefaultCoapIdentityHandler implements IdentityHandler {

    @Override
    public LwM2mPeer getIdentity(Object receivedMessage) {
        // TODO we need a Message Abstraction ?
        if (receivedMessage instanceof CoapRequest) {
            CoapRequest receivedRequest = (CoapRequest) receivedMessage;
            return getIdentity(receivedRequest);
        } else if (receivedMessage instanceof SeparateResponse) {
            SeparateResponse separatedResponse = (SeparateResponse) receivedMessage;
            return getIdentity(separatedResponse);
        }
        return null;
    }

    protected LwM2mPeer getIdentity(CoapRequest receivedRequest) {
        return getIdentity(receivedRequest.getPeerAddress(), receivedRequest.getTransContext());
    }

    protected LwM2mPeer getIdentity(SeparateResponse separatedResponse) {
        return getIdentity(separatedResponse.getPeerAddress(), separatedResponse.getTransContext());
    }

    protected LwM2mPeer getIdentity(InetSocketAddress address, TransportContext context) {
        return new IpPeer(address);
    }

    @Override
    public TransportContext createTransportContext(LwM2mPeer client, boolean allowConnectionInitiation) {
        return TransportContext.EMPTY;
    }
}
