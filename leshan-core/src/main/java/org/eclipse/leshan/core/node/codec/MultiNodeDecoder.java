/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
import java.util.Map;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ContentFormat;

/**
 * A decoder for a list of {@link LwM2mNode} which supports only one {@link ContentFormat}.
 * 
 * @see DefaultLwM2mNodeDecoder
 */
public interface MultiNodeDecoder {

    /**
     * Deserializes a binary content into a list of {@link LwM2mNode} of the expected type.
     * <p>
     * Expected type is guess from list of {@link LwM2mPath}.
     *
     * @param content the content
     * @param paths the list of path of node to build. The list of path can be <code>null</code> meaning that we don't
     *        know which kind of {@link LwM2mNode} is encoded. In this case, let's assume this is a list of
     *        {@link LwM2mSingleResource} or {@link LwM2mResourceInstance}.
     * @param model the collection of supported object models
     * @return the Map of {@link LwM2mPath} to decoded {@link LwM2mNode}. value can be <code>null</code> if no data was
     *         available for a given path
     * @throws CodecException if there payload is malformed.
     */
    Map<LwM2mPath, LwM2mNode> decodeNodes(byte[] content, List<LwM2mPath> paths, LwM2mModel model)
            throws CodecException;
}
