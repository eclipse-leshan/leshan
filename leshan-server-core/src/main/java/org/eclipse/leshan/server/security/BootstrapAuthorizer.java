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
 *     Orange - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.security;

import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.Identity;

public interface BootstrapAuthorizer {

    /**
     * ...
     *
     * @param request the request received
     * @param clientIdentity the {@link Identity} of the client that sent the request.
     * @return <code>true</code> if request is authorized or <code>false</code> if it is not authorized.
     */
    boolean isAuthorized(BootstrapRequest request, Identity clientIdentity);
}
