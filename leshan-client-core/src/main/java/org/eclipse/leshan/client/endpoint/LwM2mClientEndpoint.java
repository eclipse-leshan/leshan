package org.eclipse.leshan.client.endpoint;

import java.net.InetSocketAddress;
import java.net.URI;

import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;

public interface LwM2mClientEndpoint {

    Protocol getProtocol();

    URI getURI();

    InetSocketAddress getInetSocketAddress();

    void forceReconnection(ServerIdentity server, boolean resume);

    long getMaxCommunicationPeriodFor(long lifetimeInMs);

    <T extends LwM2mResponse> T send(ServerIdentity server, UplinkRequest<T> request, long timeoutInMs)
            throws InterruptedException;

    <T extends LwM2mResponse> void send(ServerIdentity server, UplinkRequest<T> request,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback, long timeoutInMs);

}
