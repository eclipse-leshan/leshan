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
 *     Orange - keep one JSON dependency
 *******************************************************************************/
package org.eclipse.leshan.demo.server.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.attributes.InvalidAttributeException;
import org.eclipse.leshan.core.link.lwm2m.attributes.DefaultLwM2mAttributeParser;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeParser;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.node.LwM2mChildNode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.CancelCompositeObservationRequest;
import org.eclipse.leshan.core.request.CancelObservationRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.DownlinkDeviceManagementRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.SimpleDownlinkRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.request.exception.ClientSleepingException;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.demo.server.servlet.json.JacksonLinkSerializer;
import org.eclipse.leshan.demo.server.servlet.json.JacksonLwM2mNodeDeserializer;
import org.eclipse.leshan.demo.server.servlet.json.JacksonLwM2mNodeSerializer;
import org.eclipse.leshan.demo.server.servlet.json.JacksonRegistrationSerializer;
import org.eclipse.leshan.demo.server.servlet.json.JacksonResponseSerializer;
import org.eclipse.leshan.demo.server.servlet.queuemode.QueueHandler;
import org.eclipse.leshan.demo.servers.json.servlet.LeshanDemoServlet;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.registration.IRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Service HTTP REST API calls.
 */
public class ClientServlet extends LeshanDemoServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(ClientServlet.class);

    private static final String APPLICATION_JSON = "application/json";

    private static final String ERROR_MESSAGE_NO_REGISTERED = "No registered client with id '%s'";
    private static final String ERROR_MESSAGE_INVALID_PATH = "Invalid path";

    public static final String URI_DELIMITER = "/";
    private static final String OBSERVE_PATH_PART = "observe";
    private static final String COMPOSITE_PATH_PART = "composite";

    private static final String FORMAT_PARAM = "format";
    private static final String TIMEOUT_PARAM = "timeout";
    private static final String REPLACE_PARAM = "replace";
    private static final String ACTIVE_CANCEL = "active";

    // for composite operation
    private static final String PATH_PARAM = "paths";
    private static final String PATH_FORMAT_PARAM = "pathformat";
    private static final String NODE_FORMAT_PARAM = "nodeformat";

    private static final long DEFAULT_TIMEOUT = 5000; // ms

    private final transient LeshanServer server;
    private final transient ObjectMapper mapper;
    private final transient LwM2mAttributeParser attributeParser;
    private final transient QueueHandler queueHandler;
    private final transient EventServlet eventServlet;

    public ClientServlet(LeshanServer server, EventServlet servlet) {
        this.server = server;
        this.queueHandler = new QueueHandler(server);
        this.eventServlet = servlet;

        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        SimpleModule module = new SimpleModule();
        module.addSerializer(Link.class, new JacksonLinkSerializer());
        module.addSerializer(IRegistration.class, new JacksonRegistrationSerializer(server.getPresenceService()));
        module.addSerializer(LwM2mResponse.class, new JacksonResponseSerializer());
        module.addSerializer(LwM2mNode.class, new JacksonLwM2mNodeSerializer());
        module.addDeserializer(LwM2mNode.class, new JacksonLwM2mNodeDeserializer());
        mapper.registerModule(module);
        attributeParser = new DefaultLwM2mAttributeParser();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // all registered clients
        if (req.getPathInfo() == null) {
            sendAllRegistration(req, resp);
            return;
        }

        // get registration
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        if (path.length < 1) {
            safelySendError(req, resp, HttpServletResponse.SC_BAD_REQUEST, ERROR_MESSAGE_INVALID_PATH);
            return;
        }
        String clientEndpoint = path[0];
        IRegistration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
        if (registration == null) {
            safelySendError(req, resp, HttpServletResponse.SC_BAD_REQUEST,
                    String.format(ERROR_MESSAGE_NO_REGISTERED, clientEndpoint));
            return;
        }

        // /endPoint : get client
        if (path.length == 1) {
            sendRegistration(req, resp, registration);
            return;
        }
        // /composite : do Read-Composite request.
        if (path.length == 2 && COMPOSITE_PATH_PART.equals(path[1])) {
            sendReadCompositeRequest(req, resp, registration);
            return;
        }

        // /clients/endPoint/LWRequest/discover : do LightWeight M2M discover request on a given client.
        if (path.length >= 3 && "discover".equals(path[path.length - 1])) {
            String lwm2mpath = StringUtils.substringBetween(req.getPathInfo(), clientEndpoint, "/discover");
            sendDiscover(req, resp, registration, lwm2mpath);
            return;
        }

        // /clients/endPoint/LWRequest : do LightWeight M2M read request on a given client.
        String lwm2mpath = Strings.CS.removeStart(req.getPathInfo(), URI_DELIMITER + clientEndpoint);
        sendReadRequest(req, resp, registration, lwm2mpath);
    }

    private void sendReadRequest(HttpServletRequest req, HttpServletResponse resp, IRegistration registration,
            String lwm2mpath) {
        executeSafely(req, resp, () -> {
            try {
                // get content format
                String contentFormatParam = req.getParameter(FORMAT_PARAM);
                ContentFormat contentFormat = contentFormatParam != null
                        ? ContentFormat.fromName(contentFormatParam.toUpperCase())
                        : null;

                // create & process request
                ReadRequest request = new ReadRequest(contentFormat, lwm2mpath);
                sendRequestAndWriteResponse(registration, request, req, resp);
            } catch (RuntimeException e) {
                handleException(e, resp);
            }
        });
    }

    private void sendDiscover(HttpServletRequest req, HttpServletResponse resp, IRegistration registration,
            String lwm2mpath) {
        executeSafely(req, resp, () -> {
            try {
                // create & process request
                DiscoverRequest request = new DiscoverRequest(lwm2mpath);
                sendRequestAndWriteResponse(registration, request, req, resp);
            } catch (RuntimeException e) {
                handleException(e, resp);
            }
        });
    }

    private void sendRegistration(HttpServletRequest req, HttpServletResponse resp, IRegistration registration) {
        executeSafely(req, resp, () -> {
            resp.setContentType(APPLICATION_JSON);
            resp.getOutputStream().write(this.mapper.writeValueAsString(registration).getBytes(StandardCharsets.UTF_8));
            resp.setStatus(HttpServletResponse.SC_OK);
        });
    }

    private void sendAllRegistration(HttpServletRequest req, HttpServletResponse resp) {
        executeSafely(req, resp, () -> {
            Collection<IRegistration> registrations = new ArrayList<>();
            for (Iterator<IRegistration> iterator = server.getRegistrationService().getAllRegistrations(); iterator
                    .hasNext();) {
                registrations.add(iterator.next());
            }

            String json = this.mapper.writeValueAsString(registrations.toArray(new IRegistration[] {}));
            resp.setContentType(APPLICATION_JSON);
            resp.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
            resp.setStatus(HttpServletResponse.SC_OK);
        });
    }

    private void sendReadCompositeRequest(HttpServletRequest req, HttpServletResponse resp,
            IRegistration registration) {
        executeSafely(req, resp, () -> {
            try {
                // get paths
                String pathParam = req.getParameter(PATH_PARAM);
                List<String> paths = Arrays.asList(pathParam.split(","));

                // get content format
                String pathContentFormatParam = req.getParameter(PATH_FORMAT_PARAM);
                ContentFormat pathContentFormat = pathContentFormatParam != null
                        ? ContentFormat.fromName(pathContentFormatParam.toUpperCase())
                        : null;
                String nodeContentFormatParam = req.getParameter(NODE_FORMAT_PARAM);
                ContentFormat nodeContentFormat = nodeContentFormatParam != null
                        ? ContentFormat.fromName(nodeContentFormatParam.toUpperCase())
                        : null;

                // create & process request
                ReadCompositeRequest request = new ReadCompositeRequest(pathContentFormat, nodeContentFormat, paths);
                sendRequestAndWriteResponse(registration, request, req, resp);
            } catch (RuntimeException e) {
                handleException(e, resp);
            }
        });
    }

    private void handleException(Exception e, HttpServletResponse resp) throws IOException {
        if (e instanceof InvalidRequestException || e instanceof CodecException
                || e instanceof ClientSleepingException) {
            LOG.warn("Invalid request", e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().append("Invalid request: ").append(e.getMessage()).flush();
        } else if (e instanceof RequestRejectedException) {
            LOG.warn("Request rejected", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append("Request rejected: ").append(e.getMessage()).flush();
        } else if (e instanceof RequestCanceledException) {
            LOG.warn("Request cancelled", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append("Request cancelled: ").append(e.getMessage()).flush();
        } else if (e instanceof InvalidResponseException) {
            LOG.warn("Invalid response", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append("Invalid Response: ").append(e.getMessage()).flush();
        } else {
            LOG.warn("Unexpected exception", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append("Unexpected exception: ").append(e.getMessage()).flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');

        // get registration
        if (path.length < 1) {
            safelySendError(req, resp, HttpServletResponse.SC_BAD_REQUEST, ERROR_MESSAGE_INVALID_PATH);
            return;
        }
        String clientEndpoint = path[0];
        IRegistration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
        if (registration == null) {
            safelySendError(req, resp, HttpServletResponse.SC_BAD_REQUEST,
                    String.format(ERROR_MESSAGE_NO_REGISTERED, clientEndpoint));
            return;
        }

        // /clients/endPoint/composite : do LightWeight M2M WriteComposite request on a given client.
        if (path.length == 2 && COMPOSITE_PATH_PART.equals(path[1])) {
            sendWriteCompositeRequest(req, resp, registration);
            return;
        }

        // at least /endpoint/objectId/instanceId
        if (path.length < 3) {
            safelySendError(req, resp, HttpServletResponse.SC_BAD_REQUEST, ERROR_MESSAGE_INVALID_PATH);
            return;
        }

        String lwm2mPath = Strings.CS.removeStart(req.getPathInfo(), URI_DELIMITER + clientEndpoint);
        if (path.length >= 3 && "attributes".equals(path[path.length - 1])) {
            lwm2mPath = Strings.CS.removeEnd(lwm2mPath, path[path.length - 1]);
            sendWriteAttributeRequest(req, resp, registration, lwm2mPath);
        } else {
            sendWriteRequest(req, resp, registration, lwm2mPath);
        }
    }

    private void sendWriteRequest(HttpServletRequest req, HttpServletResponse resp, IRegistration registration,
            String lwm2mPath) {
        executeSafely(req, resp, () -> {
            try {
                // get content format
                String contentFormatParam = req.getParameter(FORMAT_PARAM);
                ContentFormat contentFormat = contentFormatParam != null
                        ? ContentFormat.fromName(contentFormatParam.toUpperCase())
                        : null;

                // get replace parameter
                String replaceParam = req.getParameter(REPLACE_PARAM);
                boolean replace = true;
                if (replaceParam != null)
                    replace = Boolean.valueOf(replaceParam);

                // create & process request
                LwM2mNode node = extractLwM2mNode(lwm2mPath, req);
                WriteRequest request = new WriteRequest(replace ? Mode.REPLACE : Mode.UPDATE, contentFormat, lwm2mPath,
                        node);
                sendRequestAndWriteResponse(registration, request, req, resp);
            } catch (RuntimeException e) {
                handleException(e, resp);
            }
        });
    }

    private void sendWriteAttributeRequest(HttpServletRequest req, HttpServletResponse resp, IRegistration registration,
            String lwm2mPath) {
        executeSafely(req, resp, () -> {
            try {
                // create & process request WriteAttributes request
                LwM2mAttributeSet attributes = new LwM2mAttributeSet(
                        attributeParser.parseUriQuery(req.getQueryString()));
                WriteAttributesRequest request = new WriteAttributesRequest(lwm2mPath, attributes);
                sendRequestAndWriteResponse(registration, request, req, resp);
            } catch (RuntimeException e) {
                handleException(e, resp);
            } catch (InvalidAttributeException e) {
                LOG.warn("Invalid attributes", e);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().append("Invalid attributes: ").append(e.getMessage()).flush();
            }
        });
    }

    private void sendWriteCompositeRequest(HttpServletRequest req, HttpServletResponse resp,
            IRegistration registration) {
        executeSafely(req, resp, () -> {
            try {
                // get content format
                String nodeContentFormatParam = req.getParameter(NODE_FORMAT_PARAM);
                ContentFormat nodeContentFormat = nodeContentFormatParam != null
                        ? ContentFormat.fromName(nodeContentFormatParam.toUpperCase())
                        : null;

                // get node values
                String content = IOUtils.toString(req.getInputStream(), req.getCharacterEncoding());
                Map<LwM2mPath, LwM2mNode> values = mapper.readValue(content,
                        new TypeReference<HashMap<LwM2mPath, LwM2mNode>>() {
                        });
                // create & process request
                sendRequestAndWriteResponse(registration, new WriteCompositeRequest(nodeContentFormat, values, null),
                        req, resp);
            } catch (RuntimeException e) {
                handleException(e, resp);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // get registration
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        if (path.length < 1) {
            safelySendError(req, resp, HttpServletResponse.SC_BAD_REQUEST, ERROR_MESSAGE_INVALID_PATH);
            return;
        }
        String clientEndpoint = path[0];
        IRegistration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
        if (registration == null) {
            safelySendError(req, resp, HttpServletResponse.SC_BAD_REQUEST,
                    String.format(ERROR_MESSAGE_NO_REGISTERED, clientEndpoint));
            return;
        }

        // /clients/endPoint/composite/observe : do LightWeight M2M Observe-Composite request on a given client.
        if (path.length == 3 && COMPOSITE_PATH_PART.equals(path[1]) && OBSERVE_PATH_PART.equals(path[2])) {
            sendObserveCompositeRequest(req, resp, registration);
            return;
        }

        // /clients/endPoint/LWRequest/observe : do LightWeight M2M observe request on a given client.
        if (path.length >= 3 && OBSERVE_PATH_PART.equals(path[path.length - 1])) {
            String lwm2mPath = StringUtils.substringBetween(req.getPathInfo(), clientEndpoint, "/observe");
            sendObserveRequest(req, resp, registration, lwm2mPath);
            return;
        }

        String lwm2mPath = Strings.CS.removeStart(req.getPathInfo(), URI_DELIMITER + clientEndpoint);

        // /clients/endPoint/LWRequest : do LightWeight M2M execute request on a given client.
        if (path.length == 4) {
            sendExecuteRequest(req, resp, registration, lwm2mPath);
            return;
        }

        // /clients/endPoint/LWRequest : do LightWeight M2M create request on a given client.
        if (2 <= path.length && path.length <= 3) {
            sendCreateRequest(req, resp, registration, lwm2mPath);
            return;
        }

        // nothing match
        safelySendError(req, resp, HttpServletResponse.SC_BAD_REQUEST, ERROR_MESSAGE_INVALID_PATH);
    }

    private void sendCreateRequest(HttpServletRequest req, HttpServletResponse resp, IRegistration registration,
            String lwm2mPath) {
        executeSafely(req, resp, () -> {
            try {
                // get content format
                String contentFormatParam = req.getParameter(FORMAT_PARAM);
                ContentFormat contentFormat = contentFormatParam != null
                        ? ContentFormat.fromName(contentFormatParam.toUpperCase())
                        : null;

                // create & process request
                LwM2mChildNode node = extractLwM2mNode(lwm2mPath, req);
                if (node instanceof LwM2mObjectInstance) {
                    CreateRequest request;
                    if (node.getId() == LwM2mObjectInstance.UNDEFINED) {
                        request = new CreateRequest(contentFormat, lwm2mPath,
                                ((LwM2mObjectInstance) node).getResources().values());
                    } else {
                        request = new CreateRequest(contentFormat, lwm2mPath, (LwM2mObjectInstance) node);
                    }

                    sendRequestAndWriteResponse(registration, request, req, resp);
                } else {
                    throw new IllegalArgumentException("payload must contain an object instance");
                }
            } catch (RuntimeException e) {
                handleException(e, resp);
            }
        });

    }

    private void sendExecuteRequest(HttpServletRequest req, HttpServletResponse resp, IRegistration registration,
            String lwm2mPath) {
        executeSafely(req, resp, () -> {
            try {
                String params = null;
                if (req.getContentLength() > 0) {
                    params = IOUtils.toString(req.getInputStream(), StandardCharsets.UTF_8);
                }
                ExecuteRequest request = new ExecuteRequest(lwm2mPath, params);
                sendRequestAndWriteResponse(registration, request, req, resp);
            } catch (RuntimeException e) {
                handleException(e, resp);
            }
        });
    }

    private void sendObserveRequest(HttpServletRequest req, HttpServletResponse resp, IRegistration registration,
            String lwm2mPath) {
        executeSafely(req, resp, () -> {
            try {
                // get content format
                String contentFormatParam = req.getParameter(FORMAT_PARAM);
                ContentFormat contentFormat = contentFormatParam != null
                        ? ContentFormat.fromName(contentFormatParam.toUpperCase())
                        : null;

                // create & process request
                ObserveRequest request = new ObserveRequest(contentFormat, lwm2mPath);
                sendRequestAndWriteResponse(registration, request, req, resp);
            } catch (RuntimeException e) {
                handleException(e, resp);
            }
        });
    }

    private void sendObserveCompositeRequest(HttpServletRequest req, HttpServletResponse resp,
            IRegistration registration) {
        executeSafely(req, resp, () -> {
            try {
                // get paths
                String pathParam = req.getParameter(PATH_PARAM);
                String[] paths = pathParam.split(",");

                // get content format
                String pathContentFormatParam = req.getParameter(PATH_FORMAT_PARAM);
                ContentFormat pathContentFormat = pathContentFormatParam != null
                        ? ContentFormat.fromName(pathContentFormatParam.toUpperCase())
                        : null;
                String nodeContentFormatParam = req.getParameter(NODE_FORMAT_PARAM);
                ContentFormat nodeContentFormat = nodeContentFormatParam != null
                        ? ContentFormat.fromName(nodeContentFormatParam.toUpperCase())
                        : null;

                // create & process request
                ObserveCompositeRequest request = new ObserveCompositeRequest(pathContentFormat, nodeContentFormat,
                        paths);
                sendRequestAndWriteResponse(registration, request, req, resp);
            } catch (RuntimeException e) {
                handleException(e, resp);
            }
        });
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // get registration
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        if (path.length < 1) {
            safelySendError(req, resp, HttpServletResponse.SC_BAD_REQUEST, ERROR_MESSAGE_INVALID_PATH);
            return;
        }
        String clientEndpoint = path[0];
        IRegistration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
        if (registration == null) {
            safelySendError(req, resp, HttpServletResponse.SC_BAD_REQUEST,
                    String.format(ERROR_MESSAGE_NO_REGISTERED, clientEndpoint));
            return;
        }

        // /clients/endPoint/composite/observe : do LightWeight M2M observe request on a given client.
        if (path.length == 3 && COMPOSITE_PATH_PART.equals(path[1]) && OBSERVE_PATH_PART.equals(path[2])) {
            cancelCompositeObservation(req, resp, registration);
            return;
        }

        // /clients/endPoint/LWRequest/observe : cancel observation for the given resource.
        if (path.length >= 3 && OBSERVE_PATH_PART.equals(path[path.length - 1])) {
            String lwm2mPath = StringUtils.substringsBetween(req.getPathInfo(), clientEndpoint, "/observe")[0];
            cancelObservation(req, resp, registration, lwm2mPath);
            return;
        }

        // /clients/endPoint/LWRequest/ : delete instance
        String lwm2mPath = Strings.CS.removeStart(req.getPathInfo(), URI_DELIMITER + clientEndpoint);
        sendDeleteInstanceRequest(req, resp, registration, lwm2mPath);
    }

    private void sendDeleteInstanceRequest(HttpServletRequest req, HttpServletResponse resp, IRegistration registration,
            String lwm2mPath) {
        executeSafely(req, resp, () -> {
            try {
                DeleteRequest request = new DeleteRequest(lwm2mPath);
                sendRequestAndWriteResponse(registration, request, req, resp);
            } catch (RuntimeException e) {
                handleException(e, resp);
            }
        });
    }

    private void cancelObservation(HttpServletRequest req, HttpServletResponse resp, IRegistration registration,
            String lwm2mPath) {
        executeSafely(req, resp, () -> {
            try {

                String active = req.getParameter(ACTIVE_CANCEL);
                if (active != null) {
                    Set<Observation> observations = server.getObservationService().getObservations(registration);
                    Optional<Observation> observation = observations.stream()
                            .filter(obs -> obs instanceof SingleObservation
                                    && ((SingleObservation) obs).getPath().equals(new LwM2mPath(lwm2mPath)))
                            .findFirst();
                    if (observation.isPresent()) {
                        CompletableFuture<LwM2mResponse> futureResponse = sendRequestAndWriteResponse(registration,
                                new CancelObservationRequest((SingleObservation) observation.get()), req, resp);

                        futureResponse.thenApply(r -> {
                            if (r.isSuccess()) {
                                server.getObservationService().cancelObservations(registration, lwm2mPath);
                            }
                            return r;
                        });
                    } else {
                        safelySendError(req, resp, HttpServletResponse.SC_NOT_FOUND,
                                String.format("no observation for path %s for  client '%s'", lwm2mPath, registration));
                    }
                } else {
                    server.getObservationService().cancelObservations(registration, lwm2mPath);
                    resp.setStatus(HttpServletResponse.SC_OK);
                }
            } catch (RuntimeException e) {
                handleException(e, resp);
            }
        });
    }

    private void cancelCompositeObservation(HttpServletRequest req, HttpServletResponse resp,
            IRegistration registration) {
        executeSafely(req, resp, () -> {
            try {
                // get paths
                String pathParam = req.getParameter(PATH_PARAM);
                String[] paths = pathParam.split(",");

                String active = req.getParameter(ACTIVE_CANCEL);
                if (active != null) {
                    Set<Observation> observations = server.getObservationService().getObservations(registration);
                    Optional<Observation> observation = observations.stream()
                            .filter(obs -> obs instanceof CompositeObservation && ((CompositeObservation) obs)
                                    .getPaths().equals(LwM2mPath.getLwM2mPathList(Arrays.asList(paths))))
                            .findFirst();
                    if (observation.isPresent()) {
                        CompletableFuture<LwM2mResponse> futureResponse = sendRequestAndWriteResponse(registration,
                                new CancelCompositeObservationRequest((CompositeObservation) observation.get()), req,
                                resp);
                        futureResponse.thenApply(r -> {
                            if (r.isSuccess()) {
                                server.getObservationService().cancelCompositeObservations(registration, paths);
                            }
                            return r;
                        });
                    } else {
                        safelySendError(req, resp, HttpServletResponse.SC_NOT_FOUND,
                                String.format("no composite observation for paths %s for  client '%s'",
                                        Arrays.toString(paths), registration));
                    }
                } else {
                    server.getObservationService().cancelCompositeObservations(registration, paths);
                    resp.setStatus(HttpServletResponse.SC_OK);
                }
            } catch (RuntimeException e) {
                handleException(e, resp);
            }
        });
    }

    private CompletableFuture<LwM2mResponse> sendRequestAndWriteResponse(IRegistration destination,
            DownlinkDeviceManagementRequest<?> lwm2mReq, HttpServletRequest httpReq, HttpServletResponse httpResp)
            throws InterruptedException, IOException {

        // Send Request
        CompletableFuture<LwM2mResponse> future = queueHandler.send(destination, lwm2mReq, extractTimeout(httpReq));

        // if we get response now
        if (future.isDone()) {
            try {
                LwM2mResponse lwm2mResp = future.get();
                processDeviceResponse(httpReq, httpResp, lwm2mResp);
                return future;
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else {
                    throw new IllegalStateException(String
                            .format("Unexpected error when trying to send request %s because of %s", lwm2mReq, cause));
                }
            }
        }

        // else response will be receive later (probably because request was delayed as device is not awake)
        final String requestId = UUID.randomUUID().toString();
        future.thenApply(lwm2mResp -> {
            try {
                // when response will be received, send a event with the response
                ResponseDelayed responseDelayed = new ResponseDelayed();
                responseDelayed.ep = destination.getEndpoint();
                if (lwm2mReq instanceof SimpleDownlinkRequest) {
                    responseDelayed.path = ((SimpleDownlinkRequest<?>) lwm2mReq).getPath().toString();
                }
                responseDelayed.response = lwm2mResp;
                responseDelayed.requestId = requestId;
                responseDelayed.delayed = true;
                eventServlet.sendEvent("REQUEST_RESPONSE", this.mapper.writeValueAsString(responseDelayed),
                        destination.getEndpoint());
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
            return lwm2mResp;
        });

        // answer that request is delayed
        ResponseDelayed responseDelayed = new ResponseDelayed();
        responseDelayed.ep = destination.getEndpoint();
        if (lwm2mReq instanceof SimpleDownlinkRequest) {
            responseDelayed.path = ((SimpleDownlinkRequest<?>) lwm2mReq).getPath().toString();
        }
        responseDelayed.response = null;
        responseDelayed.requestId = requestId;
        responseDelayed.delayed = true;
        httpResp.setContentType(APPLICATION_JSON);
        httpResp.getOutputStream().write(this.mapper.writeValueAsString(responseDelayed).getBytes());
        httpResp.setStatus(HttpServletResponse.SC_OK);

        return future;
    }

    public static class ResponseDelayed {
        public String ep;
        public String path;
        public String requestId;
        public LwM2mResponse response; // may be null if response is delayed
        public boolean delayed;
    }

    private void processDeviceResponse(HttpServletRequest httpReq, HttpServletResponse httpResp,
            LwM2mResponse lwm2mResp) throws IOException {
        if (lwm2mResp == null) {
            LOG.warn("Request {}{} timed out.", httpReq.getServletPath(), httpReq.getPathInfo());
            httpResp.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
            httpResp.getWriter().append("Request timeout").flush();
        } else {
            String response = this.mapper.writeValueAsString(lwm2mResp);
            httpResp.setContentType(APPLICATION_JSON);
            httpResp.getOutputStream().write(response.getBytes());
            httpResp.setStatus(HttpServletResponse.SC_OK);
        }
    }

    private LwM2mChildNode extractLwM2mNode(String target, HttpServletRequest req) throws IOException {
        String contentType = StringUtils.substringBefore(req.getContentType(), ";");
        if (APPLICATION_JSON.equals(contentType)) {
            String content = IOUtils.toString(req.getInputStream(), req.getCharacterEncoding());
            LwM2mNode node;
            try {
                node = mapper.readValue(content, LwM2mNode.class);
            } catch (JsonProcessingException e) {
                throw new InvalidRequestException(e, "unable to parse json to tlv:%s", e.getMessage());
            }
            if (!(node instanceof LwM2mChildNode)) {
                throw new InvalidRequestException(String.format(
                        "Can not handle %s : Only LwM2m Child Node is supported", node.getClass().getSimpleName()));
            }
            return (LwM2mChildNode) node;
        } else if ("text/plain".equals(contentType)) {
            String content = IOUtils.toString(req.getInputStream(), req.getCharacterEncoding());
            int rscId = Integer.parseInt(target.substring(target.lastIndexOf("/") + 1));
            return LwM2mSingleResource.newStringResource(rscId, content);
        }
        throw new InvalidRequestException("content type %s not supported", req.getContentType());
    }

    private long extractTimeout(HttpServletRequest req) {
        // get content format
        String timeoutParam = req.getParameter(TIMEOUT_PARAM);
        long timeout;
        if (timeoutParam != null) {
            try {
                timeout = Long.parseLong(timeoutParam) * 1000;
            } catch (NumberFormatException e) {
                timeout = DEFAULT_TIMEOUT;
            }
        } else {
            timeout = DEFAULT_TIMEOUT;
        }
        return timeout;
    }
}
