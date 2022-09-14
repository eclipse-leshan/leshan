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
package org.eclipse.leshan.client.send;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.request.ContentFormat;

/**
 * A data sender which collects and sends data on a manual API call.
 */
public class ManualDataSender implements DataSender {

    public final static String DEFAULT_NAME = "MANUAL_SENDER";

    private TimestampedLwM2mNodes.Builder builder = new TimestampedLwM2mNodes.Builder();
    private DataSenderManager dataSenderManager;
    private final String name;

    public ManualDataSender() {
        this.name = DEFAULT_NAME;
    }

    public ManualDataSender(String name) {
        this.name = name;
    }

    public synchronized void collectData(List<LwM2mPath> paths) {
        Instant currentTimestamp = Instant.now();
        Map<LwM2mPath, LwM2mNode> currentValues = dataSenderManager.getCurrentValues(ServerIdentity.SYSTEM, paths);
        synchronized (this) {
            builder.addNodes(currentTimestamp, currentValues);
        }
    }

    /**
     * Send collected data.
     *
     * @throws NoDataException if no data was collected before to send
     */
    public void sendCollectedData(ServerIdentity server, ContentFormat format, long timeoutInMs, boolean noFlush)
            throws NoDataException {
        TimestampedLwM2mNodes data;
        synchronized (this) {
            data = builder.build();
            if (data.isEmpty()) {
                throw new NoDataException("Unable to send data to %s : no data collected");
            }
            if (!noFlush) {
                builder = new TimestampedLwM2mNodes.Builder();
            }
        }

        dataSenderManager.sendData(server, format, data, response -> {
            if (response.isFailure()) {
                restoreData(data);
            }
        }, error -> {
            restoreData(data);
        }, timeoutInMs);
    }

    @Override
    public void setDataSenderManager(DataSenderManager dataSenderManager) {
        this.dataSenderManager = dataSenderManager;
    }

    @Override
    public String getName() {
        return name;
    }

    private synchronized void restoreData(TimestampedLwM2mNodes data) {
        builder.add(data);
    }
}
