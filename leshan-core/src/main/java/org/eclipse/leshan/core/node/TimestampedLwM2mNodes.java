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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A container for nodes {@link LwM2mNode} with path {@link LwM2mPath} and optional timestamp information.
 */
public class TimestampedLwM2mNodes {

    private final Map<Long, Map<LwM2mPath, LwM2mNode>> timestampedPathNodesMap;

    private TimestampedLwM2mNodes(Map<Long, Map<LwM2mPath, LwM2mNode>> timestampedPathNodesMap) {
        this.timestampedPathNodesMap = timestampedPathNodesMap;
    }

    /**
     * Get maps {@link LwM2mPath}-{@link LwM2mNode} grouped by timestamp with ascending order.
     * Null timestamp keys are allowed, and are considered as most recent one.
     */
    public Map<Long, Map<LwM2mPath, LwM2mNode>> getTimestampedNodes() {
        return timestampedPathNodesMap;
    }

    /**
     * Get nodes for specific timestamp. Null timestamp is allowed.
     *
     * @param timestamp
     * @return map of {@link LwM2mPath}-{@link LwM2mNode} or null if there is no value for asked timestamp.
     */
    public Map<LwM2mPath, LwM2mNode> getNodesForTimestamp(Long timestamp) {
        return timestampedPathNodesMap.get(timestamp);
    }

    /**
     * Get all collected nodes as {@link LwM2mPath}-{@link LwM2mNode} map ignoring timestamp information. In case
     * of the same path conflict the most recent one is taken. Null timestamp is considered as most recent one.
     */
    public Map<LwM2mPath, LwM2mNode> getNodes() {
        Map<LwM2mPath, LwM2mNode> result = new HashMap<>();
        for (Map.Entry<Long, Map<LwM2mPath, LwM2mNode>> entry : timestampedPathNodesMap.entrySet()) {
            result.putAll(entry.getValue());
        }
        return result;
    }

    /**
     * Returns the all sorted timestamps of contained nodes with ascending order. Null timestamp is considered as most
     * recent one.
     */
    public Set<Long> getTimestamps() {
        return timestampedPathNodesMap.keySet();
    }

    @Override
    public String toString() {
        return String.format("TimestampedLwM2mNodes [timestampedPathNodesMap=%s, timestampedNodes=%s]",
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
        TimestampedLwM2mNodes other = (TimestampedLwM2mNodes) obj;
        if (timestampedPathNodesMap == null) {
            if (other.timestampedPathNodesMap != null)
                return false;
        } else if (!timestampedPathNodesMap.equals(other.timestampedPathNodesMap))
            return false;
        return true;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Map<Long, Map<LwM2mPath, LwM2mNode>> timestampedPathNodesMap = getTimestampedMap();

        public Builder addNodes(Map<LwM2mPath, LwM2mNode> pathNodesMap) {
            timestampedPathNodesMap.put(null, pathNodesMap);
            return this;
        }

        public Builder put(Long timestamp, LwM2mPath path, LwM2mNode node) {
            if (!timestampedPathNodesMap.containsKey(timestamp)) {
                timestampedPathNodesMap.put(timestamp, new HashMap<>());
            }
            timestampedPathNodesMap.get(timestamp).put(path, node);
            return this;
        }

        public Builder put(LwM2mPath path, LwM2mNode node) {
            put(null, path, node);
            return this;
        }

        public Builder add(TimestampedLwM2mNodes timestampedNodes) {
            for (Map.Entry<Long, Map<LwM2mPath, LwM2mNode>> entry : timestampedNodes.getTimestampedNodes().entrySet()) {
                Long timestamp = entry.getKey();
                Map<LwM2mPath, LwM2mNode> pathNodeMap = entry.getValue();

                for (Map.Entry<LwM2mPath, LwM2mNode> pathNodeEntry : pathNodeMap.entrySet()) {
                    LwM2mPath path = pathNodeEntry.getKey();
                    LwM2mNode node = pathNodeEntry.getValue();
                    put(timestamp, path, node);
                }
            }
            return this;
        }

        public TimestampedLwM2mNodes build() {
            return new TimestampedLwM2mNodes(timestampedPathNodesMap);
        }

        private static TreeMap<Long, Map<LwM2mPath, LwM2mNode>> getTimestampedMap() {
            return new TreeMap<>(getTimestampComparator());
        }

        private static Comparator<Long> getTimestampComparator() {
            return (o1, o2) -> {
                if (o1 == null) {
                    return (o2 == null) ? 0 : 1;
                } else if (o2 == null) {
                    return -1;
                } else {
                    return o1.compareTo(o2);
                }
            };
        }
    }
}