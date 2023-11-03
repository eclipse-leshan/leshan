/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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

/**
 * A child node in the LWM2M resource tree: Objects, Object instances, Resources or Resource instances.
 */
public interface LwM2mChildNode extends LwM2mNode {

    /**
     * @return the node identifier
     */
    int getId();

    /**
     * Convert node to pretty string
     *
     * @param path of this node;
     */
    String toPrettyString(LwM2mPath path);

    /**
     * Append pretty node to given {@link StringBuilder}
     *
     * @param b string builder to which node should be appended
     * @param path of this node;
     */
    StringBuilder appendPrettyNode(StringBuilder b, LwM2mPath path);
}
