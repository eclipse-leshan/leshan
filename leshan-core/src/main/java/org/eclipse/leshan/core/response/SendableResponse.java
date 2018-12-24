/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Response wrapper which can be notify when the response is sent
 */
public class SendableResponse<T extends LwM2mResponse> {

    private static final Logger LOG = LoggerFactory.getLogger(SendableResponse.class);

    private T response;
    private Runnable sentCallback;

    public SendableResponse(T response) {
        this(response, null);
    }

    public SendableResponse(T response, Runnable sentCallback) {
        this.response = response;
        this.sentCallback = sentCallback;
    }

    public T getResponse() {
        return response;
    }

    public void sent() {
        if (sentCallback != null)
            try {
                sentCallback.run();
            } catch (RuntimeException e) {
                LOG.error("Exception while calling the reponse sent callback", e);
            }
    }
}
