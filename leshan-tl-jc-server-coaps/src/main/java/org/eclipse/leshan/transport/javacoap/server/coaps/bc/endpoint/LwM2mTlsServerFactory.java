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

import org.bouncycastle.tls.TlsPSKIdentityManager;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import org.eclipse.leshan.servers.security.SecurityInfo;
import org.eclipse.leshan.servers.security.SecurityStore;
import org.eclipse.leshan.servers.security.ServerSecurityInfo;

public class LwM2mTlsServerFactory {

    private final ServerSecurityInfo serverSecurityInfo;
    private final SecurityStore securityStore;
    private final BcTlsCrypto crypto;

    public LwM2mTlsServerFactory(BcTlsCrypto crypto, ServerSecurityInfo securityInfo, SecurityStore securityStore) {
        this.crypto = crypto;
        this.serverSecurityInfo = securityInfo;
        this.securityStore = securityStore;
    }

    @SuppressWarnings("java:S1168")
    public LwM2mTlsServer createTlsServer(InetSocketAddress clientAddr) {
        return new LwM2mTlsServer(crypto, new TlsPSKIdentityManager() {

            @Override
            public byte[] getPSK(byte[] identity) {
                SecurityInfo clientSecurityInfo = securityStore.getByIdentity(new String(identity));
                if (clientSecurityInfo != null && clientSecurityInfo.usePSK())
                    // bouncy castle will erase the byte array so clone is needed.
                    return clientSecurityInfo.getPreSharedKey().clone();
                return null;
            }

            @Override
            public byte[] getHint() {
                return null;
            }
        }, serverSecurityInfo, clientAddr);
    }

    public BcTlsCrypto getCrypto() {
        return crypto;
    }
}
