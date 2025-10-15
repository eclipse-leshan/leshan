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
package org.eclipse.leshan.demo.server.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.demo.server.model.ObjectModelSerDes;
import org.eclipse.leshan.demo.servers.json.servlet.LeshanDemoServlet;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.registration.IRegistration;
import org.eclipse.leshan.server.registration.RegistrationService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ObjectSpecServlet extends LeshanDemoServlet {

    private static final long serialVersionUID = 1L;

    private final transient ObjectModelSerDes serializer;
    private final transient LwM2mModelProvider modelProvider;
    private final transient RegistrationService registrationService;

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
            // we need the endpoint in the URL
            safelySendError(req, resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid path");
            return;
        }

        // Get registration
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        if (path.length != 1) {
            safelySendError(req, resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid path");
            return;
        }
        String clientEndpoint = path[0];
        IRegistration registration = registrationService.getByEndpoint(clientEndpoint);
        if (registration == null) {
            safelySendError(req, resp, HttpServletResponse.SC_BAD_REQUEST,
                    String.format("no registered client with id '%s'", clientEndpoint));
        }

        // Get Model for this registration
        sendModel(req, resp, registration);
    }

    private void sendModel(HttpServletRequest req, HttpServletResponse resp, IRegistration registration) {
        executeSafely(req, resp, () -> {
            LwM2mModel model = modelProvider.getObjectModel(registration);
            resp.setContentType("application/json");
            List<ObjectModel> objectModels = new ArrayList<>(model.getObjectModels());
            Collections.sort(objectModels, (o1, o2) -> {
                return Integer.compare(o1.id, o2.id);
            });

            resp.getOutputStream().write(serializer.bSerialize(objectModels));
            resp.setStatus(HttpServletResponse.SC_OK);
        });
    }
}
