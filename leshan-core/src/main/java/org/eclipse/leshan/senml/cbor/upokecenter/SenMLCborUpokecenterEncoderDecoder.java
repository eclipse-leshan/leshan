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
        this(false, false);
    }

    /**
     * Create an Encoder/Decoder for SenML-CBOR based on CBOR-JAVA.
     * 
     * SenML value is defined as mandatory in <a href="https://tools.ietf.org/html/rfc8428#section-4.2">rfc8428</a>, but
     * SenML records used with a Read-Composite operation do not contain any value field, so
     * <code>allowNoValue=true</code> can be used skip this validation.
     * 
     * @param keepingInsertionOrder Set it to <code>True</code> allows to keep insertion order at serialization. This is
     *        a kind of HACK using reflection which could make testing easier but could bring performance penalty
     * @param allowNoValue <code>True</code> to not check if there is a value for each SenML record.
     * @see <a href="https://github.com/peteroupc/CBOR-Java/issues/13">CBOR-Java#13 issue</a>
     */
    public SenMLCborUpokecenterEncoderDecoder(boolean keepingInsertionOrder, boolean allowNoValue) {
        if (keepingInsertionOrder) {
            serDes = new SenMLCborPackSerDes(allowNoValue) {
                @Override
                CBORObject newMap() {
                    return CBORObject.NewOrderedMap();
                }
            };

        } else {
            serDes = new SenMLCborPackSerDes(allowNoValue);
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
