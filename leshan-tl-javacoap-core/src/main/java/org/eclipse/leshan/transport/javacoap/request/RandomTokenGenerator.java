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
package org.eclipse.leshan.transport.javacoap.request;

import java.security.SecureRandom;

import com.mbed.coap.packet.Opaque;

public class RandomTokenGenerator {

    private final int tokenSize;
    private final SecureRandom random;

    public RandomTokenGenerator(int tokenSize) {
        // TODO check size is between 1 and 8;
        random = new SecureRandom();
        this.tokenSize = tokenSize;
    }

    public Opaque createToken() {
        byte[] token = new byte[tokenSize];
        random.nextBytes(token);
        return Opaque.of(token);
    }
}
