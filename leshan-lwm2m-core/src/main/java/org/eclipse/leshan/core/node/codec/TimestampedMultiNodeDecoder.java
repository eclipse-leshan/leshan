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

import java.util.List;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;

/**
 * A decoder for {@link TimestampedLwM2mNodes}.
 *
 * @see DefaultLwM2mDecoder
 */
public interface TimestampedMultiNodeDecoder {
    /**
     * Deserializes a binary content into a {@link TimestampedLwM2mNodes}.
     *
     * @param content the content
     * @param rootPath the expected rootPath also known as alternatePath of LWM2M client.
     * @param paths the list of path of node to build. The list of path can be <code>null</code> meaning that we don't
     *        know which kind of {@link LwM2mNode} is encoded. In this case, let's assume this is a list of
     *        {@link LwM2mSingleResource} or {@link LwM2mResourceInstance}.
     * @param model the collection of supported object models
     * @return the decoded timestamped nodes represented by {@link TimestampedLwM2mNodes}
     * @throws CodecException if content is malformed.
     */
    TimestampedLwM2mNodes decodeTimestampedNodes(byte[] content, String rootPath, List<LwM2mPath> paths,
            LwM2mModel model) throws CodecException;

}
