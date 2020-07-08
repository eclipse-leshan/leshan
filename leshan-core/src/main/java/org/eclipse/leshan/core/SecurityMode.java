/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.core;

/**
 * The different DTLS security modes
 */
public enum SecurityMode {
    PSK(0), RPK(1), X509(2), NO_SEC(3), EST(4);

    public final int code;

    private SecurityMode(int code) {
        this.code = code;
    }

    public static SecurityMode fromCode(long code) {
        return fromCode((int) code);
    }

    public static SecurityMode fromCode(int code) {
        for (SecurityMode sm : SecurityMode.values()) {
            if (sm.code == code) {
                return sm;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported security code : %d", code));
    }
}
