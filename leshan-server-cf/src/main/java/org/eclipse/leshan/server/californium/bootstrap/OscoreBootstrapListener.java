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