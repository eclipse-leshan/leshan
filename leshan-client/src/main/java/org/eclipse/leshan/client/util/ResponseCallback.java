/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.util;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.leshan.client.response.Callback;
import org.eclipse.leshan.client.response.OperationResponse;

public class ResponseCallback implements Callback {

    private final AtomicBoolean called;
    private OperationResponse response;

    public ResponseCallback() {
        called = new AtomicBoolean(false);
    }

    @Override
    public void onSuccess(final OperationResponse t) {
        called.set(true);
        response = t;
    }

    @Override
    public void onFailure(final OperationResponse t) {
        called.set(true);
        response = t;
    }

    public AtomicBoolean isCalled() {
        return called;
    }

    public byte[] getResponsePayload() {
        return response.getPayload();
    }

    public ResponseCode getResponseCode() {
        return response.getResponseCode();
    }

    public boolean isSuccess() {
        return response.isSuccess();
    }

    public void reset() {
        called.set(false);
    }

    public OperationResponse getResponse() {
        return response;
    }
}