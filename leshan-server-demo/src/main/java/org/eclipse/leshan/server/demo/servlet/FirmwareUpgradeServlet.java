/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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

import static org.eclipse.leshan.server.demo.servlet.FirmwareUpgradeServlet.UpdateState.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.demo.utils.EventSource;
import org.eclipse.leshan.server.demo.utils.EventSourceServlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class FirmwareUpgradeServlet extends EventSourceServlet {

    /**
     * Update state values (Resource 5.0.3)
     */
    public enum UpdateState {

        IDLE(0L), //
        DOWNLOADING(1L), //
        DOWNLOADED(2L), //
        UPDATING(3L); //

        final long value;

        private UpdateState(Long val) {
            this.value = val;
        }

        static UpdateState fromValue(Object value) {
            for (UpdateState u : values()) {
                if (value.equals(u.value)) {
                    return u;
                }
            }
            return null;
        }
    }

    private static final long serialVersionUID = 1L;

    private static final String EVENT_FIRMWARE_UPDATE_LOG = "FW_UPDATE_LOG";
    private static final String QUERY_PARAM_ENDPOINT = "ep";
    private final Gson gson;

    private LwM2mServer server;
    private Set<FirmwareUpdateEventSource> eventSources = new ConcurrentHashSet<>();

    public FirmwareUpgradeServlet(LwM2mServer server) {
        this.server = server;

        GsonBuilder gsonBuilder = new GsonBuilder();
        this.gson = gsonBuilder.create();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Path validation
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        if (path.length != 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path.");
            return;
        }

        // Check the client exists
        String clientEndpoint = path[0];
        Client client = server.getClientRegistry().get(clientEndpoint);
        if (client == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().format("no registered client with id: '%s'.", clientEndpoint).flush();
            return;
        }

        // Get package URI
        URI packageURI = null;
        Map<String, String> parameters = new HashMap<String, String>();
        String contentType = HttpFields.valueParameters(req.getContentType(), parameters);
        if ("text/plain".equals(contentType)) {
            String content = IOUtils.toString(req.getInputStream(), parameters.get("charset"));
            try {
                packageURI = new URI(content);
            } catch (URISyntaxException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().format("Bad package URI: '%s'.", e.getMessage()).flush();
                return;
            }
        }

        // launch the Firmware Update task
        Thread updateTask = new Thread(new FirmwareUpdateTask(client, packageURI));
        updateTask.start();
        return;
    }

    public class FirmwareUpdateTask implements Runnable {
        URI packageURI;
        Client client;

        public FirmwareUpdateTask(Client client, URI packageURI) {
            this.packageURI = packageURI;
            this.client = client;
        }

        public void run() {
            // get client end-point
            String endpoint = client.getEndpoint();

            // Start Firmware update
            try {
                // write package URI
                sendLog(endpoint, "DEBUG", String.format("Writing package URI '%s'...", packageURI.toString()));
                WriteResponse writePackageResp = server.send(client,
                        new WriteRequest(Mode.REPLACE, 5, 0, 1, packageURI.toString()));
                if (writePackageResp.isSuccess()) {
                    sendLog(endpoint, "INFO", String.format("Package URI written"));
                } else {
                    sendLog(endpoint, "ERROR", String.format("Unable to write package URI: %s %s", writePackageResp
                            .getCode().toString(), writePackageResp.getErrorMessage()));
                    return;
                }

                // Poll Firmware update State, wait for DOWNLOADED state
                sendLog(endpoint, "DEBUG", String.format("Watching download state..."));
                boolean downloaded = false;
                do {
                    Thread.sleep(2000); // sleep 2s.
                    ReadResponse readStateResp = server.send(client, new ReadRequest(5, 0, 3));
                    if (readStateResp.isSuccess()) {
                        Object state = ((LwM2mSingleResource) readStateResp.getContent()).getValue();
                        sendLog(endpoint, "DEBUG", String.format("Current state: %s...", UpdateState.fromValue(state)));
                        downloaded = state.equals(DOWNLOADED.value);
                    } else {
                        sendLog(endpoint, "ERROR", String.format("Unable to read state: %s %s", readStateResp.getCode()
                                .toString(), readStateResp.getErrorMessage()));
                        return;
                    }
                } while (!downloaded);
                sendLog(endpoint, "INFO", String.format("Package downloaded"));

                // Ask for update
                sendLog(endpoint, "DEBUG", String.format("Starting update..."));
                ExecuteResponse execUpdateResp = server.send(client, new ExecuteRequest(5, 0, 2));
                if (execUpdateResp.isSuccess()) {
                    sendLog(endpoint, "INFO", String.format("Update started"));
                } else {
                    sendLog(endpoint, "ERROR", String.format("Unable to start update: %s %s", execUpdateResp.getCode()
                            .toString(), execUpdateResp.getErrorMessage()));
                    return;
                }

                // Poll Firmware update State, wait for IDLE (success) or DOWNLOADED(failure) state
                sendLog(endpoint, "DEBUG", String.format("Watching update state..."));
                boolean idle = false;
                do {
                    Thread.sleep(2000); // sleep 2s.
                    ReadResponse readStateResp = server.send(client, new ReadRequest(5, 0, 3));
                    if (readStateResp.isSuccess()) {
                        Object state = ((LwM2mSingleResource) readStateResp.getContent()).getValue();
                        if (state.equals(DOWNLOADED.value)) {
                            sendLog(endpoint, "ERROR", String.format("Update failed: %s %s", readStateResp.getCode()
                                    .toString(), readStateResp.getErrorMessage()));
                            return;
                        } else {
                            sendLog(endpoint, "DEBUG",
                                    String.format("Current state: %s...", UpdateState.fromValue(state)));
                            idle = state.equals(IDLE.value);
                        }
                    } else {
                        sendLog(endpoint, "ERROR", String.format("Unable to read state: %s %s", readStateResp.getCode()
                                .toString(), readStateResp.getErrorMessage()));
                        return;
                    }
                } while (!idle);

                // read version
                ReadResponse readVersionResp = server.send(client, new ReadRequest(3, 0, 3));
                if (readVersionResp.isSuccess()) {
                    Object version = ((LwM2mSingleResource) readVersionResp.getContent()).getValue();
                    sendLog(endpoint, "INFO", String.format("Final firmware version: %s", version));
                } else {
                    sendLog(endpoint, "WARNING", String.format("Unable to read version: %s %s.", readVersionResp
                            .getCode().toString(), readVersionResp.getErrorMessage()));
                }

                sendLog(endpoint, "SUCCESS", String.format("Firmware updated!"));

            } catch (InterruptedException e) {
                sendLog(endpoint, "ERROR", String.format("Thread was interrupted... '%s'", e.getMessage()));
                return;
            }
        }
    }

    private synchronized void sendLog(String endpoint, String status, String message) {
        for (FirmwareUpdateEventSource eventSource : eventSources) {
            if (eventSource.getEndpoint() == null || eventSource.getEndpoint().equals(endpoint)) {
                eventSource.sentEvent(EVENT_FIRMWARE_UPDATE_LOG,
                        gson.toJson(new FirmwareUpdateLog(status, message, System.currentTimeMillis())));
            }
        }
    }

    @Override
    protected EventSource newEventSource(HttpServletRequest req) {
        String endpoint = req.getParameter(QUERY_PARAM_ENDPOINT);
        return new FirmwareUpdateEventSource(endpoint);
    }

    private class FirmwareUpdateEventSource implements EventSource {

        private String endpoint;
        private Emitter emitter;

        public FirmwareUpdateEventSource(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public void onOpen(Emitter emitter) throws IOException {
            this.emitter = emitter;
            eventSources.add(this);
        }

        @Override
        public void onClose() {
            eventSources.remove(this);
        }

        public void sentEvent(String event, String data) {
            try {
                emitter.event(event, data);
            } catch (IOException e) {
                onClose();
            }
        }

        public String getEndpoint() {
            return endpoint;
        }
    }

    public class FirmwareUpdateLog {

        public String status;
        public String message;
        public long date;

        public FirmwareUpdateLog(String status, String message, long date) {
            this.status = status;
            this.message = message;
            this.date = date;
        }
    }
}
