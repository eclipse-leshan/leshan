/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.observation;

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.registration.Registration;

public interface ObservationListener {

    void newObservation(Observation observation, Registration registration);

    void cancelled(Observation observation);

    /**
     * Called on new notification.
     * 
     * @param observation the observation for which new data are received
     * @param registration the registration concerned by this observation
     * @param response the lwm2m response received (successful or error response)
     * 
     */
    void onResponse(Observation observation, Registration registration, ObserveResponse response);

    /**
     * Called when an error occurs on new notification.
     * 
     * @param observation the observation for which new data are received
     * @param registration the registration concerned by this observation
     * @param error the exception raised when we handle the notification. It can be :
     *        <ul>
     *        <li>InvalidResponseException if the response received is malformed.</li>
     *        <li>or any other RuntimeException for unexpected issue.
     *        </ul>
     */
    void onError(Observation observation, Registration registration, Exception error);
}
