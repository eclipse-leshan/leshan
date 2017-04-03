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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.core.californium.ExchangeUtil;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.security.pkcs10.PKCS10;
import sun.security.pkcs10.PKCS10Attribute;

/**
 * Implements the needed CoAP EST resources, based on https://tools.ietf.org/html/draft-vanderstok-ace-coap-est-01 URLs:
 * /est/crts (CA certs) /est/sen (simple enroll) /est/sren (simple re-enroll)
 */
@SuppressWarnings("restriction")
public class EstResource extends CoapResource {

    private static final Logger LOG = LoggerFactory.getLogger(EstResource.class);

    private BootstrapSecurityStore bsSecurityStore;

    private PkiService pki;

    public EstResource(BootstrapSecurityStore bsSecurityStore, PkiService pki) {
        super("est");
        this.bsSecurityStore = bsSecurityStore;
        this.pki = pki;
    }

    @Override
    public void handleRequest(Exchange exchange) {
        try {
            super.handleRequest(exchange);
        } catch (RuntimeException e) {
            LOG.error(String.format("Exception while handling EST request(%s)", exchange.getRequest()), e);
            exchange.sendResponse(new Response(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * Handle the /est/crts URL which is used for receiving the root CA + intermediates certificate chain. It's a public
     * API
     */
    @Override
    public void handleGET(CoapExchange exchange) {
        Request request = exchange.advanced().getRequest();
        LOG.debug("GET received : {}", request);

        List<String> uri = exchange.getRequestOptions().getUriPath();
        if (uri.size() != 2 || !"est".equals(uri.get(0)) || !"crts".equals(uri.get(1))) {
            exchange.respond(ResponseCode.NOT_FOUND);
            return;
        }

        exchange.respond(ResponseCode.CONTENT, pki.getCaCertificates());
    }

    @Override
    public void handlePOST(CoapExchange exchange) {
        Request request = exchange.advanced().getRequest();
        LOG.debug("POST received : {}", request);
        List<String> uri = exchange.getRequestOptions().getUriPath();
        if (uri.size() != 2 || !"est".equals(uri.get(0)) || !"sen".equals(uri.get(1))) {
            exchange.respond(ResponseCode.NOT_FOUND);
            return;
        }
        if (request.getPayload() == null || request.getPayload().length <= 0) {
            exchange.respond(ResponseCode.BAD_REQUEST, "PKCS#10 payload mandatory");
            return;
        }
        // open the PCKS10 (Certificate Signing Request)
        try {
            PKCS10 csr = new PKCS10(request.getPayload());

            String csrIdentity = csr.getSubjectName().getCommonName();

            Map<String, Object> attributes = new HashMap<>();

            for (PKCS10Attribute att : csr.getAttributes().getAttributes()) {
                attributes.put(att.getAttributeId().toString(), att.getAttributeValue());
            }
            System.err.println(csrIdentity);
            // looks at the DTLS session
            Identity dtlsIdentity = ExchangeUtil.extractIdentity(exchange);

            // try to find the end-point corresponding to the used DTLS credentials
            String ep = "";

            if (dtlsIdentity.isPSK()) {
                SecurityInfo secInfo = bsSecurityStore.getByIdentity(dtlsIdentity.getPskIdentity());
                if (secInfo == null) {
                    exchange.respond(ResponseCode.BAD_REQUEST, "no endpoint associated with your DTLS identity");
                    return;
                }
                ep = secInfo.getEndpoint();
            } else if (dtlsIdentity.isRPK()) {
                // not supported
                exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR, "DTLS RPK authentication not supported");
                return;
            } else if (dtlsIdentity.isX509()) {
                // extract CN=
                ep = dtlsIdentity.getX509CommonName();
            } else {
                exchange.respond(ResponseCode.BAD_REQUEST, "You must be authenticated using DTLS");
                return;
            }

            // the CN in the CSR must match the end-point in the security store
            if (!csrIdentity.equals(ep)) {
                exchange.respond(ResponseCode.BAD_REQUEST, "Your CSR CN doesn't match your credentials");
                return;
            }

            // enroll
            byte[] x509 = pki.enroll(request.getPayload(), ep, attributes);
            exchange.respond(ResponseCode.CONTENT, x509);
        } catch (IOException e) {
            LOG.error("Unexpected IOException", e);
            exchange.respond(ResponseCode.BAD_REQUEST, "Corrupted CSR");
            return;
        } catch (SignatureException e) {
            LOG.debug("Invalid PKCS10 signature", e);
            exchange.respond(ResponseCode.BAD_REQUEST, "Invalid signature");
            return;
        } catch (NoSuchAlgorithmException e) {
            LOG.warn("Unsupported PKCS10 signature", e);
            exchange.respond(ResponseCode.BAD_REQUEST, "Unsupported algorithm");
        }
    }
}