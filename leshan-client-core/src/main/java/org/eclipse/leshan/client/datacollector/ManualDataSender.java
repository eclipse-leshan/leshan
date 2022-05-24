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
package org.eclipse.leshan.client.datacollector;

import java.util.Map;

import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.response.SendResponse;

/**
 * A data sender which collect and send data on manual API call.
 */
public class ManualDataSender implements DataSender {

    private TimestampedLwM2mNodes.Builder builder = new TimestampedLwM2mNodes.Builder();
    private DataSenderManager manager;

    public synchronized void collectData(LwM2mPath... paths) {
        // Here we use ServerIdentity.System to read data but we should probably use a real server ?
        // to be sure we only collect allowed data ? (e.g. if one day we implement ACL)
        // Maybe this means that a collector could only act for 1 server (but this could generate duplicate collection
        // of same data)?
        // OR we collect everything with ServerIdentity.SYSTEM and then we filter on send depending to who we send (but
        // this will generate duplicate checks)?
        // OR we have a map of collected data by server
        // anyway for now we don't support ACL and we only support 1 server, so I don't know if this is a real question.

        long currentTimestamp = System.currentTimeMillis();
        Map<LwM2mPath, LwM2mNode> currentValues = manager.getCurrentValue(ServerIdentity.SYSTEM, paths);
        synchronized (this) {
            builder.addNodes(currentTimestamp, currentValues);
        }
    };

    // TODO maybe we can add `noFlush` argument to force to not flush data even if send success ?
    public synchronized void sendCollectedData(ServerIdentity server, ContentFormat format, long timeoutInMs) {
        // replace builder to collect new data in another builder
        TimestampedLwM2mNodes data;
        synchronized (this) {
            data = builder.build();
            builder = new TimestampedLwM2mNodes.Builder();
        }

        manager.sendData(server, format, data, new ResponseCallback<SendResponse>() {
            @Override
            public void onResponse(SendResponse response) {
                if (response.isFailure()) {
                    // restore data to resent it later
                    restoreData(data);
                }
            }
        }, new ErrorCallback() {
            @Override
            public void onError(Exception e) {
                // restore data to resent it later
                restoreData(data);
            }
        }, timeoutInMs);
    }

    private synchronized void restoreData(TimestampedLwM2mNodes data) {
        builder.add(data);
    }

    @Override
    public void setDataCollectorManager(DataSenderManager dataCollectorManager) {
        manager = dataCollectorManager;
    }
}
