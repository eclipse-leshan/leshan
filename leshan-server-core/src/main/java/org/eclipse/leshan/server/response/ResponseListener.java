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
 *     Bosch Software Innovations GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.response;

import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.client.Registration;

/**
 * The listener is responsible for handling all the success and error responses for a given requestTicket.
 */
public interface ResponseListener {

    /**
     * this method is invoked when a response is received from LWM2M Client correlated by the request ticket.
     *
     * @param registration the LWM2M registration of the client which sent the response
     * @param requestTicket globally unique identifier used to correlate the response to the orginial request
     * @param response from LWM2M client
     */
    void onResponse(Registration registration, String requestTicket, LwM2mResponse response);

    /**
     * this method is invoked when a an error response is received from LWM2M Client correlated by the request ticket.
     *
     * @param registration the LWM2M registration of the client to which we try to send a request
     * @param requestTicket globally unique identifier used to correlate the response to the orginial request
     * @param exception error from LWM2M client
     */
    void onError(Registration registration, String requestTicket, Exception exception);
}
