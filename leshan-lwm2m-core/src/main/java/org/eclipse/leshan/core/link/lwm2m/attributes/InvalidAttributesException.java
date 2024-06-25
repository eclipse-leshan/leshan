/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
package org.eclipse.leshan.core.link.lwm2m.attributes;

/**
 * Exception raised when a collection of Attribute are not valid.
 */
public class InvalidAttributesException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidAttributesException(String message) {
        super(message);
    }

    public InvalidAttributesException(String message, Object... args) {
        super(String.format(message, args));
    }

    public InvalidAttributesException(Exception e, String message, Object... args) {
        super(String.format(message, args), e);
    }

    public InvalidAttributesException(String message, Exception e) {
        super(message, e);
    }
}
