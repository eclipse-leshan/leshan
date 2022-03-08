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

import java.util.Map;
import java.util.Set;

/**
 * A container for path {@link LwM2mPath} - nodes {@link LwM2mNode} map with optional timestamp segregation.
 *
 * @see TimestampedLwM2mNodesImpl
 */
public interface TimestampedLwM2mNodes {

    /**
     * Get timestamp grouped map of {@link LwM2mPath}-{@link LwM2mNode} map.
     */
    Map<Long, Map<LwM2mPath, LwM2mNode>> getTimestampedNodes();

    /**
     * Get all collected nodes represented as {@link LwM2mPath}-{@link LwM2mNode} map.
     */
    Map<LwM2mPath, LwM2mNode> getNodes();

    /**
     * Get nodes for specific timestamp. Null timestamp is allowed.
     *
     * @param timestamp
     * @return map of {@link LwM2mPath}-{@link LwM2mNode} or null if there is no value for asked timestamp.
     */
    Map<LwM2mPath, LwM2mNode> getNodesForTimestamp(Long timestamp);

    /**
     * Returns the contained timestamps.
     */
    Set<Long> getTimestamps();
}
