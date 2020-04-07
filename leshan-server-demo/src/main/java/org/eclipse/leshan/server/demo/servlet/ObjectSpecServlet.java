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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.util.json.JsonException;
import org.eclipse.leshan.server.demo.model.ObjectModelSerDes;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationService;

public class ObjectSpecServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final ObjectModelSerDes serializer;

    private final LwM2mModelProvider modelProvider;
    private final RegistrationService registrationService;

    public ObjectSpecServlet(LwM2mModelProvider modelProvider, RegistrationService registrationService) {
        // use the provider from the server and return a model by client
        this.modelProvider = modelProvider;
        serializer = new ObjectModelSerDes();
        this.registrationService = registrationService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // Validate path : it must be /clientEndpoint
        if (req.getPathInfo() == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path");
            return;
        }
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        if (path.length != 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path");
            return;
        }

        // Get registration
        String clientEndpoint = path[0];
        Registration registration = registrationService.getByEndpoint(clientEndpoint);
        if (registration == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
        }

        // Get Model for this registration
        try {
            LwM2mModel model = modelProvider.getObjectModel(registration);
            resp.setContentType("application/json");
            resp.getOutputStream().write(serializer.bSerialize(model.getObjectModels()));
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (JsonException e) {
            throw new ServletException(e);
        }
        return;

    }
}
