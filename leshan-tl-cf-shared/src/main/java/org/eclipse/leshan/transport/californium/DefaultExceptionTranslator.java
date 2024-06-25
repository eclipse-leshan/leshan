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
package org.eclipse.leshan.core.californium;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.exception.EndpointUnconnectedException;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.request.exception.UnconnectedPeerException;

public class DefaultExceptionTranslator implements ExceptionTranslator {

    @Override
    public Exception translate(Request coapRequest, Throwable error) {
        if (error instanceof EndpointUnconnectedException) {
            return new UnconnectedPeerException(error, "Unable to send request %s : peer is not connected",
                    coapRequest.getURI());
        } else {
            return new SendFailedException(error, "Unable to send request %s", coapRequest.getURI());
        }
    }
}
