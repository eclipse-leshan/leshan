/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use ServerIdentity
 *******************************************************************************/
package org.eclipse.leshan.client.resource;

import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
import org.eclipse.leshan.core.response.BootstrapDiscoverResponse;

/**
 * A new version of {@link LwM2mObjectEnabler} which support bootstrap discover.
 * 
 * @since 1.1
 */
public interface LwM2mObjectEnabler2 extends LwM2mObjectEnabler {

    BootstrapDiscoverResponse discover(ServerIdentity identity, BootstrapDiscoverRequest request);

}
