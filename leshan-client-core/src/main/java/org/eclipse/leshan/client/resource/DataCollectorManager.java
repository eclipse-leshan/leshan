package org.eclipse.leshan.client.resource;

import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.response.ReadCompositeResponse;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DataCollectorManager {
    private final Map<LwM2mPath, DataCollector> dataCollectors;
    private final LwM2mRootEnabler rootEnabler;

    public DataCollectorManager(Map<LwM2mPath, DataCollector> dataCollectors, LwM2mRootEnabler rootEnabler) {
        this.rootEnabler = rootEnabler;
        this.dataCollectors = dataCollectors != null ? dataCollectors : new HashMap<>();
    }

    public ReadCompositeResponse readFromEnabler(ServerIdentity identity, ReadCompositeRequest request) {
        return rootEnabler.read(identity, request);
    }

    public TimestampedLwM2mNodes readFromDataCollectors(Collection<String> paths, boolean clearExistingNodes) {
        TimestampedLwM2mNodes.Builder builder = TimestampedLwM2mNodes.builder();
        for (String collectorPathString : paths) {
            LwM2mPath collectorPath = new LwM2mPath(collectorPathString);
            DataCollector dataCollector = dataCollectors.get(collectorPath);
            if (dataCollector == null) {
                throw new RuntimeException(
                        String.format("There is no Data Collector assigned to node %s", collectorPath));
            }
            dataCollector.getTimestampedNodes(clearExistingNodes).forEach((timestamp, pathNodeMap) -> {
                pathNodeMap.forEach((path, node) -> {
                    builder.put(timestamp, path, node);
                });
            });
        }
        return builder.build();
    }
}
