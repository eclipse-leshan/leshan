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
package org.eclipse.leshan.client.send;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.client.request.UplinkRequestSender;
import org.eclipse.leshan.client.resource.LwM2mRootEnabler;
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

public class DataSenderManager implements Startable, Stoppable, Destroyable {
    private final Map<String, DataSender> dataSenders;
    private final LwM2mRootEnabler rootEnabler;
    private final UplinkRequestSender requestSender;

    public DataSenderManager(Map<String, DataSender> dataSenders, LwM2mRootEnabler rootEnabler,
            UplinkRequestSender requestSender) {
        this.rootEnabler = rootEnabler;
        this.requestSender = requestSender;
        this.dataSenders = dataSenders != null ? dataSenders : new HashMap<>();
        for (DataSender sender : this.dataSenders.values()) {
            sender.setDataSenderManager(this);
        }
    }

    /**
     * Retrieves the nodes with current values defined by a list of paths to those nodes.
     *
     * @param server to which the data will be sent
     * @param paths from which nodes should the values be retrieved
     * @throws NoDataException if a read request turns out to be unsuccessful
     * @return retrieved nodes mapped by their respective paths
     */
    public Map<LwM2mPath, LwM2mNode> getCurrentValues(ServerIdentity server, List<LwM2mPath> paths)
            throws NoDataException {
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
     * Retrieves a data sender by its name
     *
     * @param senderName name of the sender
     * @throws IllegalArgumentException if there is no data sender with specified name
     * @return a retrieved data sender
     */
    public DataSender getDataSender(String senderName) {
        DataSender dataSender = dataSenders.get(senderName);
        if (dataSender == null) {
            throw new IllegalArgumentException(String.format("There is no Data Sender with name \"%s\"", senderName));
        }
        return dataSender;
    }

    /**
     * Retrieves a data sender by its name, cast to a specified subtype
     *
     * @param senderName name of the sender
     * @param senderSubType subtype to which the data sender should be cast
     * @throws IllegalArgumentException if there is no data sender with specified name or the data sender with specified
     *         name is not of the provided subtype
     * @return a retrieved data sender
     */
    public <T extends DataSender> T getDataSender(String senderName, Class<T> senderSubType) {
        DataSender dataSender = getDataSender(senderName);
        T castSender;
        try {
            castSender = senderSubType.cast(dataSender);
        } catch (ClassCastException exception) {
            throw new IllegalArgumentException(
                    String.format("Data Sender with name \"%s\" is of type %s instead of provided type %s.", senderName,
                            dataSender.getClass().getSimpleName(), senderSubType.getSimpleName()));
        }
        return castSender;
    }

    /**
     * Send data asynchronously
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
     * Send timestamped data asynchronously
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
        for (DataSender dataSender : dataSenders.values()) {
            if (dataSender instanceof Startable) {
                ((Startable) dataSender).start();
            }
        }
    }

    @Override
    public void stop() {
        for (DataSender dataSender : dataSenders.values()) {
            if (dataSender instanceof Stoppable) {
                ((Stoppable) dataSender).stop();
            }
        }
    }

    @Override
    public void destroy() {
        for (DataSender dataSender : dataSenders.values()) {
            if (dataSender instanceof Destroyable) {
                ((Destroyable) dataSender).destroy();
            } else if (dataSender instanceof Stoppable) {
                ((Stoppable) dataSender).stop();
            }
        }
    }
}
