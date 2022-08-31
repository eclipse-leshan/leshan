package org.eclipse.leshan.client.endpoint;

import java.security.cert.Certificate;
import java.util.Collection;
import java.util.List;

import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.client.servers.ServerInfo;

public interface LwM2mEndpointsProvider {

    void init(LwM2mObjectTree objectTree, LwM2mRequestReceiver requestReceiver, ClientEndpointToolbox toolbox);

    ServerIdentity createEndpoint(ServerInfo serverInfo, boolean clientInitiatedOnly, List<Certificate> trustStore,
            ClientEndpointToolbox toolbox);

    Collection<ServerIdentity> createEndpoints(Collection<? extends ServerInfo> serverInfo, boolean clientInitiatedOnly,
            List<Certificate> trustStore, ClientEndpointToolbox toolbox);

    void destroyEndpoints();

    void start();

    List<LwM2mClientEndpoint> getEndpoints();

    LwM2mClientEndpoint getEndpoint(ServerIdentity server);

    void stop();

    void destroy();

}
