/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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

package org.eclipse.leshan.server.bootstrap.demo.servlet;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.EnumSet;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerSecurity;
import org.eclipse.leshan.server.bootstrap.EditableBootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;
import org.eclipse.leshan.server.bootstrap.demo.json.ByteArraySerializer;
import org.eclipse.leshan.server.bootstrap.demo.json.EnumSetDeserializer;
import org.eclipse.leshan.server.bootstrap.demo.json.EnumSetSerializer;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.CollectionType;

import jline.internal.Log;

/**
 * Servlet for REST API in charge of adding bootstrap information to the bootstrap server.
 */
public class BootstrapServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final ObjectMapper mapper;

    private final EditableBootstrapConfigStore bsStore;
    private final EditableSecurityStore securityStore;

    public BootstrapServlet(EditableBootstrapConfigStore bsStore, EditableSecurityStore securityStore) {
        this.bsStore = bsStore;
        this.securityStore = securityStore;

        mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(EnumSet.class, new EnumSetDeserializer());

        CollectionType collectionType = mapper.getTypeFactory().constructCollectionType(EnumSet.class, Object.class);
        module.addSerializer(new EnumSetSerializer(collectionType));

        module.addSerializer(new ByteArraySerializer(ByteArraySerializer.ByteMode.UNSIGNED));
        mapper.registerModule(module);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo() != null) {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "bad URL");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");

        try {
            resp.getOutputStream().write(mapper.writeValueAsString(bsStore.getAll()).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo() == null) {
            // we need the endpoint in the URL
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "endpoint name should be specified in the URL");
            return;
        }

        String[] path = StringUtils.split(req.getPathInfo(), '/');

        // endPoint
        if (path.length != 1) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "endpoint name should be specified in the URL, nothing more");
            return;
        }

        String endpoint = path[0];

        try {
            BootstrapConfig cfg = mapper.readValue(new InputStreamReader(req.getInputStream()), BootstrapConfig.class);

            if (cfg == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "no content");
            } else {
                // try to add securityInfo if needed.
                if (cfg.security != null) {
                    for (Map.Entry<Integer, BootstrapConfig.ServerSecurity> bsEntry : cfg.security.entrySet()) {
                        ServerSecurity value = bsEntry.getValue();
                        // Extract PSK security info
                        if (value.bootstrapServer) {
                            SecurityInfo securityInfo = null;
                            if (value.securityMode == SecurityMode.PSK) {
                                securityInfo = SecurityInfo.newPreSharedKeyInfo(endpoint,
                                        new String(value.publicKeyOrId, StandardCharsets.UTF_8), value.secretKey);
                            }
                            // Extract RPK security info
                            else if (value.securityMode == SecurityMode.RPK) {
                                securityInfo = SecurityInfo.newRawPublicKeyInfo(endpoint,
                                        SecurityUtil.publicKey.decode(value.publicKeyOrId));
                            }
                            // Extract X509 security info
                            else if (value.securityMode == SecurityMode.X509) {
                                securityInfo = SecurityInfo.newX509CertInfo(endpoint);
                            }
                            if (securityInfo != null) {
                                securityStore.add(securityInfo);
                            }
                            break;
                        }
                    }
                }

                // Add bootstrap config
                bsStore.add(endpoint, cfg);
                resp.setStatus(HttpServletResponse.SC_OK);
            }
        } catch (JsonParseException | InvalidConfigurationException | NonUniqueSecurityInfoException
                | GeneralSecurityException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo() == null) {
            // we need the endpoint in the URL
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "endpoint name should be specified in the URL");
            return;
        }

        String[] path = StringUtils.split(req.getPathInfo(), '/');

        // endPoint
        if (path.length != 1) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "endpoint name should be specified in the URL, nothing more");
            return;
        }

        String endpoint = path[0];

        // try to remove from securityStore
        try {
            securityStore.remove(endpoint, true);
        } catch (RuntimeException e) {
            // Best effort here we just try to remove, if it failed we try to remove bsconfig anyway.
            Log.warn("unable to remove security info for client {}", endpoint, e);
        }

        // try to remove from bootstrap config store
        if (bsStore.remove(endpoint) != null) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "no config for " + endpoint);
        }
    }

    private void sendError(HttpServletResponse resp, int statusCode, String errorMessage) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("text/plain; charset=UTF-8");
        if (errorMessage != null)
            resp.getOutputStream().write(errorMessage.getBytes(StandardCharsets.UTF_8));
    }
}