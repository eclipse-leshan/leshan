/*******************************************************************************
 * Copyright (c) 2026    Sierra Wireless and others.
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
package org.eclipse.leshan.bsserver.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Iterator;

import org.eclipse.leshan.bsserver.DefaultBootstrapAuthorizer;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.peer.OscoreIdentity;
import org.eclipse.leshan.core.peer.PskIdentity;
import org.eclipse.leshan.core.peer.X509Identity;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.servers.security.SecurityInfo;
import org.junit.jupiter.api.Test;

class DefaultBootstrapAuthorizerTest {

    private static final InetSocketAddress ADDRESS = new InetSocketAddress("127.0.0.1", 5684);

    /** Knows only the security info of the endpoint "device-1", which authenticates with an X.509 certificate. */
    private final DefaultBootstrapAuthorizer authorizer = new DefaultBootstrapAuthorizer(new BootstrapSecurityStore() {
        @Override
        public Iterator<SecurityInfo> getAllByEndpoint(String endpoint) {
            return "device-1".equals(endpoint)
                    ? Collections.singletonList(SecurityInfo.newX509CertInfo("device-1")).iterator()
                    : Collections.<SecurityInfo> emptyList().iterator();
        }

        @Override
        public SecurityInfo getByIdentity(String pskIdentity) {
            return null;
        }

        @Override
        public SecurityInfo getByOscoreIdentity(OscoreIdentity oscoreIdentity) {
            return null;
        }
    });

    @Test
    void x509_client_which_sends_its_endpoint_name_is_authorized() {
        LwM2mPeer client = new IpPeer(ADDRESS, new X509Identity("device-1"));

        assertTrue(authorizer.isAuthorized("device-1", new BootstrapRequest("device-1"), client).isApproved());
    }

    @Test
    void x509_client_which_does_not_send_its_endpoint_name_is_authorized() {
        // The request has no endpoint name: the bootstrap handler found it from the identity (the CN of the
        // certificate) and gives it to the authorizer.
        LwM2mPeer client = new IpPeer(ADDRESS, new X509Identity("device-1"));

        assertTrue(authorizer.isAuthorized("device-1", new BootstrapRequest(null), client).isApproved());
    }

    @Test
    void x509_client_with_another_common_name_is_declined() {
        LwM2mPeer client = new IpPeer(ADDRESS, new X509Identity("device-2"));

        assertFalse(authorizer.isAuthorized("device-1", new BootstrapRequest("device-1"), client).isApproved());
        assertFalse(authorizer.isAuthorized("device-1", new BootstrapRequest(null), client).isApproved());
    }

    @Test
    void psk_client_is_declined_for_an_endpoint_which_uses_x509() {
        LwM2mPeer client = new IpPeer(ADDRESS, new PskIdentity("device-1"));

        assertFalse(authorizer.isAuthorized("device-1", new BootstrapRequest(null), client).isApproved());
    }
}
