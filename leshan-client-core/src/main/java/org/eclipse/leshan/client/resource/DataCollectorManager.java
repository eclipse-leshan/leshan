package org.eclipse.leshan.client.resource;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DataCollectorManager {
    private final Map<LwM2mPath, DataCollector> dataCollectors;

    public DataCollectorManager(Map<LwM2mPath, DataCollector> dataCollectors) {
        this.dataCollectors = dataCollectors != null ? dataCollectors : new HashMap<>();
    }

    public TimestampedLwM2mNodes readFromDataCollectors(Collection<String> paths, boolean clearExistingNodes) {
        //TODO: Very inefficient to concatenate already built objects, it would be better to read Timestamp-Node maps from each collector and then add them to one builder and build at the end
        TimestampedLwM2mNodes.Builder builder = TimestampedLwM2mNodes.builder();
        for(String pathString : paths) {
            LwM2mPath path = new LwM2mPath(pathString);
            DataCollector dataCollector = dataCollectors.get(path);
            if(dataCollector == null) {
                throw new RuntimeException(String.format("There is no Data Collector assigned to node %s", path));
            }
            builder.add(dataCollector.getTimestampedNodes(clearExistingNodes));
        }
        return builder.build();
    }
}
