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
import java.util.Map;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.request.ContentFormat;

/**
 * A LWM2M Encoder which supports several {@link ContentFormat} and is able to encode :
 * <ul>
 * <li>a {@link LwM2mNode}</li>
 * <li>a time-stamped {@link LwM2mNode} (e.g. for historical representations)</li>
 * <li>a list of {@link LwM2mNode} (e.g. for composite operation)</li>
 * </ul>
 */
public interface LwM2mNodeEncoder {

    /**
     * Serializes a {@link LwM2mNode} with the given content format.
     *
     * @param node the object/instance/resource to serialize
     * @param format the content format
     * @param path the path of the node to serialize
     * @param model the collection of supported object models
     * @return the encoded node as a byte array
     * @throws CodecException if encoding failed.
     */
    byte[] encode(LwM2mNode node, ContentFormat format, LwM2mPath path, LwM2mModel model) throws CodecException;

    /**
     * Serializes a list of {@link LwM2mNode} using the given content format.
     *
     * @param nodes the Map from {@link LwM2mPath} to {@link LwM2mNode} to serialize. value can be <code>null</code> if
     *        no data was available for a given path
     * @param format the content format
     * @param model the collection of supported object models
     * @return the encoded nodes as a byte array
     * @throws CodecException if encoding failed.
     */
    byte[] encodeNodes(Map<LwM2mPath, LwM2mNode> nodes, ContentFormat format, LwM2mModel model) throws CodecException;

    /**
     * Serializes a list of time-stamped {@link LwM2mNode} with the given content format.
     *
     * @param timestampedNodes the list of time-stamped object/instance/resource to serialize
     * @param format the content format
     * @param path the path of the node to serialize
     * @param model the collection of supported object models
     * @return the encoded node as a byte array
     * @throws CodecException if encoding failed.
     */
    byte[] encodeTimestampedData(List<TimestampedLwM2mNode> timestampedNodes, ContentFormat format, LwM2mPath path,
            LwM2mModel model) throws CodecException;

    /**
     * Serializes a list of {@link LwM2mPath} with the given content format.
     * 
     * @param paths The list of {@link LwM2mPath} to encode
     * @param format the {@link ContentFormat} used to encode
     * @return the encoded path as byte array
     * 
     * @throws CodecException if encoding failed.
     */
    byte[] encodePaths(List<LwM2mPath> paths, ContentFormat format) throws CodecException;

    /**
     * return true is the given {@link ContentFormat} is supported
     */
    boolean isSupported(ContentFormat format);
}
