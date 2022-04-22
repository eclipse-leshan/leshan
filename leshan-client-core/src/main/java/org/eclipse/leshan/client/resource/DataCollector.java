package org.eclipse.leshan.client.resource;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface DataCollector {
    void startPeriodicRead(int initialDelay, int period, TimeUnit timeUnit);

    void stopPeriodicRead();

    Map<LwM2mPath, LwM2mNode> readFromEnabler();

    TimestampedLwM2mNodes getTimestampedNodes(boolean clearExistingNodes);
}
