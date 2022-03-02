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
package org.eclipse.leshan.core.node.codec;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;

/**
 * A decoder for {@link TimestampedLwM2mNodes}.
 *
 * @see DefaultLwM2mDecoder
 */
public interface TimestampedMultiNodeDecoder {
    /**
     * Deserializes a binary content into a {@link TimestampedLwM2mNodes}.
     * <p>
     *
     * @param content the content
     * @param model the collection of supported object models
     * @return the decoded timestamped nodes represented by {@link TimestampedLwM2mNodes}
     * @throws CodecException if content is malformed.
     */
    TimestampedLwM2mNodes decodeTimestampedNodes(byte[] content, LwM2mModel model) throws CodecException;

}
