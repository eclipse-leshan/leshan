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

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;

/**
 * A decoder for {@link LwM2mNode} which support only one {@link ContentFormat}.
 * 
 * @see DefaultLwM2mNodeDecoder
 */
public interface NodeDecoder {

    /**
     * Deserializes a binary content into a {@link LwM2mNode} of the expected type.
     *
     * @param content the content
     * @param path the path of the node to build
     * @param model the collection of supported object models
     * @param nodeClass the class of the {@link LwM2mNode} to decode
     * @return the resulting node
     * 
     * @throws CodecException if there payload is malformed.
     */
    <T extends LwM2mNode> T decode(byte[] content, LwM2mPath path, LwM2mModel model, Class<T> nodeClass)
            throws CodecException;
}
