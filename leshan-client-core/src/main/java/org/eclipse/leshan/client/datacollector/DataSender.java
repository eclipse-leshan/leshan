package org.eclipse.leshan.client.datacollector;

/**
 * A class responsible to collect data.
 */
public interface DataSender {

    void setDataCollectorManager(DataSenderManager dataCollectorManager);

    // Maybe a data collector could need to start / stop / flushData on registration lifecyle event ?
    // see LwM2mClientObserver
    // void setLwM2MClient(LwM2mClient client);
}
