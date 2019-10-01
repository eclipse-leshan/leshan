/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
package org.eclipse.leshan.server.bootstrap;

import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.util.RandomStringUtils;

/**
 * A default implementation for {@link BootstrapSession}
 */
public class DefaultBootstrapSession implements BootstrapSession {

    private final String id;
    private final String endpoint;
    private final Identity identity;
    private final boolean authorized;
    private final ContentFormat contentFormat;
    private final long creationTime;

    private volatile boolean cancelled = false;

    /**
     * Create a {@link DefaultBootstrapSession} using default {@link ContentFormat#TLV} content format and using
     * <code>System.currentTimeMillis()</code> to set the creation time.
     * 
     * @param endpoint The endpoint of the device.
     * @param identity The transport layer identity of the device.
     * @param authorized True if device is authorized to bootstrap.
     */
    public DefaultBootstrapSession(String endpoint, Identity identity, boolean authorized) {
        this(endpoint, identity, authorized, ContentFormat.TLV);
    }

    /**
     * Create a {@link DefaultBootstrapSession} using <code>System.currentTimeMillis()</code> to set the creation time.
     * 
     * @param endpoint The endpoint of the device.
     * @param identity The transport layer identity of the device.
     * @param authorized True if device is authorized to bootstrap.
     * @param contentFormat The content format to use to write object.
     */
    public DefaultBootstrapSession(String endpoint, Identity identity, boolean authorized,
            ContentFormat contentFormat) {
        this(endpoint, identity, authorized, contentFormat, System.currentTimeMillis());
    }

    /**
     * Create a {@link DefaultBootstrapSession}.
     * 
     * @param endpoint The endpoint of the device.
     * @param identity The transport layer identity of the device.
     * @param authorized True if device is authorized to bootstrap.
     * @param contentFormat The content format to use to write object.
     * @param creationTime The creation time of this session in ms.
     */
    public DefaultBootstrapSession(String endpoint, Identity identity, boolean authorized, ContentFormat contentFormat,
            long creationTime) {
        this.id = RandomStringUtils.random(10, true, true);
        this.endpoint = endpoint;
        this.identity = identity;
        this.authorized = authorized;
        this.contentFormat = contentFormat;
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
    public Identity getIdentity() {
        return identity;
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
    public long getCreationTime() {
        return creationTime;
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
                "BootstrapSession [id=%s, endpoint=%s, identity=%s, authorized=%s, contentFormat=%s, creationTime=%dms, cancelled=%s]",
                id, endpoint, identity, authorized, contentFormat, creationTime, cancelled);
    }
}