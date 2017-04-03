/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.californium.impl;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.scandium.auth.PreSharedKeyIdentity;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.junit.Test;

public class EstResourceTest {

    @Test
    public void post_broken_CSR() {
        BootstrapSecurityStore bss = mock(BootstrapSecurityStore.class);
        PkiService pki = mock(PkiService.class);
        EstResource res = new EstResource(bss, pki);

        CoapExchange ex = mock(CoapExchange.class);
        OptionSet os = new OptionSet();
        os.addUriPath("est");
        os.addUriPath("sen");
        when(ex.getRequestOptions()).thenReturn(os);

        Exchange exchange = mock(Exchange.class);

        Request r = mock(Request.class);
        when(r.getPayload()).thenReturn("CRAP".getBytes());
        when(exchange.getRequest()).thenReturn(r);

        when(ex.advanced()).thenReturn(exchange);
        res.handlePOST(ex);

        verify(ex).respond(ResponseCode.BAD_REQUEST, "Corrupted CSR");
        verifyNoMoreInteractions(bss);
    }

    @Test
    public void post_CSR() {
        BootstrapSecurityStore bss = mock(BootstrapSecurityStore.class);

        when(bss.getByIdentity("PSK_Identity"))
                .thenReturn(SecurityInfo.newPreSharedKeyInfo("123456789", "PSK_Identity", "SecrectMe".getBytes()));

        PkiService pki = mock(PkiService.class);
        when(pki.enroll(any(byte[].class), anyString(), any(Map.class))).thenReturn("Enrolled!".getBytes());
        EstResource res = new EstResource(bss, pki);

        CoapExchange ex = mock(CoapExchange.class);
        OptionSet os = new OptionSet();
        os.addUriPath("est");
        os.addUriPath("sen");
        when(ex.getRequestOptions()).thenReturn(os);

        Exchange exchange = mock(Exchange.class);

        Request r = mock(Request.class);

        // the CN = 123456789
        byte[] rawCSR = Base64.getDecoder().decode(
                "MIIBFTCBuwIBADBZMQswCQYDVQQGEwJBVTETMBEGA1UECAwKU29tZS1TdGF0ZTEhMB8GA1UECgwYSW50ZXJuZXQgV2lkZ2l0cyBQdHkgTHRkMRIwEAYDVQQDDAkxMjM0NTY3ODkwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAASq6dH0k6JhBE/huod2o4VRAG5amwYVyXzyMNn+Ud4S54dWumDkINBdaQqvgi2ds+P3aoeidZ+KpHePfq1AWL95oAAwCgYIKoZIzj0EAwIDSQAwRgIhAPLvVe6/ee/0qBObTN/Iud3y1uMhcSnQU4M7+qsM6UfiAiEAphWCeGjxtjZcwDHbsXe0iEePaVy+RG6l+tpc7XqiDIQ=");

        when(r.getPayload()).thenReturn(rawCSR);

        when(r.getSenderIdentity()).thenReturn(new PreSharedKeyIdentity("PSK_Identity"));

        when(exchange.getRequest()).thenReturn(r);

        when(ex.advanced()).thenReturn(exchange);
        res.handlePOST(ex);

        verify(ex).respond(ResponseCode.CONTENT, "Enrolled!".getBytes());
        verify(pki).enroll(rawCSR, "123456789", new HashMap<String, Object>());
    }
}
