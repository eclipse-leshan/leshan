package org.eclipse.leshan.client.resource;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface DataCollector {
    void startPeriodicRead(int initialDelay, int period, TimeUnit timeUnit);

    void stopPeriodicRead();

    Map<LwM2mPath, LwM2mNode> readFromEnabler();

    Map<Long, Map<LwM2mPath, LwM2mNode>> getTimestampedNodes(boolean clearExistingNodes);

    void setDataCollectorManager(DataCollectorManager dataCollectorManager);
}
