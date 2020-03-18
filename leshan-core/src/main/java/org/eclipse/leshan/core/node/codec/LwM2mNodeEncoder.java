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
package org.eclipse.leshan.core.node.codec;

import java.util.List;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.request.ContentFormat;

public interface LwM2mNodeEncoder {

    /**
     * Serializes a {@link LwM2mNode} with the given content format.
     *
     * @param node the object/instance/resource to serialize
     * @param format the content format
     * @param path the path of the node to serialize
     * @param model the collection of supported object models
     * @return the encoded node as a byte array
     * @throws CodecException if there payload is malformed.
     */
    byte[] encode(LwM2mNode node, ContentFormat format, LwM2mPath path, LwM2mModel model)
            throws CodecException;

    /**
     * Serializes a list of time-stamped {@link LwM2mNode} with the given content format.
     *
     * @param timestampedNodes the list of time-stamped object/instance/resource to serialize
     * @param format the content format
     * @param path the path of the node to serialize
     * @param model the collection of supported object models
     * @return the encoded node as a byte array
     * @throws CodecException if there payload is malformed.
     */
    byte[] encodeTimestampedData(List<TimestampedLwM2mNode> timestampedNodes, ContentFormat format, LwM2mPath path,
            LwM2mModel model) throws CodecException;

    /**
     * return true is the given {@link ContentFormat} is supported
     */
    boolean isSupported(ContentFormat format);
}
