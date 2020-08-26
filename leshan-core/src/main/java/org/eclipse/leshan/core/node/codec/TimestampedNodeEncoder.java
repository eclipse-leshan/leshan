/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.core.node.codec;

import java.util.List;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;

/**
 * A {@link NodeEncoder} which can also encode time-stamped values.
 * 
 * @see DefaultLwM2mNodeEncoder
 */
public interface TimestampedNodeEncoder extends NodeEncoder {

    /**
     * Serializes a list of time-stamped {@link LwM2mNode}.
     *
     * @param timestampedNodes the list of time-stamped object/instance/resource to serialize
     * @param path the path of the node to serialize
     * @param model the collection of supported object models
     * @param converter a data type converter.
     * @return the encoded node as a byte array
     * @throws CodecException if there payload is malformed.
     */
    byte[] encodeTimestampedData(List<TimestampedLwM2mNode> timestampedNodes, LwM2mPath path, LwM2mModel model,
            LwM2mValueConverter converter) throws CodecException;
}
