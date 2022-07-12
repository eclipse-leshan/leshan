/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.core.link.attributes;

/**
 * Exception to throw if Attribute is Invalid.
 */

public class InvalidAttributeException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidAttributeException(String message) {
        super(message);
    }

    public InvalidAttributeException(String message, Object... args) {
        super(String.format(message, args));
    }

    public InvalidAttributeException(Exception e, String message, Object... args) {
        super(String.format(message, args), e);
    }

    public InvalidAttributeException(String message, Exception e) {
        super(message, e);
    }
}
