/*******************************************************************************
 * Copyright (c) 2022 Orange.
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
 *     Orange
 *******************************************************************************/
package org.eclipse.leshan.core.node.codec;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;

/**
 * An encoder for {@link TimestampedLwM2mNodes}.
 *
 * @see DefaultLwM2mEncoder
 */
public interface TimestampedMultiNodeEncoder {
    /**
     * Serializes a {@link TimestampedLwM2mNodes} object into a byte array.
     * <p>
     *
     * @param timestampedNodes timestamped nodes to be serialized
     * @param model the collection of supported object models
     * @param converter value converter for resources
     * @return the serialized byte array
     * @throws CodecException if encoding fails
     */
    byte[] encodeTimestampedNodes(TimestampedLwM2mNodes timestampedNodes, LwM2mModel model,
            LwM2mValueConverter converter) throws CodecException;
}
