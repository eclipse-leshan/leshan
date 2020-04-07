/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
 * LWM2M Object/Resource IDs
 */
public interface LwM2mId {

    /* OBJECTS */

    public static final int SECURITY = 0;
    public static final int SERVER = 1;
    public static final int ACCESS_CONTROL = 2;
    public static final int DEVICE = 3;
    public static final int CONNECTIVITY_MONITORING = 4;
    public static final int FIRMWARE = 5;
    public static final int LOCATION = 6;
    public static final int CONNECTIVITY_STATISTICS = 7;
    public static final int SOFTWARE_MANAGEMENT = 9;

    /* SECURITY RESOURCES */

    public static final int SEC_SERVER_URI = 0;
    public static final int SEC_BOOTSTRAP = 1;
    public static final int SEC_SECURITY_MODE = 2;
    public static final int SEC_PUBKEY_IDENTITY = 3;
    public static final int SEC_SERVER_PUBKEY = 4;
    public static final int SEC_SECRET_KEY = 5;
    public static final int SEC_SERVER_ID = 10;

    /* SERVER RESOURCES */

    public static final int SRV_SERVER_ID = 0;
    public static final int SRV_LIFETIME = 1;
    public static final int SRV_BINDING = 7;
}
