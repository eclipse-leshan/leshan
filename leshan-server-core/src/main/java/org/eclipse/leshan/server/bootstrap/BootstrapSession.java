/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
package org.eclipse.leshan.server.bootstrap;

import java.net.URI;
import java.util.Map;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.server.security.BootstrapAuthorizer;

/**
 * Represent a single Bootstrapping session.
 *
 * Should be created by {@link BootstrapSessionManager} implementations in
 * {@link BootstrapSessionManager#begin(BootstrapRequest, Identity, URI)}.
 */
public interface BootstrapSession {

    /**
     * @return the identifier for this session
     */
    String getId();

    /**
     * @return the endpoint of the LwM2M client.
     */
    String getEndpoint();

    /**
     * @return the bootstrap request which initiate the session.
     */
    BootstrapRequest getBootstrapRequest();

    /**
     * @return the network identity of the LwM2M client.
     */
    Identity getIdentity();

    URI getEndpointUsed();

    /**
     * @return <code>true</code> if the LwM2M client is authorized to start a bootstrap session.
     */
    boolean isAuthorized();

    /**
     * @return the content format to use on write request during this bootstrap session.
     */
    ContentFormat getContentFormat();

    /**
     * @return some application data that could be attached by {@link BootstrapAuthorizer}.
     */
    Map<String, String> getApplicationData();

    /**
     * @return the create time in milliseconds
     * @see System#currentTimeMillis()
     */
    long getCreationTime();

    /**
     * Cancel the current session
     */
    void cancel();

    /**
     * @return True if this session was cancellled
     */
    boolean isCancelled();

    /**
     * @return objects model supported by the client for this session.
     */
    LwM2mModel getModel();
}
