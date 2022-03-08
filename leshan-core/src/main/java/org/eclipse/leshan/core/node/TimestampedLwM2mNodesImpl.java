/*******************************************************************************
 * Copyright (c) 2021 Orange.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Orange - Send with multiple-timestamped values
 *******************************************************************************/
package org.eclipse.leshan.core.node;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The default implementation of {@link TimestampedLwM2mNodes}
 */
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
    public Map<Long, Map<LwM2mPath, LwM2mNode>> getTimestampedNodes() {
        return timestampedPathNodesMap;
    }

    @Override
    public Map<LwM2mPath, LwM2mNode> getNodesForTimestamp(Long timestamp) {
        return timestampedPathNodesMap.get(timestamp);
    }

    @Override
    public Map<LwM2mPath, LwM2mNode> getNodes() {
        Map<LwM2mPath, LwM2mNode> result = new HashMap<>();
        for (Map.Entry<Long, Map<LwM2mPath, LwM2mNode>> entry : timestampedPathNodesMap.entrySet()) {
            result.putAll(entry.getValue());
        }
        return result;
    }

    @Override
    public Set<Long> getTimestamps() {
        return timestampedPathNodesMap.keySet();
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

    public void add(TimestampedLwM2mNodes timestampedNodes) {
        for (Map.Entry<Long, Map<LwM2mPath, LwM2mNode>> entry : timestampedNodes.getTimestampedNodes().entrySet()) {
            Long timestamp = entry.getKey();
            Map<LwM2mPath, LwM2mNode> pathNodeMap = entry.getValue();

            for (Map.Entry<LwM2mPath, LwM2mNode> pathNodeEntry : pathNodeMap.entrySet()) {
                LwM2mPath path = pathNodeEntry.getKey();
                LwM2mNode node = pathNodeEntry.getValue();
                put(timestamp, path, node);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("TimestampedLwM2mNodesImpl [timestampedPathNodesMap=%s, timestampedNodes=%s]",
                timestampedPathNodesMap);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((timestampedPathNodesMap == null) ? 0 : timestampedPathNodesMap.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TimestampedLwM2mNodesImpl other = (TimestampedLwM2mNodesImpl) obj;
        if (timestampedPathNodesMap == null) {
            if (other.timestampedPathNodesMap != null)
                return false;
        } else if (!timestampedPathNodesMap.equals(other.timestampedPathNodesMap))
            return false;
        return true;
    }

}
