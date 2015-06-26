/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.request.exception;


public class RequestTimeoutException extends RuntimeException {

    private static final long serialVersionUID = -6372006578730743741L;

    // TODO this should take a LwM2mRequest object as parameters instead of String

    /**
     * @param request a string representing the request which timed out
     */
    public RequestTimeoutException(String request) {
        super(String.format("Request %s timed out after all CoaP retransmission attempt", request));
    }

    /**
     * @param request request a string representing the request which timed out
     * @param timeout the number of milliseconds after which the request has timed out
     */
    public RequestTimeoutException(String request, long timeout) {
        super(String.format("Request %s timed out after %d milliseconds", request, timeout));

    }
}
