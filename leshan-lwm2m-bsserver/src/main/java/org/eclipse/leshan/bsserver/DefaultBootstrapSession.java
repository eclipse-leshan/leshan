/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
package org.eclipse.leshan.bsserver;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.DownlinkBootstrapRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.util.RandomStringUtils;
import org.eclipse.leshan.core.util.Validate;

/**
 * A default implementation for {@link BootstrapSession}
 */
public class DefaultBootstrapSession implements BootstrapSession {

    private final String id;
    private final String endpoint;
    private final LwM2mPeer client;
    private final boolean authorized;
    private final ContentFormat contentFormat;
    private final Map<String, String> applicationData;
    private final long creationTime;
    private final EndpointUri endpointUsed;
    private final BootstrapRequest request;

    private volatile LwM2mModel model;
    private volatile List<DownlinkBootstrapRequest<? extends LwM2mResponse>> requests;
    private volatile List<LwM2mResponse> responses;
    private volatile boolean moreTasks = false;
    private volatile boolean cancelled = false;

    /**
     * Create a {@link DefaultBootstrapSession} using default client preferred content format (see
     * {@link BootstrapRequest#getPreferredContentFormat()} or {@link ContentFormat#TLV} content format if there is no
     * preferences and using <code>System.currentTimeMillis()</code> to set the creation time.
     *
     * @param endpointName The endpoint name of the client.
     * @param request The bootstrap request which initiate the session.
     * @param client The transport layer information about client.
     * @param authorized True if device is authorized to bootstrap.
     * @param applicationData Data that could be attached to a session.
     */
    public DefaultBootstrapSession(String endpointName, BootstrapRequest request, LwM2mPeer client, boolean authorized,
            Map<String, String> applicationData, EndpointUri endpointUsed) {
        this(endpointName, request, client, authorized, null, applicationData, endpointUsed);
    }

    /**
     * Create a {@link DefaultBootstrapSession} using <code>System.currentTimeMillis()</code> to set the creation time.
     *
     * @param endpointName The endpoint name of the client.
     * @param request The bootstrap request which initiate the session.
     * @param client The transport layer information about client.
     * @param authorized True if device is authorized to bootstrap.
     * @param contentFormat The content format to use to write object.
     * @param applicationData Data that could be attached to a session.
     */
    public DefaultBootstrapSession(String endpointName, BootstrapRequest request, LwM2mPeer client, boolean authorized,
            ContentFormat contentFormat, Map<String, String> applicationData, EndpointUri endpointUsed) {
        this(endpointName, request, client, authorized, contentFormat, applicationData, System.currentTimeMillis(),
                endpointUsed);
    }

    /**
     * Create a {@link DefaultBootstrapSession}.
     *
     * @param endpointName The endpoint name of the client.
     * @param request The bootstrap request which initiate the session.
     * @param client The transport layer information about client.
     * @param authorized True if device is authorized to bootstrap.
     * @param contentFormat The content format to use to write object.
     * @param applicationData Data that could be attached to a session.
     * @param creationTime The creation time of this session in ms.
     */
    public DefaultBootstrapSession(String endpointName, BootstrapRequest request, LwM2mPeer client, boolean authorized,
            ContentFormat contentFormat, Map<String, String> applicationData, long creationTime,
            EndpointUri endpointUsed) {
        Validate.notNull(request);
        this.id = RandomStringUtils.random(10, true, true);
        this.request = request;
        this.endpoint = endpointName;
        this.client = client;
        this.endpointUsed = endpointUsed;
        this.authorized = authorized;
        if (contentFormat == null) {
            if (request.getPreferredContentFormat() != null) {
                this.contentFormat = request.getPreferredContentFormat();
            } else {
                this.contentFormat = ContentFormat.TLV;
            }
        } else {
            this.contentFormat = contentFormat;
        }
        if (applicationData == null) {
            this.applicationData = Collections.emptyMap();
        } else {
            this.applicationData = Collections.unmodifiableMap(new HashMap<>(applicationData));
        }
        this.creationTime = creationTime;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getEndpoint() {
        return endpoint;
    }

    @Override
    public LwM2mPeer getClientTransportData() {
        return client;
    }

    @Override
    public EndpointUri getEndpointUsed() {
        return endpointUsed;
    }

    @Override
    public boolean isAuthorized() {
        return authorized;
    }

    @Override
    public ContentFormat getContentFormat() {
        return contentFormat;
    }

    @Override
    public Map<String, String> getApplicationData() {
        return applicationData;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public BootstrapRequest getBootstrapRequest() {
        return request;
    }

    public List<DownlinkBootstrapRequest<? extends LwM2mResponse>> getRequests() {
        return requests;
    }

    public void setRequests(List<DownlinkBootstrapRequest<? extends LwM2mResponse>> requests) {
        this.requests = requests;
    }

    public List<LwM2mResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<LwM2mResponse> responses) {
        this.responses = responses;
    }

    @Override
    public LwM2mModel getModel() {
        return model;
    }

    public void setModel(LwM2mModel model) {
        this.model = model;
    }

    public void setMoreTasks(boolean moreTasks) {
        this.moreTasks = moreTasks;
    }

    public boolean hasMoreTasks() {
        return moreTasks;
    }

    @Override
    public void cancel() {
        cancelled = true;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public String toString() {
        return String.format(
                "DefaultBootstrapSession [id=%s, endpoint=%s, identity=%s, authorized=%s, contentFormat=%s, applicationData=%s, creationTime=%s, request=%s, cancelled=%s]",
                id, endpoint, client, authorized, contentFormat, applicationData, creationTime, request, cancelled);
    }

}
