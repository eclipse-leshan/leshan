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
 * An exception indicating a problem while accessing a resource on a LWM2M Client (invalid error code, invalid resource
 * value...).
 */
public class ResourceAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String uri;

    /**
     * Initializes all fields.
     * 
     * @param uri the URI of the accessed resource
     * @param message the message returned by the server or <code>null</code> if the server did not return a message
     * @throws NullPointerException if the uri is <code>null</code>
     */
    public ResourceAccessException(String uri, String message) {
        this(uri, message, null);
    }

    /**
     * Initializes all fields.
     * 
     * @param uri the URI of the accessed resource
     * @param message the message returned by the server or <code>null</code> if the server did not return a message
     * @param cause the root cause of the access problem
     * @throws NullPointerException if the uri is <code>null</code>
     */
    public ResourceAccessException(String uri, String message, Throwable cause) {
        super(message, cause);
        if (uri == null) {
            throw new NullPointerException("Request URI must not be null");
        }
        this.uri = uri;
    }

    /**
     * Gets the URI of the accessed resource.
     * 
     * @return the URI
     */
    public String getUri() {
        return this.uri;
    }

}
