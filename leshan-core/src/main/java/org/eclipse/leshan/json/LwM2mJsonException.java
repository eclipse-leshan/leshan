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
 *     Gemalto M2M GmbH
 *******************************************************************************/
package org.eclipse.leshan.json;

/**
 * Exception thrown in case of JSON parsing error
 */
public class LwM2mJsonException extends Exception {

    private static final long serialVersionUID = 1L;

    public LwM2mJsonException(String message) {
        super(message);
    }

    public LwM2mJsonException(String message, Exception cause) {
        super(message, cause);
    }

}
