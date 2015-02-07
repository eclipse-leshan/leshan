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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.Value;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ValueResponse;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.request.ResourceAccessException;
import org.eclipse.leshan.standalone.servlet.json.ClientSerializer;
import org.eclipse.leshan.standalone.servlet.json.LwM2mNodeDeserializer;
import org.eclipse.leshan.standalone.servlet.json.LwM2mNodeSerializer;
import org.eclipse.leshan.standalone.servlet.json.ResponseSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Service HTTP REST API calls.
 */
public class ClientServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ClientServlet.class);

    private static final long serialVersionUID = 1L;

    private final LwM2mServer server;

    private final Gson gson;

    public ClientServlet(LwM2mServer server) {
        this.server = server;

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(Client.class, new ClientSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mResponse.class, new ResponseSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeDeserializer());
        gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        this.gson = gsonBuilder.create();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // all registered clients
        if (req.getPathInfo() == null) {
            Collection<Client> clients = server.getClientRegistry().allClients();

            String json = this.gson.toJson(clients.toArray(new Client[] {}));
            resp.setContentType("application/json");
            resp.getOutputStream().write(json.getBytes("UTF-8"));
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String[] path = StringUtils.split(req.getPathInfo(), '/');
        if (path.length < 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path");
            return;
        }
        String clientEndpoint = path[0];

        // /endPoint : get client
        if (path.length == 1) {
            Client client = server.getClientRegistry().get(clientEndpoint);
            if (client != null) {
                resp.setContentType("application/json");
                resp.getOutputStream().write(this.gson.toJson(client).getBytes("UTF-8"));
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
            }
            return;
        }

        // /clients/endPoint/LWRequest : do LightWeight M2M read request on a given client.
        try {
            String target = StringUtils.removeStart(req.getPathInfo(), "/" + clientEndpoint);
            Client client = server.getClientRegistry().get(clientEndpoint);
            if (client != null) {
                ReadRequest request = new ReadRequest(target);
                ValueResponse cResponse = server.send(client, request);
                processDeviceResponse(resp, cResponse);
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().format("No registered client with id '%s'", clientEndpoint).flush();
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid request", e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().append(e.getMessage()).flush();
        } catch (ResourceAccessException e) {
            LOG.warn(String.format("Error accessing resource %s%s.", req.getServletPath(), req.getPathInfo()), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append(e.getMessage()).flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        String clientEndpoint = path[0];

        // at least /endpoint/objectId/instanceId
        if (path.length < 3) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path");
            return;
        }

        try {
            String target = StringUtils.removeStart(req.getPathInfo(), "/" + clientEndpoint);
            Client client = server.getClientRegistry().get(clientEndpoint);
            if (client != null) {
                LwM2mResponse cResponse = this.writeRequest(client, target, req, resp);
                processDeviceResponse(resp, cResponse);
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().format("No registered client with id '%s'", clientEndpoint).flush();
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid request", e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().append(e.getMessage()).flush();
        } catch (ResourceAccessException e) {
            LOG.warn(String.format("Error accessing resource %s%s.", req.getServletPath(), req.getPathInfo()), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append(e.getMessage()).flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        String clientEndpoint = path[0];

        // /clients/endPoint/LWRequest/observe : do LightWeight M2M observe request on a given client.
        if (path.length >= 4 && "observe".equals(path[path.length - 1])) {
            try {
                String target = StringUtils.substringBetween(req.getPathInfo(), clientEndpoint, "/observe");
                Client client = server.getClientRegistry().get(clientEndpoint);
                if (client != null) {
                    ObserveRequest request = new ObserveRequest(target);
                    LwM2mResponse cResponse = server.send(client, request);
                    processDeviceResponse(resp, cResponse);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (IllegalArgumentException e) {
                LOG.warn("Invalid request", e);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().append(e.getMessage()).flush();
            } catch (ResourceAccessException e) {
                LOG.warn(String.format("Error accessing resource %s%s.", req.getServletPath(), req.getPathInfo()), e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().append(e.getMessage()).flush();
            }
            return;
        }

        String target = StringUtils.removeStart(req.getPathInfo(), "/" + clientEndpoint);

        // /clients/endPoint/LWRequest : do LightWeight M2M execute request on a given client.
        if (path.length == 4) {
            try {
                Client client = server.getClientRegistry().get(clientEndpoint);
                if (client != null) {
                    ExecuteRequest request = new ExecuteRequest(target);
                    LwM2mResponse cResponse = server.send(client, request);
                    processDeviceResponse(resp, cResponse);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (IllegalArgumentException e) {
                LOG.warn("Invalid request", e);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().append(e.getMessage()).flush();
            } catch (ResourceAccessException e) {
                LOG.warn(String.format("Error accessing resource %s%s.", req.getServletPath(), req.getPathInfo()), e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().append(e.getMessage()).flush();
            }
            return;
        }

        // /clients/endPoint/LWRequest : do LightWeight M2M create request on a given client.
        if (2 <= path.length && path.length <= 3) {
            try {
                Client client = server.getClientRegistry().get(clientEndpoint);
                if (client != null) {
                    LwM2mResponse cResponse = this.createRequest(client, target, req, resp);
                    processDeviceResponse(resp, cResponse);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (IllegalArgumentException e) {
                LOG.warn("Invalid request", e);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().append(e.getMessage()).flush();
            } catch (ResourceAccessException e) {
                LOG.warn(String.format("Error accessing resource %s%s.", req.getServletPath(), req.getPathInfo()), e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().append(e.getMessage()).flush();
            }
            return;
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        String clientEndpoint = path[0];

        // /clients/endPoint/LWRequest/observe : cancel observation for the given resource.
        if (path.length >= 4 && "observe".equals(path[path.length - 1])) {
            try {
                String target = StringUtils.substringsBetween(req.getPathInfo(), clientEndpoint, "/observe")[0];
                Client client = server.getClientRegistry().get(clientEndpoint);
                if (client != null) {
                    server.getObservationRegistry().cancelObservation(client, target);
                    resp.setStatus(HttpServletResponse.SC_OK);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (IllegalArgumentException e) {
                LOG.warn("Invalid request", e);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().append(e.getMessage()).flush();
            } catch (ResourceAccessException e) {
                LOG.warn(String.format("Error accessing resource %s%s.", req.getServletPath(), req.getPathInfo()), e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().append(e.getMessage()).flush();
            }
            return;
        }

        // /clients/endPoint/LWRequest/ : delete instance
        try {
            String target = StringUtils.removeStart(req.getPathInfo(), "/" + clientEndpoint);
            Client client = server.getClientRegistry().get(clientEndpoint);
            if (client != null) {
                DeleteRequest request = new DeleteRequest(target);
                LwM2mResponse cResponse = server.send(client, request);
                processDeviceResponse(resp, cResponse);
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid request", e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().append(e.getMessage()).flush();
        } catch (ResourceAccessException e) {
            LOG.warn(String.format("Error accessing resource %s%s.", req.getServletPath(), req.getPathInfo()), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append(e.getMessage()).flush();
        }
    }

    private void processDeviceResponse(HttpServletResponse resp, LwM2mResponse cResponse) throws IOException {
        String response = null;
        if (cResponse == null) {
            response = "Request timeout";
        } else {
            response = this.gson.toJson(cResponse);
        }
        resp.setContentType("application/json");
        resp.getOutputStream().write(response.getBytes());
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    // TODO refactor the code to remove this method.
    private LwM2mResponse writeRequest(Client client, String target, HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        Map<String, String> parameters = new HashMap<String, String>();
        String contentType = HttpFields.valueParameters(req.getContentType(), parameters);

        if ("text/plain".equals(contentType)) {
            String content = IOUtils.toString(req.getInputStream(), parameters.get("charset"));
            int rscId = Integer.valueOf(target.substring(target.lastIndexOf("/") + 1));
            return server.send(client, new WriteRequest(target,
                    new LwM2mResource(rscId, Value.newStringValue(content)), ContentFormat.TEXT, true));

        } else if ("application/json".equals(contentType)) {
            String content = IOUtils.toString(req.getInputStream(), parameters.get("charset"));
            LwM2mNode node = null;
            try {
                node = gson.fromJson(content, LwM2mNode.class);
            } catch (JsonSyntaxException e) {
                throw new IllegalArgumentException("unable to parse json to tlv:" + e.getMessage(), e);
            }
            return server.send(client, new WriteRequest(target, node, null, true));

        } else {
            throw new IllegalArgumentException("content type " + req.getContentType()
                    + " not supported for write requests");
        }
    }

    // TODO refactor the code to remove this method.
    private LwM2mResponse createRequest(Client client, String target, HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        Map<String, String> parameters = new HashMap<String, String>();
        String contentType = HttpFields.valueParameters(req.getContentType(), parameters);
        if ("application/json".equals(contentType)) {
            String content = IOUtils.toString(req.getInputStream(), parameters.get("charset"));
            LwM2mNode node;
            try {
                node = gson.fromJson(content, LwM2mNode.class);
            } catch (JsonSyntaxException e) {
                throw new IllegalArgumentException("unable to parse json to tlv:" + e.getMessage(), e);
            }
            if (!(node instanceof LwM2mObjectInstance)) {
                throw new IllegalArgumentException("payload must contain an object instance");
            }

            return server.send(client, new CreateRequest(target, (LwM2mObjectInstance) node, ContentFormat.TLV));
        } else {
            throw new IllegalArgumentException("content type " + req.getContentType()
                    + " not supported for write requests");
        }
    }

}
