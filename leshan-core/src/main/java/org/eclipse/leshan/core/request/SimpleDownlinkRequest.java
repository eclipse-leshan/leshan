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
package org.eclipse.leshan.core.request;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.response.LwM2mResponse;

/**
 * A simple {@link DownlinkRequest} which targets only 1 node.
 */
public interface SimpleDownlinkRequest<T extends LwM2mResponse> extends DownlinkRequest<T> {
    /**
     * Gets the node path targeted by the request.
     *
     * @return the request path
     */
    LwM2mPath getPath();
}
