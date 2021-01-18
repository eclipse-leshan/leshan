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
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.request.ContentFormat;

/**
 * A Decoder which should support several {@link ContentFormat} and is able to decode :
 * <ul>
 * <li>a {@link LwM2mNode}</li>
 * <li>a time-stamped {@link LwM2mNode} (e.g. for historical representations)</li>
 * <li>a list of {@link LwM2mNode} (e.g. for composite operation)</li>
 * </ul>
 */
public interface LwM2mNodeDecoder {

    /**
     * Deserializes a binary content into a {@link LwM2mNode}.
     * 
     * The type of the returned node depends on the path argument.
     *
     * @param content the content
     * @param format the content format
     * @param path the path of the node to build
     * @param model the collection of supported object models
     * @return the resulting node
     * @throws CodecException if content is malformed.
     */
    LwM2mNode decode(byte[] content, ContentFormat format, LwM2mPath path, LwM2mModel model) throws CodecException;

    /**
     * Deserializes a binary content into a {@link LwM2mNode} of the expected type.
     *
     * @param content the content
     * @param format the content format
     * @param path the path of the node to build
     * @param model the collection of supported object models
     * @param nodeClass the class of the {@link LwM2mNode} to decode
     * @return the resulting node
     * @throws CodecException if content is malformed.
     */
    <T extends LwM2mNode> T decode(byte[] content, ContentFormat format, LwM2mPath path, LwM2mModel model,
            Class<T> nodeClass) throws CodecException;

    /**
     * Deserializes a binary content into a list of {@link LwM2mNode} of the expected type.
     * <p>
     * Expected type is guess from list of {@link LwM2mPath}.
     *
     * @param content the content
     * @param format the content format
     * @param paths the list of path of node to build. The list of path can be <code>null</code> meaning that we don't
     *        know which kind of {@link LwM2mNode} is encoded. In this case, let's assume this is a list of
     *        {@link LwM2mSingleResource} or {@link LwM2mResourceInstance}.
     * @param model the collection of supported object models
     * @return the Map of {@link LwM2mPath} to decoded {@link LwM2mNode}. value can be <code>null</code> if no data was
     *         available for a given path
     * @throws CodecException if content is malformed.
     */
    Map<LwM2mPath, LwM2mNode> decodeNodes(byte[] content, ContentFormat format, List<LwM2mPath> paths, LwM2mModel model)
            throws CodecException;

    /**
     * Deserializes a binary content into a list of time-stamped {@link LwM2mNode} ordering by time-stamp.
     *
     * @param content the content
     * @param format the content format
     * @param path the path of the node to build
     * @param model the collection of supported object models
     * @return the resulting list of time-stamped {@link LwM2mNode} ordering by time-stamp
     * @exception CodecException if content is malformed.
     */
    List<TimestampedLwM2mNode> decodeTimestampedData(byte[] content, ContentFormat format, LwM2mPath path,
            LwM2mModel model) throws CodecException;

    /**
     * Deserializes a binary content into a list of {@link LwM2mPath}.
     * 
     * @param content the content to decode
     * @param format the format used to encode the content
     * @return a list of {@link LwM2mPath}
     * 
     * @throws CodecException if content is malformed.
     */
    List<LwM2mPath> decodePaths(byte[] content, ContentFormat format) throws CodecException;

    /**
     * return true is the given {@link ContentFormat} is supported
     */
    boolean isSupported(ContentFormat format);

}
