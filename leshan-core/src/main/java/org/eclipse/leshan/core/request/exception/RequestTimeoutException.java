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

public class RequestTimeoutException extends ResourceAccessException {

    private static final long serialVersionUID = -6372006578730743741L;

    /**
     * @param uri the resource URI accessed
     * @param timeout the number of milliseconds after which the request has timed out
     */
    public RequestTimeoutException(String uri, long timeout) {
        super(null, uri, String.format("Request timed out after %d milliseconds", timeout));
    }
}
