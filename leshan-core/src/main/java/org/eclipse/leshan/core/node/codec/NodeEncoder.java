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
 * An encoder for {@link LwM2mNode} which support only one {@link ContentFormat}.
 * 
 * @see DefaultLwM2mNodeEncoder
 */
public interface NodeEncoder {

    /**
     * Serializes a {@link LwM2mNode}.
     *
     * @param node the object/instance/resource to serialize
     * @param path the path of the node to serialize
     * @param model the collection of supported object models
     * @param converter a data type converter.
     * @return the encoded node as a byte array
     * 
     * @throws CodecException if there payload is malformed.
     */
    byte[] encode(LwM2mNode node, LwM2mPath path, LwM2mModel model, LwM2mValueConverter converter)
            throws CodecException;
}
