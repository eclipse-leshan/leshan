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
 *******************************************************************************/

package org.eclipse.leshan.server.bootstrap.demo.servlet;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.EditableBootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;

/**
 * Servlet for REST API in charge of adding bootstrap information to the bootstrap server.
 */
public class BootstrapServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static class SignedByteUnsignedByteAdapter implements JsonSerializer<Byte>, JsonDeserializer<Byte> {

        @Override
        public Byte deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return json.getAsByte();
        }

        @Override
        public JsonElement serialize(Byte src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive((int) src & 0xff);
        }
    }

    private final EditableBootstrapConfigStore bsStore;

    private final Gson gson;

    public BootstrapServlet(EditableBootstrapConfigStore bsStore) {
        this.bsStore = bsStore;

        this.gson = new GsonBuilder().registerTypeHierarchyAdapter(Byte.class, new SignedByteUnsignedByteAdapter())
                .create();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo() != null) {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "bad URL");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.getOutputStream().write(gson.toJson(bsStore.getAll()).getBytes(StandardCharsets.UTF_8));
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
            BootstrapConfig cfg = gson.fromJson(new InputStreamReader(req.getInputStream()), BootstrapConfig.class);

            if (cfg == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "no content");
            } else {
                bsStore.add(endpoint, cfg);
                resp.setStatus(HttpServletResponse.SC_OK);
            }
        } catch (JsonSyntaxException | InvalidConfigurationException e) {
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