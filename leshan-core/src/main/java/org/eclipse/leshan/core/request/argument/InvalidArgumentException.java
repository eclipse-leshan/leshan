/*******************************************************************************
 * Copyright (c) 2021 Orange.
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
 *     Orange - Add better support for Arguments of Execute Operation.
 *******************************************************************************/
package org.eclipse.leshan.core.request.argument;

/**
 * Exception thrown in case of invalid argument format while creating one.
 */
public class InvalidArgumentException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidArgumentException(String message) {
        super(message);
    }

    public InvalidArgumentException(String message, Object... args) {
        super(String.format(message, args));
    }

    public InvalidArgumentException(Exception exception, String message, Object... args) {
        super(String.format(message, args), exception);
    }
}
