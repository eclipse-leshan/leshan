package org.eclipse.leshan.core.node;

import java.util.Map;
import java.util.Set;

public interface TimestampedLwM2mNodes {
    Map<Long, Map<LwM2mPath, LwM2mNode>> getTimestampedPathNodesMap();

    Map<String, LwM2mNode> getStrPathNodesMap();

    Map<LwM2mPath, LwM2mNode> getPathNodesMap();

    LwM2mNode getFirstNode();

    Set<LwM2mPath> getPaths();

    Set<Long> getTimestamps();
}
