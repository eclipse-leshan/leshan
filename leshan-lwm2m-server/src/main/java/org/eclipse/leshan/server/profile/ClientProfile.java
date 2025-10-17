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
package org.eclipse.leshan.server.profile;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.server.registration.IRegistration;

public class ClientProfile {

    private final IRegistration registration;
    private final LwM2mModel model;

    public ClientProfile(IRegistration registration, LwM2mModel model) {
        this.registration = registration;
        this.model = model;
    }

    public LwM2mPeer getTransportData() {
        return registration.getClientTransportData();
    }

    public String getRegistrationId() {
        return registration.getId();
    }

    public LwM2mModel getModel() {
        return model;
    }

    public String getEndpoint() {
        return registration.getEndpoint();
    }

    public String getRootPath() {
        return registration.getRootPath();
    }

    public boolean canInitiateConnection() {
        return registration.canInitiateConnection();
    }

    public IRegistration getRegistration() {
        return registration;
    }
}
