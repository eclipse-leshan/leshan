/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.node;

import java.time.Instant;
import java.util.Objects;

import org.eclipse.leshan.core.util.Validate;

public class TimestampedLwM2mNode {

    private final Instant timestamp;

    private final LwM2mNode node;

    public TimestampedLwM2mNode(Instant timestamp, LwM2mNode node) {
        Validate.notNull(node);
        this.timestamp = timestamp;
        this.node = node;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public LwM2mNode getNode() {
        return node;
    }

    public boolean isTimestamped() {
        return timestamp != null && timestamp.isAfter(Instant.EPOCH);
    }

    @Override
    public String toString() {
        return String.format("TimestampedLwM2mNode [timestamp=%s, node=%s]", timestamp, node);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TimestampedLwM2mNode))
            return false;
        TimestampedLwM2mNode that = (TimestampedLwM2mNode) o;
        return Objects.equals(timestamp, that.timestamp) && Objects.equals(node, that.node);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(timestamp, node);
    }
}
