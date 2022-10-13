/*******************************************************************************
 * Copyright (c) 2022    Sierra Wireless and others.
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

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.leshan.server.bootstrap.BootstrapFailureCause;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionAdapter;

// TODO OSCORE temporary code to be able to implement get by uri in LwM2mBootstrapOscoreStore
// but this should be removed when https://github.com/eclipse/leshan/pull/1212#discussion_r831056432 will be implemented
public class OscoreBootstrapListener extends BootstrapSessionAdapter {

    private final ConcurrentMap<InetSocketAddress, BootstrapSession> addrToSession = new ConcurrentHashMap<>();

    @Override
    public void authorized(BootstrapSession session) {
        addrToSession.put(session.getIdentity().getPeerAddress(), session);
    }

    @Override
    public void end(BootstrapSession session) {
        addrToSession.remove(session.getIdentity(), session);

    }

    @Override
    public void failed(BootstrapSession session, BootstrapFailureCause cause) {
        addrToSession.remove(session.getIdentity(), session);
    }

    public BootstrapSession getSessionByAddr(InetSocketAddress addr) {
        return addrToSession.get(addr);
    }
}
