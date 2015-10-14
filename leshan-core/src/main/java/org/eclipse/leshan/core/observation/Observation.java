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
package org.eclipse.leshan.core.observation;

import org.eclipse.leshan.core.node.LwM2mPath;

/**
 * An observation of a resource provided by a LWM2M Client.
 * 
 */
public interface Observation {

    /**
     * Get the registration ID link to this observation.
     * 
     * @return the registration ID
     */
    String getRegistrationId();

    /**
     * Gets the observed resource path.
     * 
     * @return the resource path
     */
    LwM2mPath getPath();

    /**
     * Cancels the observation.
     * 
     * As a result the observer will no longer get notified about changes to the resource.
     */
    void cancel();

    void addListener(ObservationListener listener);

    void removeListener(ObservationListener listener);
}
