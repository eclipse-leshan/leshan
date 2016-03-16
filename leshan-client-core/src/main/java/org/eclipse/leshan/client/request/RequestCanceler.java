/*******************************************************************************
 * Copyright (c) 2016 Bosch Software Innovations GmbH and others.
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
 *    Achim Kraus (Bosch Software Innovations GmbH) - initial version
 *******************************************************************************/

package org.eclipse.leshan.client.request;

/**
 * Provide cancel functionality for asynchronously sent requests.
 * {@link LwM2mClientRequestSender#send(java.net.InetSocketAddress, boolean, org.eclipse.leshan.core.request.UplinkRequest, org.eclipse.leshan.core.response.ResponseCallback, org.eclipse.leshan.core.response.ErrorCallback)}
 * .
 */
public interface RequestCanceler {
    /**
     * Cancel pending request.
     */
    void cancel();
}
