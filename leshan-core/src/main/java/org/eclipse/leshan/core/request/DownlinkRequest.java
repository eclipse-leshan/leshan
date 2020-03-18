/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 * A Downlink Lightweight M2M request.<br>
 * This is a request sent from server to client to interact with the client resource tree.
 */
public interface DownlinkRequest<T extends LwM2mResponse> extends LwM2mRequest<T> {

    /**
     * Gets the requested resource path.
     *
     * @return the request path
     */
    LwM2mPath getPath();

    /**
     * Accept a visitor for this request.
     */
    void accept(DownlinkRequestVisitor visitor);

}
