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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * A container for nodes {@link LwM2mNode} with path {@link LwM2mPath} and optional timestamp information.
 */
public class TimestampedLwM2mNodes {

    private final Map<Instant, Map<LwM2mPath, LwM2mNode>> timestampedPathNodesMap;

    private TimestampedLwM2mNodes(Map<Instant, Map<LwM2mPath, LwM2mNode>> timestampedPathNodesMap) {
        this.timestampedPathNodesMap = timestampedPathNodesMap;
    }

    /**
     * Get nodes for specific timestamp. Null timestamp is allowed.
     *
     * @return map of {@link LwM2mPath}-{@link LwM2mNode} or null if there is no value for asked timestamp.
     */
    public Map<LwM2mPath, LwM2mNode> getNodesAt(Instant timestamp) {
        Map<LwM2mPath, LwM2mNode> map = timestampedPathNodesMap.get(timestamp);
        if (map != null) {
            return Collections.unmodifiableMap(timestampedPathNodesMap.get(timestamp));
        }
        return null;
    }

    /**
     * Get all collected nodes as {@link LwM2mPath}-{@link LwM2mNode} map ignoring timestamp information. In case of the
     * same path conflict the most recent one is taken. Null timestamp is considered as most recent one.
     */
    public Map<LwM2mPath, LwM2mNode> getNodes() {
        Map<LwM2mPath, LwM2mNode> result = new HashMap<>();
        for (Map.Entry<Instant, Map<LwM2mPath, LwM2mNode>> entry : timestampedPathNodesMap.entrySet()) {
            result.putAll(entry.getValue());
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns the all sorted timestamps of contained nodes with ascending order. Null timestamp is considered as most
     * recent one.
     */
    public Set<Instant> getTimestamps() {
        return Collections.unmodifiableSet(timestampedPathNodesMap.keySet());
    }

    public boolean isEmpty() {
        return timestampedPathNodesMap.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("TimestampedLwM2mNodes [%s]", timestampedPathNodesMap);
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

        private static class InternalNode {
            Instant timestamp;
            LwM2mPath path;
            LwM2mNode node;

            public InternalNode(Instant timestamp, LwM2mPath path, LwM2mNode node) {
                this.timestamp = timestamp;
                this.path = path;
                this.node = node;
            }
        }

        private final List<InternalNode> nodes = new ArrayList<>();
        private boolean noDuplicate = true;

        public Builder raiseExceptionOnDuplicate(boolean raiseException) {
            noDuplicate = raiseException;
            return this;
        }

        public Builder addNodes(Map<LwM2mPath, LwM2mNode> pathNodesMap) {
            for (Entry<LwM2mPath, LwM2mNode> node : pathNodesMap.entrySet()) {
                nodes.add(new InternalNode(null, node.getKey(), node.getValue()));
            }
            return this;
        }

        public Builder addNodes(Instant timestamp, Map<LwM2mPath, LwM2mNode> pathNodesMap) {
            for (Entry<LwM2mPath, LwM2mNode> node : pathNodesMap.entrySet()) {
                nodes.add(new InternalNode(timestamp, node.getKey(), node.getValue()));
            }
            return this;
        }

        public Builder put(Instant timestamp, LwM2mPath path, LwM2mNode node) {
            nodes.add(new InternalNode(timestamp, path, node));
            return this;
        }

        public Builder put(LwM2mPath path, LwM2mNode node) {
            nodes.add(new InternalNode(null, path, node));
            return this;
        }

        public Builder add(TimestampedLwM2mNodes timestampedNodes) {
            for (Instant timestamp : timestampedNodes.getTimestamps()) {
                Map<LwM2mPath, LwM2mNode> pathNodeMap = timestampedNodes.getNodesAt(timestamp);
                for (Map.Entry<LwM2mPath, LwM2mNode> pathNodeEntry : pathNodeMap.entrySet()) {
                    nodes.add(new InternalNode(timestamp, pathNodeEntry.getKey(), pathNodeEntry.getValue()));
                }
            }
            return this;
        }

        /**
         * Build the {@link TimestampedLwM2mNodes} and raise {@link IllegalArgumentException} if builder inputs are
         * invalid.
         */
        public TimestampedLwM2mNodes build() throws IllegalArgumentException {
            Map<Instant, Map<LwM2mPath, LwM2mNode>> timestampToPathToNode = new TreeMap<>(getTimestampComparator());

            for (InternalNode internalNode : nodes) {
                // validate path is consistent with Node
                String cause = LwM2mNodeUtil.getInvalidPathForNodeCause(internalNode.node, internalNode.path);
                if (cause != null) {
                    throw new IllegalArgumentException(cause);
                }

                // add to the map
                Map<LwM2mPath, LwM2mNode> pathToNode = timestampToPathToNode.get(internalNode.timestamp);
                if (pathToNode == null) {
                    pathToNode = new HashMap<>();
                    timestampToPathToNode.put(internalNode.timestamp, pathToNode);
                    pathToNode.put(internalNode.path, internalNode.node);
                } else {
                    LwM2mNode previous = pathToNode.put(internalNode.path, internalNode.node);
                    if (noDuplicate && previous != null) {
                        throw new IllegalArgumentException(String.format(
                                "Unable to create TimestampedLwM2mNodes : duplicate value for path %s. (%s, %s)",
                                internalNode.path, internalNode.node, previous));
                    }
                }
            }
            return new TimestampedLwM2mNodes(timestampToPathToNode);
        }

        private static Comparator<Instant> getTimestampComparator() {
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
