/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.core.node;

/**
 * Raise if you try to create an Invalid {@link LwM2mPath}
 */
public class InvalidLwM2mPathException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidLwM2mPathException(String message) {
        super(message);
    }

    public InvalidLwM2mPathException(String message, Object... args) {
        super(String.format(message, args));
    }

    public InvalidLwM2mPathException(Exception e, String message, Object... args) {
        super(String.format(message, args), e);
    }

    public InvalidLwM2mPathException(String message, Exception cause) {
        super(message, cause);
    }
}
