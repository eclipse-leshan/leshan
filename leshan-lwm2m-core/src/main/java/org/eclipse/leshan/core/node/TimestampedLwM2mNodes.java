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
import java.util.Objects;
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
    public Map<LwM2mPath, LwM2mNode> getFlattenNodes() {
        Map<LwM2mPath, LwM2mNode> result = new HashMap<>();
        for (Map.Entry<Instant, Map<LwM2mPath, LwM2mNode>> entry : timestampedPathNodesMap.entrySet()) {
            result.putAll(entry.getValue());
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Get all collected nodes as {@link LwM2mPath}-{@link LwM2mNode} map from the most recent timestamp. Null is
     * considered as most recent one.
     */
    public Map<LwM2mPath, LwM2mNode> getMostRecentNodes() {
        return timestampedPathNodesMap.values().iterator().next();
    }

    /**
     * Get the most recent timestamp and return a {@link TimestampedLwM2mNodes} containing value for this timestamp.
     * Null is considered as most recent one.
     */
    public TimestampedLwM2mNodes getMostRecentTimestampedNodes() {
        Entry<Instant, Map<LwM2mPath, LwM2mNode>> entry = timestampedPathNodesMap.entrySet().iterator().next();
        return new TimestampedLwM2mNodes(Collections.singletonMap(entry.getKey(), entry.getValue()));
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

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(List<LwM2mPath> paths) {
        return new Builder(paths);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TimestampedLwM2mNodes))
            return false;
        TimestampedLwM2mNodes that = (TimestampedLwM2mNodes) o;
        return Objects.equals(timestampedPathNodesMap, that.timestampedPathNodesMap);
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(timestampedPathNodesMap);
    }

    public static class Builder {

        /**
         * Create a Builder to create timestamped nodes for given paths.
         * <p>
         * If there is not exactly one entry for each path by timestamp for each given <code>paths</code>, then an
         * {@link IllegalArgumentException} will be raised on {@link #build()}.
         * <p>
         * e.g. if you provide "/1/0/1" and "/3/0/15" as path
         *
         * <pre>
         * {@code
         * // valid
         * t1 => {
         *   "/1/0/1"     => LwM2mResource
         *   "/3/0/15"    => LwM2mResource
         * },
         * // valid
         * t2 => {
         *   "/1/0/1"     => LwM2mResource
         *   "/3/0/15"    => null
         * },
         * // invalid  : 3/0/15 is missing
         * t3 => {
         *   "/1/0/1"     => LwM2mResource
         * },
         * // invalid : 3/0/1 should not be here
         * t4 => {
         *   "/1/0/1"     => LwM2mResource
         *   "/3/0/1"     => LwM2mResource
         *   "/3/0/15"    => LwM2mResource
         * }
         * }
         * </pre>
         *
         * @param paths list of allowed {@link LwM2mPath}
         */
        public Builder(List<LwM2mPath> paths) {
            this.paths = paths;
        }

        public Builder() {
            this.paths = null;
        }

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
        private final List<LwM2mPath> paths;

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
                if (internalNode.node != null) {
                    String cause = LwM2mNodeUtil.getInvalidPathForNodeCause(internalNode.node, internalNode.path);
                    if (cause != null) {
                        throw new IllegalArgumentException(cause);
                    }
                }

                // validate path is included in expected paths
                if (paths != null && !paths.contains(internalNode.path)) {
                    throw new IllegalArgumentException(String.format(
                            "Unable to create TimestampedLwM2mNodes : Unexpected path  %s, only %s are expected.",
                            internalNode.path, paths));
                }

                // add to the map
                Map<LwM2mPath, LwM2mNode> pathToNode = timestampToPathToNode.get(internalNode.timestamp);
                if (pathToNode == null) {
                    pathToNode = new HashMap<>();
                    timestampToPathToNode.put(internalNode.timestamp, pathToNode);
                    pathToNode.put(internalNode.path, internalNode.node);
                } else {
                    LwM2mNode previous = pathToNode.put(internalNode.path, internalNode.node);
                    // check for duplicate
                    if (noDuplicate && previous != null) {
                        throw new IllegalArgumentException(String.format(
                                "Unable to create TimestampedLwM2mNodes : duplicate value for path %s. (%s, %s)",
                                internalNode.path, internalNode.node, previous));
                    }
                }
            }

            // When paths is provided, validate there is not missing path
            if (paths != null) {
                timestampToPathToNode.forEach((timestamp, pathToNodes) -> {
                    if (!pathToNodes.keySet().containsAll(paths)) {
                        throw new IllegalArgumentException(String.format(
                                "Unable to create TimestampedLwM2mNodes : Some path are missing for timestamp %s, expected %s but get %s.",
                                timestamp, paths, pathToNodes.keySet()));
                    }
                });
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
