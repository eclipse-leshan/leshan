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
package org.eclipse.leshan.core.endpoint;

public class InvalidEndpointUriException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidEndpointUriException() {
    }

    public InvalidEndpointUriException(String m) {
        super(m);
    }

    public InvalidEndpointUriException(String m, Object... args) {
        super(String.format(m, args));
    }

    public InvalidEndpointUriException(Throwable e) {
        super(e);
    }

    public InvalidEndpointUriException(String m, Throwable e) {
        super(m, e);
    }

    public InvalidEndpointUriException(Throwable e, String m, Object... args) {
        super(String.format(m, args), e);
    }
}
