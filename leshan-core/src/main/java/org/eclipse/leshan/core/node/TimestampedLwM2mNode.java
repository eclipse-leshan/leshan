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

import org.eclipse.leshan.core.util.Validate;

public class TimestampedLwM2mNode {

    private final Long timestamp;

    private final LwM2mNode node;

    public TimestampedLwM2mNode(Long timestamp, LwM2mNode node) {
        Validate.notNull(node);
        this.timestamp = timestamp;
        this.node = node;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public LwM2mNode getNode() {
        return node;
    }

    public boolean isTimestamped() {
        return timestamp != null && timestamp >= 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((node == null) ? 0 : node.hashCode());
        result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
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
        TimestampedLwM2mNode other = (TimestampedLwM2mNode) obj;
        if (node == null) {
            if (other.node != null)
                return false;
        } else if (!node.equals(other.node))
            return false;
        if (timestamp == null) {
            if (other.timestamp != null)
                return false;
        } else if (!timestamp.equals(other.timestamp))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("TimestampedLwM2mNode [timestamp=%s, node=%s]", timestamp, node);
    }
}
