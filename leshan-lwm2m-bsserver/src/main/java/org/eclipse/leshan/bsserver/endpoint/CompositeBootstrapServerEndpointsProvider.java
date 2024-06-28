/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.server.bootstrap.endpoint;

import java.util.Collection;

import org.eclipse.leshan.server.endpoint.LwM2mServerEndpointsProvider;

/**
 * A {@link LwM2mBootstrapServerEndpointsProvider} composed of several internal
 * {@link LwM2mBootstrapServerEndpointsProvider}.
 * <p>
 * Implementation should allow to use several {@link LwM2mBootstrapServerEndpointsProvider}.
 *
 * @see DefaultCompositeBootstrapServerEndpointsProvider
 */
public interface CompositeBootstrapServerEndpointsProvider extends LwM2mBootstrapServerEndpointsProvider {

    /**
     * @return all internal {@link LwM2mServerEndpointsProvider}.
     */
    Collection<LwM2mBootstrapServerEndpointsProvider> getProviders();
}
