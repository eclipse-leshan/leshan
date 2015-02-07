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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.tlv.Tlv;
import org.eclipse.leshan.tlv.TlvDecoder;
import org.eclipse.leshan.tlv.TlvEncoder;
import org.eclipse.leshan.tlv.TlvException;
import org.eclipse.leshan.tlv.Tlv.TlvType;

public class ReadResponse extends BaseLwM2mResponse {

    private ReadResponse(final ResponseCode code, final byte[] payload) {
        super(code, payload);
    }

    private ReadResponse(final ResponseCode code) {
        this(code, new byte[0]);
    }

    public static ReadResponse success(final byte[] readValue) {
        return new ReadResponse(ResponseCode.CONTENT, readValue);
    }

    public static ReadResponse successMultiple(final Map<Integer, byte[]> readValues) {
        return new MultipleReadResponse(ResponseCode.CONTENT, readValues);
    }

    // TODO Evaluate whether this needs to be used
    public static ReadResponse failure() {
        return new ReadResponse(ResponseCode.METHOD_NOT_ALLOWED);
    }

    public static ReadResponse notAllowed() {
        return new ReadResponse(ResponseCode.METHOD_NOT_ALLOWED);
    }

    private static class MultipleReadResponse extends ReadResponse {

        private final Tlv tlvPayload;

        public MultipleReadResponse(final ResponseCode code, final Map<Integer, byte[]> readValues) {
            super(code, getPayload(readValues));
            try {
                tlvPayload = new Tlv(TlvType.MULTIPLE_RESOURCE,
                        TlvDecoder.decode(ByteBuffer.wrap(getResponsePayload())), null, 0);
            } catch (TlvException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Tlv getResponsePayloadAsTlv() {
            return tlvPayload;
        }

    }

    private static byte[] getPayload(final Map<Integer, byte[]> readValues) {
        final List<Tlv> children = new ArrayList<Tlv>();
        for (final Entry<Integer, byte[]> entry : new TreeMap<>(readValues).entrySet()) {
            children.add(new Tlv(TlvType.RESOURCE_INSTANCE, null, entry.getValue(), entry.getKey()));
        }
        return TlvEncoder.encode(children.toArray(new Tlv[0])).array();
    }

}
