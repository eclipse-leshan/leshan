/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.core.request.exception;

public class TimeoutException extends Exception {

    public enum Type {
        RESPONSE_TIMEOUT, COAP_TIMEOUT, DTLS_HANDSHAKE_TIMEOUT
    }

    private static final long serialVersionUID = -8966041387554358975L;

    private Type type;

    public TimeoutException(Type type, String message, Object... args) {
        super(String.format(message, args));
        this.type = type;
    }

    public TimeoutException(Type type, Throwable cause, String message, Object... args) {
        super(String.format(message, args), cause);
        this.type = type;
    }

    /**
     * Get the kind of timeout.
     * <p>
     * See https://github.com/eclipse/leshan/wiki/Request-Timeout for more details.
     * <p>
     * Current implementation is not able to make differences between CoAP and Blockwise timeout, all is regroup over
     * <code>COAP_TIMEOUT</code>.
     * 
     * @return the kind of timeout
     */
    public Type getType() {
        return type;
    }
}
