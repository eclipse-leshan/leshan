/*******************************************************************************
 * Copyright (c) 2024 Semtech and others.
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
 *     Semtech - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.servers;

import org.eclipse.leshan.core.peer.LwM2mIdentity;

/**
 * Since LWM2M v1.1, endpoint name is optional in REGISTER and BOOTSTRAP request. An {@link ServerEndpointNameProvider}
 * is responsible to find the right Endpoint Name from the client identity.
 * <p>
 *
 * @see <a href="https://github.com/eclipse-leshan/leshan/issues/1457"></a>
 * @see DefaultServerEndpointNameProvider
 */
public interface ServerEndpointNameProvider {

    /**
     * @return endpointName if it's possible to guess it from identity, else return <code>null</code>
     */
    String getEndpointName(LwM2mIdentity clientIdentity);

}
