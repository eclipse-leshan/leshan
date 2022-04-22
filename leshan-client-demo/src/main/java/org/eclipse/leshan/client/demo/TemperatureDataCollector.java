package org.eclipse.leshan.client.demo;

import org.eclipse.leshan.client.resource.DataCollector;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TemperatureDataCollector implements DataCollector, Destroyable {
    private static final Logger LOG = LoggerFactory.getLogger(TemperatureDataCollector.class);
    private final Object timestampedNodesSynchronizer = new Object();
    private final LwM2mInstanceEnabler lwM2mInstanceEnabler;
    private final ServerIdentity identity;
    private final LwM2mPath path;
    private final ScheduledExecutorService scheduler;
    private TimestampedLwM2mNodes.Builder timestampedNodesBuilder = TimestampedLwM2mNodes.builder();
    private boolean isCollectorReading;
    private ScheduledFuture<?> future;

    public TemperatureDataCollector(LwM2mInstanceEnabler lwM2mInstanceEnabler, ServerIdentity identity,
            LwM2mPath path) {
        this.lwM2mInstanceEnabler = lwM2mInstanceEnabler;
        this.identity = identity;
        this.path = path;
        scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Temperature Data Collector"));
    }

    @Override public void startPeriodicRead(int initialDelay, int period, TimeUnit timeUnit) {
        if (isCollectorReading) {
            LOG.error("Cannot start periodic read, as it has already started");
            return;
        }
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

    private void readAndAddTimestampedValue() {
        long currentTime = System.currentTimeMillis();
        try {
            Map<LwM2mPath, LwM2mNode> readValueMap = readFromEnabler();
            synchronized (timestampedNodesSynchronizer) {
                readValueMap.forEach((path, node) -> timestampedNodesBuilder.put(currentTime, path, node));
            }
        } catch (RuntimeException exception) {
            LOG.error("Failed to read from {}. Error message: {}", path, exception.getMessage());
        }
    }

    @Override public Map<LwM2mPath, LwM2mNode> readFromEnabler() {
        int resourceId = path.getResourceId();
        ReadResponse response = lwM2mInstanceEnabler.read(identity, resourceId);
        if(response.isFailure()) {
            throw new RuntimeException(response.getErrorMessage());
        }
        LwM2mNode content = response.getContent();
        LOG.debug("Read from {} and received {}", path, content.toString());
        return Collections.singletonMap(path, content);
    }

    @Override public TimestampedLwM2mNodes getTimestampedNodes(boolean clearExistingNodes) {
        TimestampedLwM2mNodes timestampedLwM2mNodes;
        synchronized (timestampedNodesSynchronizer) {
            timestampedLwM2mNodes = timestampedNodesBuilder.build();
            LOG.info("Retrieving collection of {} timestamped Path-Node Maps",
                    timestampedLwM2mNodes.getTimestamps().size());
            if (clearExistingNodes) {
                timestampedNodesBuilder = TimestampedLwM2mNodes.builder();
            }
        }
        return timestampedLwM2mNodes;
    }

    @Override public void destroy() {
        scheduler.shutdown();
    }
}
