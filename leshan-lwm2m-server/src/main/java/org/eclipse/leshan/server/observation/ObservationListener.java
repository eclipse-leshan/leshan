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
package org.eclipse.leshan.server.observation;

import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.registration.IRegistration;

/**
 * Monitor observation lifetime.
 * <p>
 * Those methods are called by the protocol stage thread pool, this means that execution MUST be done in a short delay,
 * if you need to do long time processing use a dedicated thread pool.
 */
public interface ObservationListener {

    /**
     * Called when a new observation is created.
     *
     * @param observation the new observation.
     * @param registration the related registration
     */
    void newObservation(Observation observation, IRegistration registration);

    /**
     * Called when an observation is cancelled.
     *
     * @param observation the cancelled observation.
     */
    void cancelled(Observation observation);

    /**
     * Called on new notification.
     *
     * @param observation the observation for which new data are received
     * @param registration the registration concerned by this observation
     * @param response the lwm2m response received (successful or error response)
     *
     */
    void onResponse(SingleObservation observation, IRegistration registration, ObserveResponse response);

    /**
     * Called on new notification.
     *
     * @param observation the composite-observation for which new data are received
     * @param registration the registration concerned by this observation
     * @param response the lwm2m observe-composite response received (successful or error response)
     *
     */
    void onResponse(CompositeObservation observation, IRegistration registration, ObserveCompositeResponse response);

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
    void onError(Observation observation, IRegistration registration, Exception error);
}
