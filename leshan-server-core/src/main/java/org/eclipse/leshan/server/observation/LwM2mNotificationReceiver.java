/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
import org.eclipse.leshan.server.profile.ClientProfile;
import org.eclipse.leshan.server.registration.Registration;

public interface LwM2mNotificationReceiver {

    /**
     * Called when a new observation is created.
     *
     * @param observation the new observation.
     * @param registration the related registration
     */
    void newObservation(Observation observation, Registration registration);

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
     * @param profile the client profile concerned by this observation
     * @param response the lwm2m response received (successful or error response)
     *
     */
    void onNotification(SingleObservation observation, ClientProfile profile, ObserveResponse response);

    /**
     * Called on new notification.
     *
     * @param observation the composite-observation for which new data are received
     * @param profile the client profile concerned by this observation
     * @param response the lwm2m observe-composite response received (successful or error response)
     *
     */
    void onNotification(CompositeObservation observation, ClientProfile profile, ObserveCompositeResponse response);

    /**
     * Called when an error occurs on new notification.
     *
     * @param observation the observation for which new data are received
     * @param profile the client profile concerned by this observation
     * @param error the exception raised when we handle the notification. It can be :
     *        <ul>
     *        <li>InvalidResponseException if the response received is malformed.</li>
     *        <li>or any other RuntimeException for unexpected issue.
     *        </ul>
     */
    void onError(Observation observation, ClientProfile profile, Exception error);
}
