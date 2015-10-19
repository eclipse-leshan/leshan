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
package org.eclipse.leshan.core.response;

import org.eclipse.leshan.ResponseCode;

/**
 * A response to Lightweight M2M request.
 */
public interface LwM2mResponse {

    /**
     * Gets the response code.
     *
     * @return the code
     */
    ResponseCode getCode();

    /**
     * Gets the error Message. The message is similar to the Reason-Phrase on an HTTP status line. It is not intended
     * for end users but for software engineers that during debugging need to interpret it.
     *
     * @return the error message
     */
    String getErrorMessage();

}
