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
import org.eclipse.californium.scandium.dtls.DtlsHandshakeTimeoutException;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.request.exception.TimeoutException.Type;

// TODO TL : this is just a class for backward compatibility waiting TL refactoring was done.
// It should be deleted at the end.
public class TemporaryExceptionTranslator extends DefaultExceptionTranslator {

    @Override
    public Exception translate(Request coapRequest, Throwable error) {
        if (error instanceof DtlsHandshakeTimeoutException) {
            return new TimeoutException(Type.DTLS_HANDSHAKE_TIMEOUT, error,
                    "Request %s timeout : dtls handshake timeout", coapRequest.getURI());
        } else {
            return super.translate(coapRequest, error);
        }
    }

}
