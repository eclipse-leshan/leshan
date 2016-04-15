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
package org.eclipse.leshan;

/**
 * Response codes defined for LWM2M enabler
 */
public enum ResponseCode {
    /** Resource correctly created */
    CREATED,
    /** Resource correctly deleted */
    DELETED,
    /** Resource correctly changed */
    CHANGED,
    /** Content correctly delivered */
    CONTENT,
    /** Access Right Permission Denied */
    UNAUTHORIZED,
    /** Bad request format (missing parameters, bad encoding ...) */
    BAD_REQUEST,
    /** This method (GET/PUT/POST/DELETE) is not allowed on this resource */
    METHOD_NOT_ALLOWED,
    /** The End-point Client Name results in a duplicate entry on the LWM2M Server */
    FORBIDDEN,
    /** Resource not found */
    NOT_FOUND,
    /** None of the preferred Content-Formats can be returned */
    NOT_ACCEPTABLE,
    /** The specified format is not supported */
    UNSUPPORTED_CONTENT_FORMAT,
    /** generic response code for unexpected error */
    INTERNAL_SERVER_ERROR;

    public boolean isError() {
        switch (this) {
        case UNAUTHORIZED:
        case BAD_REQUEST:
        case METHOD_NOT_ALLOWED:
        case FORBIDDEN:
        case NOT_FOUND:
        case INTERNAL_SERVER_ERROR:
        case UNSUPPORTED_CONTENT_FORMAT:
        case NOT_ACCEPTABLE:
            return true;
        default:
            return false;
        }
    }
}
