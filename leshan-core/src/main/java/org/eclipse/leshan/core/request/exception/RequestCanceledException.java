/*******************************************************************************
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
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
 *    Achim Kraus (Bosch Software Innovations GmbH) - cloned from RequestFailedException
 *******************************************************************************/
package org.eclipse.leshan.core.request.exception;

/**
 * Exception for canceled requests.
 */
public class RequestCanceledException extends RequestFailedException {

    private static final long serialVersionUID = 1L;

    public RequestCanceledException() {
        super();
    }

    public RequestCanceledException(String message) {
        super(message);
    }

}
