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
     * This method is invoked when the LWM2M client with the given endpoint state changes from online to offline. This
     * listener method will be invoked only once when the state change occurs;i.e:- it will not be invoked when the
     * previous state of the endpoint is offline and further events from the client indicates that the client is still
     * offline.
     * 
     * @param registration data of the lwm2m client.
     */
    void onOffline(Registration registration);

    /**
     * This method is invoked when the LWM2M client with the given endpoint state changes from offline to online again.
     * This listener method will be invoked only once when the state change occurs;i.e:- it will not be invoked when the
     * previous state of the endpoint is online and further events from the client indicates that the client is still
     * online.
     * 
     * @param registration data of the lwm2m client.
     */
    void onOnline(Registration registration);
}
