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
package org.eclipse.leshan.demo.servers.json.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.leshan.core.endpoint.LwM2mEndpoint;
import org.eclipse.leshan.demo.servers.json.PublicKeySerDes;
import org.eclipse.leshan.demo.servers.json.X509CertificateSerDes;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ServerServlet extends LeshanDemoServlet {

    private static final long serialVersionUID = 1L;

    private final transient X509CertificateSerDes certificateSerDes;
    private final transient PublicKeySerDes publicKeySerDes;
    private final transient List<? extends LwM2mEndpoint> endpoints;
    private final transient PublicKey publicKey;
    private final transient X509Certificate serverCertificate;

    public ServerServlet(List<? extends LwM2mEndpoint> endpoints, X509Certificate serverCertificate) {
        this.endpoints = endpoints;
        certificateSerDes = new X509CertificateSerDes();
        publicKeySerDes = new PublicKeySerDes();

        this.publicKey = null;
        this.serverCertificate = serverCertificate;
    }

    public ServerServlet(List<? extends LwM2mEndpoint> endpoints, PublicKey serverPublicKey) {
        this.endpoints = endpoints;
        certificateSerDes = new X509CertificateSerDes();
        publicKeySerDes = new PublicKeySerDes();
        this.publicKey = serverPublicKey;
        this.serverCertificate = null;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');

        if (path.length != 1) {
            safelySendError(req, resp, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if ("security".equals(path[0])) {
            sendServerSecurityInfo(req, resp);
            return;
        }

        // search coap and coaps port
        if ("endpoint".equals(path[0])) {
            sendEndpoints(req, resp);
            return;
        }

        safelySendError(req, resp, HttpServletResponse.SC_BAD_REQUEST);
    }

    private void sendServerSecurityInfo(HttpServletRequest req, HttpServletResponse resp) {
        executeSafely(req, resp, () -> {
            ObjectNode security = JsonNodeFactory.instance.objectNode();
            if (publicKey != null) {
                security.set("pubkey", publicKeySerDes.jSerialize(publicKey));
            } else if (serverCertificate != null) {
                security.set("certificate", certificateSerDes.jSerialize(serverCertificate));
            }
            resp.setContentType("application/json");
            resp.getOutputStream().write(security.toString().getBytes(StandardCharsets.UTF_8));
            resp.setStatus(HttpServletResponse.SC_OK);
        });
    }

    private void sendEndpoints(HttpServletRequest req, HttpServletResponse resp) {
        executeSafely(req, resp, () -> {
            ArrayNode endpointsArrayNode = JsonNodeFactory.instance.arrayNode();
            for (LwM2mEndpoint endpoint : endpoints) {
                ObjectNode ep = JsonNodeFactory.instance.objectNode();
                ObjectNode uri = JsonNodeFactory.instance.objectNode();
                ep.set("uri", uri);
                uri.put("full", endpoint.getURI().toString());
                uri.put("scheme", endpoint.getURI().getScheme());
                uri.put("host", endpoint.getURI().getHost());
                uri.put("port", endpoint.getURI().getPort());
                ep.put("description", endpoint.getDescription());
                endpointsArrayNode.add(ep);
            }
            resp.setContentType("application/json");
            resp.getOutputStream().write(endpointsArrayNode.toString().getBytes(StandardCharsets.UTF_8));
            resp.setStatus(HttpServletResponse.SC_OK);
        });
    }
}
