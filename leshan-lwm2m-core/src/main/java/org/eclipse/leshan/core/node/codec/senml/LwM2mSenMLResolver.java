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

import java.math.BigDecimal;

import org.eclipse.leshan.core.node.InvalidLwM2mPathException;
import org.eclipse.leshan.core.node.PrefixedLwM2mPath;
import org.eclipse.leshan.core.node.PrefixedLwM2mPathParser;
import org.eclipse.leshan.senml.SenMLException;
import org.eclipse.leshan.senml.SenMLRecord;
import org.eclipse.leshan.senml.SenMLResolver;

/**
 * A {@link SenMLResolver} dedicated for LWM2M usage as it creates {@link LwM2mResolvedSenMLRecord}.
 */
public class LwM2mSenMLResolver extends SenMLResolver<LwM2mResolvedSenMLRecord> {

    private final PrefixedLwM2mPathParser pathParser;

    public LwM2mSenMLResolver() {
        this(new PrefixedLwM2mPathParser());
    }

    public LwM2mSenMLResolver(PrefixedLwM2mPathParser pathParser) {
        this.pathParser = pathParser;
    }

    @Override
    protected LwM2mResolvedSenMLRecord createResolvedRecord(SenMLRecord unresolvedRecord, String resolvedName,
            BigDecimal resolvedTimestamp) throws SenMLException {
        try {
            PrefixedLwM2mPath path = pathParser.parsePrefixedPath(resolvedName);
            return new LwM2mResolvedSenMLRecord(unresolvedRecord, resolvedName, path, resolvedTimestamp);
        } catch (InvalidLwM2mPathException e) {
            throw new SenMLException(e, "Unable to resolve record, invalid path", resolvedName);
        }
    }
};
