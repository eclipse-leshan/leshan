/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.californium.client.endpoint;

import org.eclipse.californium.core.network.Exchange;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.peer.IpPeer;

public interface ServerIdentityExtractor {
    LwM2mServer extractIdentity(Exchange exchange, IpPeer foreignPeer);
}
