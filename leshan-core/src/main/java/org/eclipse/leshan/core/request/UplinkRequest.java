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

import org.eclipse.leshan.core.response.LwM2mResponse;

/**
 * A Uplink Lightweight M2M request.<br>
 * This is a request sent from client to server.
 */
public interface UplinkRequest<T extends LwM2mResponse> extends LwM2mRequest<T> {

    /**
     * Accept a visitor for this request.
     */
    void accept(UplinkRequestVisitor visitor);
}
