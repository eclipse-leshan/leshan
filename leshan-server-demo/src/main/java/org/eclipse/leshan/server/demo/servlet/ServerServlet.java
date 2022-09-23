/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
 *     Orange - keep one JSON dependency
 *******************************************************************************/
package org.eclipse.leshan.server.demo.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.core.demo.json.PublicKeySerDes;
import org.eclipse.leshan.server.core.demo.json.X509CertificateSerDes;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpoint;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ServerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final X509CertificateSerDes certificateSerDes;
    private final PublicKeySerDes publicKeySerDes;

    private final LeshanServer server;
    private final PublicKey publicKey;
    private final X509Certificate serverCertificate;

    public ServerServlet(LeshanServer server, X509Certificate serverCertificate) {
        this.server = server;
        certificateSerDes = new X509CertificateSerDes();
        publicKeySerDes = new PublicKeySerDes();

        this.publicKey = null;
        this.serverCertificate = serverCertificate;
    }

    public ServerServlet(LeshanServer server, PublicKey serverPublicKey) {
        this.server = server;
        certificateSerDes = new X509CertificateSerDes();
        publicKeySerDes = new PublicKeySerDes();
        this.publicKey = serverPublicKey;
        this.serverCertificate = null;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');

        if (path.length != 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if ("security".equals(path[0])) {
            ObjectNode security = JsonNodeFactory.instance.objectNode();
            if (publicKey != null) {
                security.set("pubkey", publicKeySerDes.jSerialize(publicKey));
            } else if (serverCertificate != null) {
                security.set("certificate", certificateSerDes.jSerialize(serverCertificate));
            }
            resp.setContentType("application/json");
            resp.getOutputStream().write(security.toString().getBytes(StandardCharsets.UTF_8));
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // search coap and coaps port
        Integer coapPort = null;
        Integer coapsPort = null;
        for (LwM2mServerEndpoint endpoint : server.getEndpoints()) {
            if (endpoint.getProtocol().equals(Protocol.COAP)) {
                coapPort = endpoint.getURI().getPort();
            } else if (endpoint.getProtocol().equals(Protocol.COAPS)) {
                coapsPort = endpoint.getURI().getPort();
            }
        }

        if ("endpoint".equals(path[0])) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            resp.getOutputStream().write(String
                    .format("{ \"securedEndpointPort\":\"%s\", \"unsecuredEndpointPort\":\"%s\"}", coapsPort, coapPort)
                    .getBytes(StandardCharsets.UTF_8));
            return;
        }

        resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
}
