/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.server.demo.servlet;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.eclipse.leshan.server.demo.servlet.json.PublicKeySerDes;
import org.eclipse.leshan.server.demo.servlet.json.SecurityDeserializer;
import org.eclipse.leshan.server.demo.servlet.json.SecuritySerializer;
import org.eclipse.leshan.server.demo.servlet.json.X509CertificateSerDes;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

/**
 * Service HTTP REST API calls for security information.
 */
public class SecurityServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityServlet.class);

    private static final long serialVersionUID = 1L;

    private final EditableSecurityStore store;
    private final PublicKey serverPublicKey;
    private final X509Certificate serverCertificate;

    private final X509CertificateSerDes certificateSerDes;
    private final PublicKeySerDes publicKeySerDes;
    // TODO we must remove Gson dependency.
    private final Gson gsonSer;
    private final Gson gsonDes;

    public SecurityServlet(EditableSecurityStore store, X509Certificate serverCertificate) {
        this(store, null, serverCertificate);
    }

    public SecurityServlet(EditableSecurityStore store, PublicKey serverPublicKey) {
        this(store, serverPublicKey, null);
    }

    protected SecurityServlet(EditableSecurityStore store, PublicKey serverPublicKey,
            X509Certificate serverCertificate) {
        this.store = store;
        this.serverPublicKey = serverPublicKey;
        this.serverCertificate = serverCertificate;
        certificateSerDes = new X509CertificateSerDes();
        publicKeySerDes = new PublicKeySerDes();

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(SecurityInfo.class, new SecuritySerializer());
        this.gsonSer = builder.create();

        builder = new GsonBuilder();
        builder.registerTypeAdapter(SecurityInfo.class, new SecurityDeserializer());
        this.gsonDes = builder.create();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');

        if (path.length != 1 && "clients".equals(path[0])) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            SecurityInfo info = gsonDes.fromJson(new InputStreamReader(req.getInputStream()), SecurityInfo.class);
            LOG.debug("New security info for end-point {}: {}", info.getEndpoint(), info);

            store.add(info);

            resp.setStatus(HttpServletResponse.SC_OK);

        } catch (NonUniqueSecurityInfoException e) {
            LOG.warn("Non unique security info: " + e.getMessage());
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().append(e.getMessage()).flush();
        } catch (JsonParseException e) {
            LOG.warn("Could not parse request body", e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().append("Invalid request body").flush();
        } catch (RuntimeException e) {
            LOG.warn("unexpected error for request " + req.getPathInfo(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');

        if (path.length != 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if ("clients".equals(path[0])) {
            Collection<SecurityInfo> infos = this.store.getAll();

            String json = this.gsonSer.toJson(infos);
            resp.setContentType("application/json");
            resp.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        if ("server".equals(path[0])) {
            JsonObject security = new JsonObject();
            if (serverPublicKey != null) {
                security.add("pubkey", publicKeySerDes.jSerialize(serverPublicKey));
            } else if (serverCertificate != null) {
                security.add("certificate", certificateSerDes.jSerialize(serverCertificate));
            }
            resp.setContentType("application/json");
            resp.getOutputStream().write(security.toString().getBytes(StandardCharsets.UTF_8));
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String endpoint = StringUtils.substringAfter(req.getPathInfo(), "/clients/");
        if (StringUtils.isEmpty(endpoint)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        LOG.debug("Removing security info for end-point {}", endpoint);
        if (this.store.remove(endpoint, true) != null) {
            resp.sendError(HttpServletResponse.SC_OK);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
