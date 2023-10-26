/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.javacoap.server.coaptcp.transport;

import java.io.IOException;

public class UnconnectedPeerException extends IOException {

    private static final long serialVersionUID = 1L;

    public UnconnectedPeerException(String message) {
        super(message);
    }

    public UnconnectedPeerException(Throwable cause) {
        super(cause);
    }

    public UnconnectedPeerException(String message, Throwable cause) {
        super(message, cause);
    }
}
