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
package org.eclipse.leshan.server.demo.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.json.ObjectModelSerializer;
import org.eclipse.leshan.core.model.json.ResourceModelSerializer;
import org.eclipse.leshan.server.model.LwM2mModelProvider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ObjectSpecServlet extends HttpServlet {

    // private static final Logger LOG = LoggerFactory.getLogger(ObjectSpecServlet.class);

    private static final long serialVersionUID = 1L;

    private final Gson gson;

    private final LwM2mModelProvider modelProvider;

    public ObjectSpecServlet(LwM2mModelProvider pModelProvider) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(ObjectModel.class, new ObjectModelSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(ResourceModel.class, new ResourceModelSerializer());
        this.gson = gsonBuilder.create();

        // use the provider from the server and return a model by client
        modelProvider = pModelProvider;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo() == null) {
            LwM2mModel model = modelProvider.getObjectModel(null);

            String json = this.gson.toJson(model.getObjectModels().toArray(new ObjectModel[] {}));
            resp.setContentType("application/json");
            resp.getOutputStream().write(json.getBytes("UTF-8"));
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }
    }
}
