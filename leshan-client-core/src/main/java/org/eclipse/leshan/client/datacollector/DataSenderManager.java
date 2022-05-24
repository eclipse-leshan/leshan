package org.eclipse.leshan.client.datacollector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.client.request.LwM2mRequestSender;
import org.eclipse.leshan.client.resource.LwM2mRootEnabler;
import org.eclipse.leshan.client.send.NoDataException;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.Startable;
import org.eclipse.leshan.core.Stoppable;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.response.SendResponse;

// TODO I don't know if we need a DataSenderManager ?
// or if we should directly pass a LwM2mClient to DataSender
public class DataSenderManager implements Startable, Stoppable, Destroyable {

    private final List<DataSender> dataSenders;
    private final LwM2mRootEnabler rootEnabler;
    private final LwM2mRequestSender requestSender;

    public DataSenderManager(List<DataSender> dataSenders, LwM2mRootEnabler rootEnabler,
            LwM2mRequestSender requestSender) {
        this.rootEnabler = rootEnabler;
        this.requestSender = requestSender;
        this.dataSenders = dataSenders != null ? dataSenders : new ArrayList<DataSender>();
    }

    /**
     * Get current LWM2M node value.
     */
    public Map<LwM2mPath, LwM2mNode> getCurrentValue(ServerIdentity server, LwM2mPath... paths) throws NoDataException {
        return getCurrentValue(server, Arrays.asList(paths));
    }

    /**
     * Get current LWM2M node value.
     */
    public Map<LwM2mPath, LwM2mNode> getCurrentValue(ServerIdentity server, List<LwM2mPath> paths)
            throws NoDataException {
        // HACK we use SENML_CBOR content format but it will not be really used...
        ReadCompositeResponse response = rootEnabler.read(server,
                new ReadCompositeRequest(paths, ContentFormat.SENML_CBOR, ContentFormat.SENML_CBOR, null));
        if (response.isSuccess()) {
            return response.getContent();
        } else {
            throw new NoDataException("Unable to collect data for %s : %s / %s", paths, response.getCode(),
                    response.getErrorMessage());
        }
    }

    /**
     * Send data Asynchronously
     */
    public void sendData(ServerIdentity server, ContentFormat format, Map<LwM2mPath, LwM2mNode> nodes,
            ResponseCallback<SendResponse> onResponse, ErrorCallback onError, long timeoutInMs) {
        requestSender.send(server, new SendRequest(format, nodes, null), timeoutInMs, onResponse, onError);
    }

    /**
     * Send data synchronously.
     */
    public SendResponse sendData(ServerIdentity server, ContentFormat format, Map<LwM2mPath, LwM2mNode> nodes,
            long timeoutInMs) throws InterruptedException {
        return requestSender.send(server, new SendRequest(format, nodes, null), timeoutInMs);
    }

    /**
     * Send timestamped data Asynchronously
     */
    public void sendData(ServerIdentity server, ContentFormat format, TimestampedLwM2mNodes nodes,
            ResponseCallback<SendResponse> onResponse, ErrorCallback onError, long timeoutInMs) {
        requestSender.send(server, new SendRequest(format, nodes, null), timeoutInMs, onResponse, onError);
    }

    /**
     * Send timestamped data synchronously.
     */
    public SendResponse sendData(ServerIdentity server, ContentFormat format, TimestampedLwM2mNodes nodes,
            long timeoutInMs) throws InterruptedException {
        return requestSender.send(server, new SendRequest(format, nodes, null), timeoutInMs);
    }

    @Override
    public void start() {
        for (DataSender dataSender : dataSenders) {
            if (dataSender instanceof Startable) {
                ((Startable) dataSender).start();
            }
        }
    }

    @Override
    public void stop() {
        for (DataSender dataSender : dataSenders) {
            if (dataSender instanceof Stoppable) {
                ((Stoppable) dataSender).stop();
            }
        }
    }

    @Override
    public void destroy() {
        for (DataSender dataSender : dataSenders) {
            if (dataSender instanceof Destroyable) {
                ((Destroyable) dataSender).destroy();
            } else if (dataSender instanceof Stoppable) {
                ((Stoppable) dataSender).stop();
            }
        }
    }
}
