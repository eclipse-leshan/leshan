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
package org.eclipse.leshan.server.bootstrap.profile;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationStore;

public class DefaultClientProfileProvider implements ClientProfileProvider {

    private final RegistrationStore registrationStore;
    private final LwM2mModelProvider modelProvider;

    public DefaultClientProfileProvider(RegistrationStore registrationStore, LwM2mModelProvider modelProvider) {
        this.registrationStore = registrationStore;
        this.modelProvider = modelProvider;
    }

    @Override
    public ClientProfile getProfile(Identity identity) {
        Registration registration = registrationStore.getRegistrationByIdentity(identity);
        LwM2mModel model = modelProvider.getObjectModel(registration);
        return new ClientProfile(registration, model);
    }

}
