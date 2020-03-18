/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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

/**
 * Thrown when an invalid response is received.
 */
public class InvalidResponseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidResponseException(String message) {
        super(message);
    }

    public InvalidResponseException(String m, Object... args) {
        super(String.format(m, args));
    }

    public InvalidResponseException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidResponseException(Throwable e, String m, Object... args) {
        super(String.format(m, args), e);
    }
}
