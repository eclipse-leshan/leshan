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

package org.eclipse.leshan.demo.bsserver.servlet;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

import org.apache.commons.lang.StringUtils;
import org.eclipse.leshan.bsserver.BootstrapConfig;
import org.eclipse.leshan.bsserver.EditableBootstrapConfigStore;
import org.eclipse.leshan.bsserver.InvalidConfigurationException;
import org.eclipse.leshan.demo.bsserver.json.ByteArraySerializer;
import org.eclipse.leshan.demo.bsserver.json.EnumSetDeserializer;
import org.eclipse.leshan.demo.bsserver.json.EnumSetSerializer;
import org.eclipse.leshan.demo.servers.json.servlet.LeshanDemoServlet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.CollectionType;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet for REST API in charge of adding bootstrap information to the bootstrap server.
 */
public class BootstrapServlet extends LeshanDemoServlet {

    private static final long serialVersionUID = 1L;

    private final transient ObjectMapper mapper;
    private final transient EditableBootstrapConfigStore bsStore;

    public BootstrapServlet(EditableBootstrapConfigStore bsStore) {
        this.bsStore = bsStore;

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
            safelySendError(req, resp, HttpServletResponse.SC_NOT_FOUND, "bad URL");
            return;
        }

        sendBootstrapConfig(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo() == null) {
            // we need the endpoint in the URL
            safelySendError(req, resp, HttpServletResponse.SC_BAD_REQUEST,
                    "endpoint name should be specified in the URL");
            return;
        }

        String[] path = StringUtils.split(req.getPathInfo(), '/');

        // endPoint
        if (path.length != 1) {
            safelySendError(req, resp, HttpServletResponse.SC_BAD_REQUEST,
                    "endpoint name should be specified in the URL, nothing more");
            return;
        }

        String endpoint = path[0];

        addBootstrapConfig(req, resp, endpoint);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo() == null) {
            // we need the endpoint in the URL
            safelySendError(req, resp, HttpServletResponse.SC_BAD_REQUEST,
                    "endpoint name should be specified in the URL");
            return;
        }

        String[] path = StringUtils.split(req.getPathInfo(), '/');

        // endPoint
        if (path.length != 1) {
            safelySendError(req, resp, HttpServletResponse.SC_BAD_REQUEST,
                    "endpoint name should be specified in the URL, nothing more");
            return;
        }

        String endpoint = path[0];

        // try to remove from bootstrap config store
        deleteBootstrapConfig(req, resp, endpoint);
    }

    private void sendBootstrapConfig(HttpServletRequest req, HttpServletResponse resp) {
        executeSafely(req, resp, () -> {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            resp.getOutputStream().write(mapper.writeValueAsString(bsStore.getAll()).getBytes(StandardCharsets.UTF_8));
        });
    }

    private void addBootstrapConfig(HttpServletRequest req, HttpServletResponse resp, String endpoint) {
        executeSafely(req, resp, () -> {
            try {
                BootstrapConfig cfg = mapper.readValue(new InputStreamReader(req.getInputStream()),
                        BootstrapConfig.class);

                if (cfg == null) {
                    safelySendError(req, resp, HttpServletResponse.SC_BAD_REQUEST, "no content");
                } else {
                    // Add bootstrap config
                    bsStore.add(endpoint, cfg);
                    resp.setStatus(HttpServletResponse.SC_OK);
                }
            } catch (JsonParseException | InvalidConfigurationException e) {
                safelySendError(req, resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            }
        });
    }

    private void deleteBootstrapConfig(HttpServletRequest req, HttpServletResponse resp, String endpoint) {
        executeSafely(req, resp, () -> {
            if (bsStore.remove(endpoint) != null) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                safelySendError(req, resp, HttpServletResponse.SC_NOT_FOUND, "no config for " + endpoint);
            }
        });
    }
}
