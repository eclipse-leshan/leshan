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

import java.util.Map;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;

/**
 * An encoder for a list of {@link LwM2mNode} which support only one {@link ContentFormat}.
 * 
 * @see DefaultLwM2mNodeEncoder
 */
public interface MultiNodeEncoder {

    /**
     * Serializes a list of {@link LwM2mNode} using the given content format.
     *
     * @param nodes the Map from {@link LwM2mPath} to {@link LwM2mNode} to serialize. value can be <code>null</code> if
     *        no data was available for a given path
     * @param model the collection of supported object models
     * @return the encoded nodes as a byte array
     * @throws CodecException if there payload is malformed.
     */
    byte[] encodeNodes(Map<LwM2mPath, LwM2mNode> nodes, LwM2mModel model, LwM2mValueConverter converter)
            throws CodecException;
}
