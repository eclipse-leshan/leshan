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
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.leshan.core.objectspec.ObjectSpec;
import org.eclipse.leshan.core.objectspec.ResourceSpec;
import org.eclipse.leshan.core.objectspec.Resources;
import org.eclipse.leshan.core.objectspec.json.ObjectSpecSerializer;
import org.eclipse.leshan.core.objectspec.json.ResourceSpecSerializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ObjectSpecServlet extends HttpServlet {

    // private static final Logger LOG = LoggerFactory.getLogger(ObjectSpecServlet.class);

    private static final long serialVersionUID = 1L;

    private final Gson gson;

    public ObjectSpecServlet() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(ObjectSpec.class, new ObjectSpecSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(ResourceSpec.class, new ResourceSpecSerializer());
        this.gson = gsonBuilder.create();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo() == null) {
            Collection<ObjectSpec> objectSpecs = Resources.getObjectSpecs();

            String json = this.gson.toJson(objectSpecs.toArray(new ObjectSpec[] {}));
            resp.setContentType("application/json");
            resp.getOutputStream().write(json.getBytes("UTF-8"));
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }
    }
}
