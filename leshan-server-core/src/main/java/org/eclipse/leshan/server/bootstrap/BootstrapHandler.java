/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
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

import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.BootstrapResponse;
import org.eclipse.leshan.core.response.SendableResponse;

/**
 * Handle the bootstrap logic at Server side. Check if the client is allowed to bootstrap, with the wanted security
 * scheme. Then send delete and write request to bootstrap the client, then close the bootstrap session by sending a
 * bootstrap finished request.
 */
public interface BootstrapHandler {

    SendableResponse<BootstrapResponse> bootstrap(Identity sender, BootstrapRequest request);
}
