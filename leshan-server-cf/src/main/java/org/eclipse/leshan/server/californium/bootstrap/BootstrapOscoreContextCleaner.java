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
package org.eclipse.leshan.server.californium.bootstrap;

import org.eclipse.californium.oscore.OSCoreCtx;
import org.eclipse.californium.oscore.OSCoreCtxDB;
import org.eclipse.leshan.server.bootstrap.BootstrapFailureCause;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionAdapter;

/**
 * This class is responsible to remove {@link OSCoreCtx} from {@link OSCoreCtxDB} on some events.
 * <p>
 * {@link OSCoreCtx} is removed when :
 * <ul>
 * <li>a {@link BootstrapSession} using OSCORE ends (success or failure).
 * </ul>
 */
public class BootstrapOscoreContextCleaner extends BootstrapSessionAdapter {

    private final OSCoreCtxDB oscoreCtxDB;

    public BootstrapOscoreContextCleaner(OSCoreCtxDB oscoreCtxDB) {
        this.oscoreCtxDB = oscoreCtxDB;
    }

    // TODO OSCORE remove this method when new API will be added to OSCoreCtxDB
    private void removeContext(byte[] rid) {
        OSCoreCtx context = oscoreCtxDB.getContext(rid);
        if (context != null)
            oscoreCtxDB.removeContext(context);
    }

    @Override
    public void end(BootstrapSession session) {
        if (session.getIdentity().isOSCORE()) {
            removeContext(session.getIdentity().getOscoreIdentity().getRecipientId());
        }

    }

    @Override
    public void failed(BootstrapSession session, BootstrapFailureCause cause) {
        if (session.getIdentity().isOSCORE()) {
            removeContext(session.getIdentity().getOscoreIdentity().getRecipientId());
        }
    }
}
