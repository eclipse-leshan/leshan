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
package org.eclipse.leshan.client.response;

import java.util.Arrays;
import java.util.Objects;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.tlv.Tlv;
import org.eclipse.leshan.tlv.Tlv.TlvType;

public abstract class BaseLwM2mResponse implements LwM2mResponse {

    private final ResponseCode code;
    private final byte[] payload;

    public BaseLwM2mResponse(final ResponseCode code, final byte[] payload) {
        this.code = code;
        this.payload = payload;
    }

    @Override
    public ResponseCode getCode() {
        return code;
    }

    @Override
    public byte[] getResponsePayload() {
        return payload;
    }

    @Override
    public Tlv getResponsePayloadAsTlv() {
        return new Tlv(TlvType.RESOURCE_VALUE, null, payload, 0);
    }

    @Override
    public boolean isSuccess() {
        switch (code) {
        case CHANGED:
        case CONTENT:
        case CREATED:
        case DELETED:
            return true;
        case BAD_REQUEST:
        case CONFLICT:
        case METHOD_NOT_ALLOWED:
        case NOT_FOUND:
        case UNAUTHORIZED:
        default:
            return false;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof BaseLwM2mResponse)) {
            return false;
        }
        final BaseLwM2mResponse other = (BaseLwM2mResponse) o;
        return code == other.code && Arrays.equals(payload, other.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, Arrays.hashCode(payload));
    }

    @Override
    public String toString() {
        final String payloadString = (payload == null) ? "" : ", \"" + Arrays.toString(payload) + "\"";
        return "[" + getClass().getSimpleName() + ": " + code + payloadString + "]";
    }

}
