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
package org.eclipse.leshan.transport.californium.server;

import java.util.Collection;

import org.eclipse.californium.oscore.OSCoreCtx;
import org.eclipse.californium.oscore.OSCoreCtxDB;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.peer.LwM2mIdentity;
import org.eclipse.leshan.core.peer.OscoreIdentity;
import org.eclipse.leshan.server.registration.IRegistration;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.servers.security.EditableSecurityStore;
import org.eclipse.leshan.servers.security.SecurityInfo;
import org.eclipse.leshan.servers.security.SecurityStoreListener;

/**
 * This class is responsible to remove {@link OSCoreCtx} from {@link OSCoreCtxDB} on some events.
 * <p>
 * {@link OSCoreCtx} is removed when :
 * <ul>
 * <li>a {@link Registration} using OSCORE is removed.
 * <li>an OSCORE {@link SecurityInfo} is removed from {@link EditableSecurityStore}.
 * </ul>
 *
 */
public class OscoreContextCleaner implements RegistrationListener, SecurityStoreListener {

    private final OSCoreCtxDB oscoreCtxDB;

    public OscoreContextCleaner(OSCoreCtxDB oscoreCtxDB) {
        this.oscoreCtxDB = oscoreCtxDB;
    }

    @Override
    public void registered(IRegistration registration, IRegistration previousReg,
            Collection<Observation> previousObsersations) {
    }

    @Override
    public void updated(RegistrationUpdate update, IRegistration updatedReg, IRegistration previousReg) {
    }

    @Override
    public void unregistered(IRegistration registration, Collection<Observation> observations, boolean expired,
            IRegistration newReg) {
        LwM2mIdentity unregisteredIdentity = registration.getClientTransportData().getIdentity();

        // Not an OSCORE identity : nothing to clear
        if (!(unregisteredIdentity instanceof OscoreIdentity))
            return;

        // This is a Re-Registration and client use same OSCORE Identity : nothing to clear
        if (newReg != null && unregisteredIdentity.equals(newReg.getClientTransportData().getIdentity())) {
            return;
        }

        // else
        removeContext(((OscoreIdentity) unregisteredIdentity).getRecipientId());
    }

    @Override
    public void securityInfoRemoved(boolean infosAreCompromised, SecurityInfo... infos) {
        for (SecurityInfo securityInfo : infos) {
            if (securityInfo.useOSCORE()) {
                removeContext(securityInfo.getOscoreSetting().getRecipientId());
            }
        }
    }

    private void removeContext(byte[] rid) {
        OSCoreCtx context = oscoreCtxDB.getContext(rid);
        if (context != null)
            oscoreCtxDB.removeContext(context);
    }
}
