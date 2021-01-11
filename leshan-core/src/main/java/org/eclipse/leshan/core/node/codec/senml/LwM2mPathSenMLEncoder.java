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

import java.util.List;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.PathEncoder;
import org.eclipse.leshan.senml.SenMLEncoder;
import org.eclipse.leshan.senml.SenMLException;
import org.eclipse.leshan.senml.SenMLPack;
import org.eclipse.leshan.senml.SenMLRecord;

/**
 * A {@link PathEncoder} for SenML format
 */
public class LwM2mPathSenMLEncoder implements PathEncoder {

    private final SenMLEncoder encoder;

    public LwM2mPathSenMLEncoder(SenMLEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public byte[] encode(List<LwM2mPath> paths) {
        // Create SenML Pack
        SenMLPack pack = new SenMLPack();
        for (LwM2mPath path : paths) {
            SenMLRecord record = new SenMLRecord();
            record.setName(path.toString());
            pack.addRecord(record);
        }

        // Encode Pack
        try {
            return encoder.toSenML(pack);
        } catch (SenMLException e) {
            throw new CodecException(e, "Unable to encode paths %s", paths);
        }
    }
}
