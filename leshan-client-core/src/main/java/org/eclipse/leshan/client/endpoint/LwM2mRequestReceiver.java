package org.eclipse.leshan.client.endpoint;

import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.LwM2mRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.SendableResponse;

public interface LwM2mRequestReceiver {

    <T extends LwM2mResponse> SendableResponse<T> requestReceived(ServerIdentity identity, DownlinkRequest<T> request);

    void onError(ServerIdentity identity, Exception e,
            Class<? extends LwM2mRequest<? extends LwM2mResponse>> requestType);
}
