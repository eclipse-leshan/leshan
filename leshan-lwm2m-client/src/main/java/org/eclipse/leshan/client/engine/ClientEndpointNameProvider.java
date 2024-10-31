/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
package org.eclipse.leshan.client.engine;

import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.core.request.UplinkRequest;

/**
 * Since LWM2M v1.1, endpoint name is optional in REGISTER and BOOTSTRAP request. An {@link ClientEndpointNameProvider}
 * is determine the endpoint name value which should be used in Register/BootstrapRequest.
 *
 * @see <a href="https://github.com/eclipse-leshan/leshan/issues/1457"></a>
 * @see DefaultClientEndpointNameProvider
 */
public interface ClientEndpointNameProvider {

    /**
     * @return the default endpoint name
     */
    String getEndpointName();

    /**
     * @return endpointName or null if it could(wanted) to be ignore for given kind of request.
     */
    String getEndpointNameFor(ServerInfo clientIdentity, Class<? extends UplinkRequest<?>> requestType);
}
