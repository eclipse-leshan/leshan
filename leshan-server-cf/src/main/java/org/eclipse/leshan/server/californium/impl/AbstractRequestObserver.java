/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
package org.eclipse.leshan.server.californium.impl;

import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.response.LwM2mResponse;

// ////// Request Observer Class definition/////////////
// TODO leshan-code-cf: All Request Observer should be factorize in a leshan-core-cf project.
// duplicate from org.eclipse.leshan.server.californium.impl.CaliforniumLwM2mRequestSender
public abstract class AbstractRequestObserver<T extends LwM2mResponse> extends MessageObserverAdapter {
    Request coapRequest;

    public AbstractRequestObserver(final Request coapRequest) {
        this.coapRequest = coapRequest;
    }

    public abstract T buildResponse(Response coapResponse);
}