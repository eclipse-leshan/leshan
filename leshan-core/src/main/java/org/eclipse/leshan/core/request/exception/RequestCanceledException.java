/*******************************************************************************
 * Copyright (c) 2016 Bosch Software Innovations GmbH and others.
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
 *     Bosch Software Innovations GmbH
 *               - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.request.exception;

/**
 * Exception indicating that the message was cancelled on the CoAP layer and any retries will be stopped.
 */
public class RequestCanceledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RequestCanceledException() {
    }

    public RequestCanceledException(String message, Object... args) {
        super(String.format(message, args));
    }
}
