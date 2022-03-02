package org.eclipse.leshan.core.node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TimestampedLwM2mNodes {

    private final Map<Long, Map<LwM2mPath, LwM2mNode>> timestampedPathNodesMap;

    public TimestampedLwM2mNodes() {
        timestampedPathNodesMap = new HashMap<>();
    }

    public TimestampedLwM2mNodes(Map<LwM2mPath, LwM2mNode> pathNodesMap) {
        this();
        this.timestampedPathNodesMap.put(null, pathNodesMap);
    }

    public TimestampedLwM2mNodes(LwM2mPath path, LwM2mNode node) {
        this();
        put(path, node);
    }

    public TimestampedLwM2mNodes(LwM2mPath path, List<TimestampedLwM2mNode> timestampedNodes) {
        this();

        for (TimestampedLwM2mNode timestampedNode : timestampedNodes) {
            Long timestamp = timestampedNode.getTimestamp();
            if (!timestampedPathNodesMap.containsKey(timestamp)) {
                timestampedPathNodesMap.put(timestamp, new HashMap<>());
            }
            timestampedPathNodesMap.get(timestamp).put(path, timestampedNode.getNode());
        }
    }

    public Map<Long, Map<LwM2mPath, LwM2mNode>> getTimestampedPathNodesMap() {
        return timestampedPathNodesMap;
    }

    public Map<String, LwM2mNode> getStrPathNodesMap() {
        Map<String, LwM2mNode> result = new HashMap<>();
        for (Map.Entry<LwM2mPath, LwM2mNode> entry : getPathNodesMap().entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue());
        }
        return result;
    }

    public void put(String path, LwM2mNode node) {
        put(new LwM2mPath(path), node);
    }

    public void put(Long timestamp, LwM2mPath path, LwM2mNode node) {
        if (!timestampedPathNodesMap.containsKey(timestamp)) {
            timestampedPathNodesMap.put(timestamp, new HashMap<>());
        }
        timestampedPathNodesMap.get(timestamp).put(path, node);
    }

    public void put(LwM2mPath path, LwM2mNode node) {
        put(null, path, node);
    }

    public Map<LwM2mPath, LwM2mNode> getPathNodesMap() {
        Iterator<Map.Entry<Long, Map<LwM2mPath, LwM2mNode>>> iterator = timestampedPathNodesMap.entrySet().iterator();
        if (iterator.hasNext()) {
            return iterator.next().getValue();
        } else {
            return null;
        }
    }

    public LwM2mNode getFirstNode() {
        return getPathNodesMap().entrySet().iterator().next().getValue();
    }

    public Set<LwM2mPath> getPaths() {
        Set<LwM2mPath> paths = new HashSet<>();
        for (Map.Entry<Long, Map<LwM2mPath, LwM2mNode>> tsEntry: timestampedPathNodesMap.entrySet()) {
            for (Map.Entry<LwM2mPath, LwM2mNode> entry: tsEntry.getValue().entrySet()) {
                paths.add(entry.getKey());
            }
        }
        return paths;
    }

    public Set<Long> getTimestamps() {
        return timestampedPathNodesMap.keySet();
    }

    public void add(TimestampedLwM2mNodes parseRecords) {
        for( Map.Entry<Long, Map<LwM2mPath, LwM2mNode>> timestampEntry : parseRecords.getTimestampedPathNodesMap().entrySet()) {
            Long timestamp = timestampEntry.getKey();
            Map<LwM2mPath, LwM2mNode> pathNodeMap = timestampEntry.getValue();

            for (Map.Entry<LwM2mPath, LwM2mNode> pathNodeEntry: pathNodeMap.entrySet()) {
                LwM2mPath path = pathNodeEntry.getKey();
                LwM2mNode node = pathNodeEntry.getValue();
                put(timestamp, path, node);
            }
        }
    }
}
