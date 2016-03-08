/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
package org.eclipse.leshan.server.californium;

import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.observe.NotificationListener;
import org.eclipse.californium.core.observe.ObservationStore;
import org.eclipse.leshan.server.observation.ObservationRegistry;

public interface CaliforniumObservationRegistry extends ObservationRegistry, NotificationListener {

    ObservationStore getObservationStore();

    void setNonSecureEndpoint(Endpoint endpoint);

    void setSecureEndpoint(Endpoint endpoint);
}
