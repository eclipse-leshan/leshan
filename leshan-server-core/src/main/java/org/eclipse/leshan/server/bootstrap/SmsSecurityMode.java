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
package org.eclipse.leshan.server.bootstrap;

/**
 * The different SMS security modes
 */
public enum SmsSecurityMode {
    RESERVED(0), SPS_DEVICE(1), SPS_SMARTCARD(2), NO_SEC(3), PROPRIETARY(255);

    public final int code;

    private SmsSecurityMode(int code) {
        this.code = code;
    }
}
