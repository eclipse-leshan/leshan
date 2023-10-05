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
package org.eclipse.leshan.client.endpoint;

import java.util.Collection;

/**
 * A {@link LwM2mClientEndpointsProvider} composed of several internal {@link LwM2mClientEndpointsProvider}.
 * <p>
 * Implementation should allow to use several {@link LwM2mClientEndpointsProvider}.
 *
 * @see DefaultCompositeClientEndpointsProvider
 */
public interface CompositeClientEndpointsProvider extends LwM2mClientEndpointsProvider {
    Collection<LwM2mClientEndpointsProvider> getProviders();
}
