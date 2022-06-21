package org.eclipse.leshan.client.datacollector;

/**
 * A class responsible for collecting and sending collected data.
 */
public interface DataSender {
    String DEFAULT_NAME = "DEFAULT_DATA_SENDER";

    void setDataSenderManager(DataSenderManager dataSenderManager);

    String getName();
}
