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
package org.eclipse.leshan.client.bootstrap;

/**
 * Raised if LWM2M client is in an invalid or inconsistent state.
 *
 */
public class InvalidStateException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidStateException() {
    }

    public InvalidStateException(String m) {
        super(m);
    }

    public InvalidStateException(String m, Object... args) {
        super(String.format(m, args));
    }

    public InvalidStateException(Throwable e) {
        super(e);
    }

    public InvalidStateException(String m, Throwable e) {
        super(m, e);
    }

    public InvalidStateException(Throwable e, String m, Object... args) {
        super(String.format(m, args), e);
    }
}
