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
package org.eclipse.leshan.transport.javacoap.client.observe;

import java.util.List;

import org.eclipse.leshan.core.node.LwM2mPath;

import com.mbed.coap.transport.TransportContext.Key;

public class LwM2mKeys {

    // Keys for Observe Request
    public static final Key<List<LwM2mPath>> LESHAN_OBSERVED_PATHS = new Key<>(null);
    public static final Key<Boolean> LESHAN_NOTIFICATION = new Key<>(null);
}
