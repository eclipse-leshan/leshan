package org.eclipse.leshan.client.resource;

import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DefaultDataCollector implements DataCollector {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultDataCollector.class);
    private final Map<Long, Map<LwM2mPath, LwM2mNode>> collectedTimestampedNodes = new HashMap<>();
    private final Object timestampedNodesSynchronizer = new Object();
    private final ServerIdentity identity;
    private final LwM2mPath path;
    private final ScheduledExecutorService scheduler;
    private DataCollectorManager dataCollectorManager;
    private boolean isCollectorReading;
    private ScheduledFuture<?> future;

    public DefaultDataCollector(ServerIdentity identity, LwM2mPath path, String name) {
        this.identity = identity;
        this.path = path;
        scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(name));
    }

    @Override public void startPeriodicRead(int initialDelay, int period, TimeUnit timeUnit) {
        if (isCollectorReading) {
            LOG.error("Cannot start periodic read, as it has already started");
            return;
        }
        if (dataCollectorManager == null) {
            LOG.error("Cannot start periodic read before providing DataCollectorManager");
            return;
        }
        LOG.info("---------------STARTED PERIODIC READ---------------");
        future = scheduler.scheduleAtFixedRate(this::readAndAddTimestampedValue, initialDelay, period, timeUnit);
        isCollectorReading = true;
    }

    @Override public void stopPeriodicRead() {
        if (!isCollectorReading) {
            LOG.error("Cannot stop periodic read, as it has not yet started");
            return;
        }
        future.cancel(false);
        future = null;
        isCollectorReading = false;
    }

    @Override public Map<LwM2mPath, LwM2mNode> readFromEnabler() {
        ContentFormat format = ContentFormat.SENML_CBOR;
        ReadCompositeRequest request = new ReadCompositeRequest(format, format,
                Collections.singletonList(path.toString()));
        ReadCompositeResponse response = dataCollectorManager.readFromEnabler(identity, request);
        if (response.isFailure()) {
            throw new RuntimeException(response.getErrorMessage());
        }
        Map<LwM2mPath, LwM2mNode> content = response.getContent();
        LOG.debug("Read from {} and received {}", path, content.toString());
        return content;
    }

    @Override public Map<Long, Map<LwM2mPath, LwM2mNode>> getTimestampedNodes(boolean clearExistingNodes) {
        Map<Long, Map<LwM2mPath, LwM2mNode>> dataToReturn = new HashMap<>(collectedTimestampedNodes);
        if (clearExistingNodes) {
            collectedTimestampedNodes.clear();
        }
        LOG.info("Retrieving collection of {} timestamped Path-Node Maps", dataToReturn.size());
        return dataToReturn;
    }

    @Override public void setDataCollectorManager(DataCollectorManager dataCollectorManager) {
        this.dataCollectorManager = dataCollectorManager;
    }

    private void readAndAddTimestampedValue() {
        long currentTime = System.currentTimeMillis();
        try {
            synchronized (timestampedNodesSynchronizer) {
                collectedTimestampedNodes.put(currentTime, readFromEnabler());
            }
        } catch (RuntimeException exception) {
            LOG.error("Failed to read from {}. Error message: {}", path, exception.getMessage());
        }
    }
}
