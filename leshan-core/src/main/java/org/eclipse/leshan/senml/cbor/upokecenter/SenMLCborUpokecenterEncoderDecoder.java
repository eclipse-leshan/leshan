/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.senml.cbor.upokecenter;

import org.eclipse.leshan.senml.SenMLDecoder;
import org.eclipse.leshan.senml.SenMLEncoder;
import org.eclipse.leshan.senml.SenMLException;
import org.eclipse.leshan.senml.SenMLPack;

import com.upokecenter.cbor.CBORException;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;

/**
 * Helper for encoding/decoding SenML CBOR using <a href="https://github.com/peteroupc/CBOR-Java">"upokecenter"
 * CBOR-Java</a>
 */
public class SenMLCborUpokecenterEncoderDecoder implements SenMLDecoder, SenMLEncoder {
    private final SenMLCborPackSerDes serDes;

    public SenMLCborUpokecenterEncoderDecoder() {
        this(false);
    }

    /**
     * @param keepingInsertionOrder Set it to true allows to keep insertion order at serialization. This is a kind of
     *        HACK using reflection which could make testing easier but could bring performance penalty
     * 
     * @see <a href="https://github.com/peteroupc/CBOR-Java/issues/13">CBOR-Java#13 issue</a>
     */
    public SenMLCborUpokecenterEncoderDecoder(boolean keepingInsertionOrder) {
        if (keepingInsertionOrder) {
            serDes = new SenMLCborPackSerDes() {
                @Override
                CBORObject newMap() {
                    return CBORObject.NewOrderedMap();
                }
            };

        } else {
            serDes = new SenMLCborPackSerDes();
        }
    }

    @Override
    public byte[] toSenML(SenMLPack pack) throws SenMLException {
        if (pack == null)
            return null;
        return serDes.serializeToCbor(pack);
    }

    @Override
    public SenMLPack fromSenML(byte[] data) throws SenMLException {
        try {
            CBORObject cborObject = CBORObject.DecodeFromBytes(data);
            if (cborObject.getType() != CBORType.Array) {
                throw new SenMLException("Unable to parse SenML CBOR: Array expected but was %s", cborObject.getType());
            }
            return serDes.deserializeFromCbor(cborObject.getValues());
        } catch (CBORException e) {
            throw new SenMLException("Unable to parse SenML CBOR.", e);
        }
    }
}
