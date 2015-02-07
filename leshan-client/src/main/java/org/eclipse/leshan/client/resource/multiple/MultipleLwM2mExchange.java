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
package org.eclipse.leshan.client.resource.multiple;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.leshan.client.exchange.LwM2mExchange;
import org.eclipse.leshan.client.resource.TypedLwM2mExchange;
import org.eclipse.leshan.client.response.ReadResponse;
import org.eclipse.leshan.tlv.Tlv;
import org.eclipse.leshan.tlv.TlvDecoder;
import org.eclipse.leshan.tlv.TlvEncoder;
import org.eclipse.leshan.tlv.TlvException;
import org.eclipse.leshan.tlv.Tlv.TlvType;

public class MultipleLwM2mExchange extends TypedLwM2mExchange<Map<Integer, byte[]>> {

    public MultipleLwM2mExchange(final LwM2mExchange exchange) {
        super(exchange);
    }

    @Override
    public void respondContent(final Map<Integer, byte[]> value) {
        advanced().respond(ReadResponse.successMultiple(value));
    }

    @Override
    protected Map<Integer, byte[]> convertFromBytes(final byte[] value) {
        final Tlv[] tlvs;
        try {
            tlvs = TlvDecoder.decode(ByteBuffer.wrap(value));
        } catch (TlvException e) {
            throw new IllegalStateException(e);
        }
        final Map<Integer, byte[]> result = new HashMap<>();
        for (final Tlv tlv : tlvs) {
            if (tlv.getType() != TlvType.RESOURCE_INSTANCE) {
                throw new IllegalArgumentException();
            }
            result.put(tlv.getIdentifier(), tlv.getValue());
        }
        return result;

    }

    @Override
    protected byte[] convertToBytes(final Map<Integer, byte[]> value) {
        final List<Tlv> tlvs = new ArrayList<>();
        for (final Entry<Integer, byte[]> entry : new TreeMap<Integer, byte[]>(value).entrySet()) {
            tlvs.add(new Tlv(TlvType.RESOURCE_INSTANCE, null, entry.getValue(), entry.getKey()));
        }
        return TlvEncoder.encode(tlvs.toArray(new Tlv[0])).array();
    }

}
