package org.eclipse.leshan.client.send;

/**
 * A class responsible for collecting and sending collected data.
 * <p>
 * {@link DataSender} are stored in a in {@link DataSenderManager} and can be retrieve by name using
 * {@link DataSenderManager#getDataSender(String)}.
 */
public interface DataSender {
    /**
     * Set the {@link DataSenderManager} which holds this {@link DataSender}.
     */
    void setDataSenderManager(DataSenderManager dataSenderManager);

    String getName();
}
