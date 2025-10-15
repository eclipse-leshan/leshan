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
package org.eclipse.leshan.transport.javacoap.server.observation;

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.registration.IRegistration;

import com.mbed.coap.transport.TransportContext.Key;

public class LwM2mKeys {

    // Keys for Observe Request
    public static final Key<Observation> LESHAN_OBSERVATION = new Key<>(null);
    public static final Key<IRegistration> LESHAN_REGISTRATION = new Key<>(null);
}
