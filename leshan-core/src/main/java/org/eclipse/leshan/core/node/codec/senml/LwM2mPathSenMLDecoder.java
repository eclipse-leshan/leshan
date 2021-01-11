/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.core.node.codec.senml;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.PathDecoder;
import org.eclipse.leshan.senml.SenMLDecoder;
import org.eclipse.leshan.senml.SenMLException;
import org.eclipse.leshan.senml.SenMLPack;
import org.eclipse.leshan.senml.SenMLRecord;

/**
 * A {@link PathDecoder} for SenML format
 */
public class LwM2mPathSenMLDecoder implements PathDecoder {

    private final SenMLDecoder decoder;

    public LwM2mPathSenMLDecoder(SenMLDecoder decoder) {
        this.decoder = decoder;

    }

    @Override
    public List<LwM2mPath> decode(byte[] content) throws CodecException {
        // Decode SenML Pack
        SenMLPack pack;
        try {
            pack = decoder.fromSenML(content);
        } catch (SenMLException e) {
            throw new CodecException(e, "Unable to decode SenML for path decoding");
        }

        // Create Path list
        List<LwM2mPath> res = new ArrayList<>();
        LwM2mSenMLResolver resolver = new LwM2mSenMLResolver();
        for (SenMLRecord record : pack.getRecords()) {
            LwM2mResolvedSenMLRecord resolvedRecord;
            try {
                resolvedRecord = resolver.resolve(record);
                if (record.getType() != null) {
                    throw new CodecException("Invalid record for path encoding : record should not have value %s",
                            record);
                }
                if (resolvedRecord.getTimeStamp() != null) {
                    throw new CodecException(
                            "Invalid record for path encoding : record should not have time or base time value %s",
                            record);
                }
                res.add(resolvedRecord.getPath());
            } catch (SenMLException e) {
                throw new CodecException(e, "Unable to resolve record");
            }
        }
        return res;
    }
}
