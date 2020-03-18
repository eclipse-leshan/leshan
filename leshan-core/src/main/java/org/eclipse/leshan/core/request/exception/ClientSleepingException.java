/*******************************************************************************
 * Copyright (c) 2017 RISE SICS AB.
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
 *     RISE SICS AB - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.request.exception;

/**
 * Exception indicating that the message was cancelled on the CoAP layer and any retries will be stopped.
 */
public class ClientSleepingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ClientSleepingException(String message, Object... args) {
        super(String.format(message, args));
    }
}
