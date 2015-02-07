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

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.server.client.Client;

/**
 * An observation of a resource provided by a LWM2M Client.
 * 
 * Instances are managed by an {@link ObservationRegistry}.
 */
public interface Observation {

    /**
     * Gets the observed client.
     * 
     * @return the client
     */
    Client getClient();

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
