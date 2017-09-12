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
import org.eclipse.leshan.core.model.json.ObjectModelSerDes;
import org.eclipse.leshan.server.model.LwM2mModelProvider;

public class ObjectSpecServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final ObjectModelSerDes serializer;

    private final LwM2mModelProvider modelProvider;

    public ObjectSpecServlet(LwM2mModelProvider pModelProvider) {
        // use the provider from the server and return a model by client
        modelProvider = pModelProvider;
        serializer = new ObjectModelSerDes();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo() == null) {
            LwM2mModel model = modelProvider.getObjectModel(null);
            resp.setContentType("application/json");
            resp.getOutputStream().write(serializer.bSerialize(model.getObjectModels()));
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }
    }
}
