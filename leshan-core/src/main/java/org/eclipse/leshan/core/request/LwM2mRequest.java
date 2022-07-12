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
 * A Lightweight M2M request.
 */
public interface LwM2mRequest<T extends LwM2mResponse> {

    /**
     * Get the underlying CoAP request. The object type depends of the chosen CoAP implementation. (e.g with Californium
     * implementation <code>getCoapResponse()</code> will returns
     * <code>a org.eclipse.californium.core.coap.Request)</code>).
     *
     * @return the CoAP request
     */
    Object getCoapRequest();
}
