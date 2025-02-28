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
package org.eclipse.leshan.client.endpoint;

import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.endpoint.LwM2mEndpoint;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;

public interface LwM2mClientEndpoint extends LwM2mEndpoint {

    void forceReconnection(LwM2mServer server, boolean resume);

    long getMaxCommunicationPeriodFor(long lifetimeInMs);

    <T extends LwM2mResponse> T send(LwM2mServer server, UplinkRequest<T> request, long timeoutInMs)
            throws InterruptedException;

    <T extends LwM2mResponse> void send(LwM2mServer server, UplinkRequest<T> request,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback, long timeoutInMs);

}
