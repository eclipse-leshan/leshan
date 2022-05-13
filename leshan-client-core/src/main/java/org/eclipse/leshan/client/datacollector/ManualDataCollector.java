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

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;

/**
 * A data collector which collect data on manual API call.
 */
// TODO this should be thread safe
public class ManualDataCollector implements DataCollector {

    private TimestampedLwM2mNodes.Builder builder = new TimestampedLwM2mNodes.Builder();
    private DataCollectorManager manager;

    void collectData(LwM2mPath... paths) {
        long currentTimestamp = System.currentTimeMillis();
        Map<LwM2mPath, LwM2mNode> values = manager.readFromEnabler(paths);
        builder.addNodes(currentTimestamp, values);
    };

    @Override
    public void setDataCollectorManager(DataCollectorManager dataCollectorManager) {
        manager = dataCollectorManager;
    }

    @Override
    public TimestampedLwM2mNodes getTimestampedNodes() {
        return builder.build();
    }
}
