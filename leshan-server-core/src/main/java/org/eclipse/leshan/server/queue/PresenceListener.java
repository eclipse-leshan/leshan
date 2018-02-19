/*******************************************************************************
 * Copyright (c) 2017 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Bosch Software Innovations GmbH - initial API
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

import org.eclipse.leshan.server.registration.Registration;

/**
 * A listener aware of the status of LWM2M Client using queue mode binding.
 *
 */
public interface PresenceListener {

    /**
     * This method is invoked when the LWM2M client with the given endpoint state changes to awake.
     * 
     * @param registration data of the lwm2m client.
     */
    void onAwake(Registration registration);

    /**
     * This method is invoked when the LWM2M client with the given endpoint state changes to sleeping.
     * 
     * @param registration data of the lwm2m client.
     */
    void onSleeping(Registration registration);
}
