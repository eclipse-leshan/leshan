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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use ServerIdentity.SYSTEM
 *     Rikard Höglund (RISE SICS) - Additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.client.servers;

import java.util.Map;

import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;

/**
 * Extract from LwM2m object tree all the servers information like server uri, security mode, ...
 */
public interface ServersInfoExtractor {

    ServersInfo getInfo(Map<Integer, LwM2mObjectEnabler> objectEnablers);

    ServersInfo getInfo(Map<Integer, LwM2mObjectEnabler> objectEnablers, boolean raiseException)
            throws IllegalStateException;

    DmServerInfo getDMServerInfo(Map<Integer, LwM2mObjectEnabler> objectEnablers, Long shortID);

    ServerInfo getBootstrapServerInfo(Map<Integer, LwM2mObjectEnabler> objectEnablers);
}
