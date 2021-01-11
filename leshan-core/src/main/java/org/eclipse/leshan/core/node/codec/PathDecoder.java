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

import org.eclipse.leshan.core.node.LwM2mPath;

/**
 * a {@link LwM2mPath} decoder.
 * 
 * @see DefaultLwM2mNodeDecoder
 */
public interface PathDecoder {

    /**
     * @param content The content to decode
     * @return The list of {@link LwM2mPath}
     */
    List<LwM2mPath> decode(byte[] content);
}
