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
package org.eclipse.leshan.server.impl;

import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;

public class DefaultBootstrapSession implements BootstrapSession {

    private final String endpoint;
    private final Identity identity;
    private final boolean authorized;
    private final ContentFormat contentFormat;

    public DefaultBootstrapSession(String endpoint, Identity identity, boolean authorized) {
        this(endpoint, identity, authorized, ContentFormat.TLV);
    }

    public DefaultBootstrapSession(String endpoint, Identity identity, boolean authorized,
            ContentFormat contentFormat) {
        this.endpoint = endpoint;
        this.identity = identity;
        this.authorized = authorized;
        this.contentFormat = contentFormat;
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
    public String toString() {
        return String.format("BootstrapSession [endpoint=%s, identity=%s, authorized=%s, contentFormat=%s]", endpoint,
                identity, authorized, contentFormat);
    }

}
