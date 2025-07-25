/*******************************************************************************
 * Copyright (c) 2025 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.javacoap.server.coaps.bc.endpoint;

import java.net.InetSocketAddress;

import com.mbed.coap.transport.TransportContext;

public class ApplicationData {

    private final byte[] data;
    private final InetSocketAddress addr;
    private final TransportContext context;

    public ApplicationData(byte[] data, InetSocketAddress addr, TransportContext context) {
        super();
        this.data = data;
        this.addr = addr;
        this.context = context;
    }

    public byte[] getData() {
        return data;
    }

    public InetSocketAddress getAddr() {
        return addr;
    }

    public TransportContext getContext() {
        return context;
    }
}
