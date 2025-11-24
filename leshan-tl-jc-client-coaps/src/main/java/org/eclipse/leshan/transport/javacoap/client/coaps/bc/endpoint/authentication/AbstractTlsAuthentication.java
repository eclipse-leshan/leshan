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
package org.eclipse.leshan.transport.javacoap.client.coaps.bc.endpoint.authentication;

import org.bouncycastle.tls.TlsAuthentication;
import org.bouncycastle.tls.TlsContext;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;

public abstract class AbstractTlsAuthentication implements TlsAuthentication {

    private final BcTlsCrypto crypto;
    private final TlsContext context;

    protected AbstractTlsAuthentication(BcTlsCrypto crypto, TlsContext context) {
        this.crypto = crypto;
        this.context = context;
    }

    public BcTlsCrypto getCrypto() {
        return crypto;
    }

    public TlsContext getContext() {
        return context;
    }
}
