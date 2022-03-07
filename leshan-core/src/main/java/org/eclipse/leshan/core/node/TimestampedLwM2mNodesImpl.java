package org.eclipse.leshan.core.node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class TimestampedLwM2mNodesImpl implements TimestampedLwM2mNodes {

    private final Map<Long, Map<LwM2mPath, LwM2mNode>> timestampedPathNodesMap = new TreeMap<>((o1, o2) -> {
        if (o1 == null) {
            return (o2 == null) ? 0 : -1;
        } else if (o2 == null) {
            return 1;
        } else {
            return o1.compareTo(o2);
        }
    });

    public TimestampedLwM2mNodesImpl() {
    }

    public TimestampedLwM2mNodesImpl(Map<LwM2mPath, LwM2mNode> pathNodesMap) {
        timestampedPathNodesMap.put(null, pathNodesMap);
    }

    public TimestampedLwM2mNodesImpl(Long timestamp, LwM2mPath path, LwM2mNode node) {
        if (!timestampedPathNodesMap.containsKey(timestamp)) {
            timestampedPathNodesMap.put(timestamp, new HashMap<>());
        }
        timestampedPathNodesMap.get(timestamp).put(path, node);
    }

    @Override
    public Map<Long, Map<LwM2mPath, LwM2mNode>> getTimestampedPathNodesMap() {
        return timestampedPathNodesMap;
    }

    @Override
    public Map<String, LwM2mNode> getStrPathNodesMap() {
        Map<String, LwM2mNode> result = new HashMap<>();
        for (Map.Entry<LwM2mPath, LwM2mNode> entry : getPathNodesMap().entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue());
        }
        return result;
    }

    @Override
    public Map<LwM2mPath, LwM2mNode> getPathNodesMap() {
        Iterator<Map.Entry<Long, Map<LwM2mPath, LwM2mNode>>> iterator = timestampedPathNodesMap.entrySet().iterator();
        if (iterator.hasNext()) {
            return iterator.next().getValue();
        } else {
            return null;
        }
    }

    @Override
    public LwM2mNode getFirstNode() {
        return getPathNodesMap().entrySet().iterator().next().getValue();
    }

    @Override
    public Set<LwM2mPath> getPaths() {
        Set<LwM2mPath> paths = new HashSet<>();
        for (Map.Entry<Long, Map<LwM2mPath, LwM2mNode>> tsEntry: timestampedPathNodesMap.entrySet()) {
            for (Map.Entry<LwM2mPath, LwM2mNode> entry: tsEntry.getValue().entrySet()) {
                paths.add(entry.getKey());
            }
        }
        return paths;
    }

    @Override
    public Set<Long> getTimestamps() {
        return timestampedPathNodesMap.keySet();
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
