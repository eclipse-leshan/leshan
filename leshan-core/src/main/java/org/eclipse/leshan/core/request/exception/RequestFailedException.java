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

/**
 * Generic exception for all unexpected errors (e.g. rejected request at CoAP level).
 */
public class RequestFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RequestFailedException() {
        super();
    }

    public RequestFailedException(String message) {
        super(message);
    }

}
