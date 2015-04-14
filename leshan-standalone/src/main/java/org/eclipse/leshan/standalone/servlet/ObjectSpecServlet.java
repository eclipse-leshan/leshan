/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.standalone.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.json.ObjectModelSerializer;
import org.eclipse.leshan.core.model.json.ResourceModelSerializer;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.client.Client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ObjectSpecServlet extends HttpServlet {

    // private static final Logger LOG = LoggerFactory.getLogger(ObjectSpecServlet.class);

    private static final long serialVersionUID = 1L;

    private final Gson gson;

    private final LwM2mServer server;

    public ObjectSpecServlet(LeshanServer lwServer) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(ObjectModel.class, new ObjectModelSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(ResourceModel.class, new ResourceModelSerializer());
        this.gson = gsonBuilder.create();
        this.server = lwServer;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');

        if (path.length == 0) {
            LwM2mModel model = server.getModelProvider().getObjectModel(null);

            String json = this.gson.toJson(model.getObjectModels().toArray(new ObjectModel[] {}));
            resp.setContentType("application/json");
            resp.getOutputStream().write(json.getBytes("UTF-8"));
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        if (path.length == 1) {
            String clientEndpoint = path[0];
            Client client = server.getClientRegistry().get(clientEndpoint);
            if (client != null) {
                LwM2mModel model = server.getModelProvider().getObjectModel(client);
                String json = this.gson.toJson(model.getObjectModels().toArray(new ObjectModel[] {}));
                resp.setContentType("application/json");
                resp.getOutputStream().write(json.getBytes("UTF-8"));
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
            }
            return;
        }
    }
}
