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
package org.eclipse.leshan.server.bootstrap.demo.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.eclipse.leshan.server.bootstrap.demo.json.SecuritySerializer;
import org.eclipse.leshan.server.californium.impl.LeshanBootstrapServer;
import org.eclipse.leshan.server.security.SecurityInfo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ServerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private LeshanBootstrapServer server;
    private Gson gsonSer;
    private PublicKey publicKey;

    public ServerServlet(LeshanBootstrapServer server, PublicKey serverPublicKey) {
        this.server = server;
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(SecurityInfo.class, new SecuritySerializer());
        this.gsonSer = builder.create();
        this.publicKey = serverPublicKey;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');

        if (path.length != 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if ("security".equals(path[0])) {
            String json = this.gsonSer.toJson(SecurityInfo.newRawPublicKeyInfo("leshan", publicKey));
            resp.setContentType("application/json");
            resp.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        if ("endpoint".equals(path[0])) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            resp.getOutputStream()
                    .write(String
                            .format("{ \"securedEndpointPort\":\"%s\", \"unsecuredEndpointPort\":\"%s\"}",
                                    server.getSecuredAddress().getPort(), server.getUnsecuredAddress().getPort())
                            .getBytes(StandardCharsets.UTF_8));
            return;
        }

        resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
}
