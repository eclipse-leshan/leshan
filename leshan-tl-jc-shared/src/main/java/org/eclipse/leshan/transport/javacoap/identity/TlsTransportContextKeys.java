/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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

import java.security.Principal;

import com.mbed.coap.transport.TransportContext;

public class TlsTransportContextKeys {
    public static final TransportContext.Key<String> TLS_SESSION_ID = new TransportContext.Key<>(null);
    public static final TransportContext.Key<String> CIPHER_SUITE = new TransportContext.Key<>(null);
    public static final TransportContext.Key<Principal> PRINCIPAL = new TransportContext.Key<>(null);
}
