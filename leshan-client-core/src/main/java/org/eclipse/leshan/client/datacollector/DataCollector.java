package org.eclipse.leshan.client.datacollector;

import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;

/**
 * A class responsible to collect data.
 */
public interface DataCollector {

    TimestampedLwM2mNodes getTimestampedNodes();

    void setDataCollectorManager(DataCollectorManager dataCollectorManager);

    // Maybe a data collector could need to start / stop / flushData on registration lifecyle event ?
    // see LwM2mClientObserver
    // void setLwM2MClient(LwM2mClient client);
}
