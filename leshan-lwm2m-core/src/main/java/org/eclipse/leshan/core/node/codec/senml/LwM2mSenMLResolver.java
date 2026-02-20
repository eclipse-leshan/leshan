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
import java.util.Collections;
import java.util.List;

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
    private final List<String> rootpath;
    private final boolean removeRootPath;

    public LwM2mSenMLResolver() {
        this(new PrefixedLwM2mPathParser());
    }

    public LwM2mSenMLResolver(PrefixedLwM2mPathParser pathParser) {
        this(pathParser, null, false);
    }

    public LwM2mSenMLResolver(String rootpath, boolean removeRootPath) {
        this(new PrefixedLwM2mPathParser(), null, false);
    }

    /**
     * @param pathParser parser for prefixed LWM2M Path
     * @param rootpath each resolved record should start by given path (can be null) else a SenMLException is raised
     * @param removeRootPath if true rootpath to {@link LwM2mResolvedSenMLRecord} path.
     */
    public LwM2mSenMLResolver(PrefixedLwM2mPathParser pathParser, String rootpath, boolean removeRootPath) {
        this.pathParser = pathParser;
        this.rootpath = rootpath == null ? Collections.emptyList() : pathParser.parsePrefix(rootpath);
        this.removeRootPath = removeRootPath;
    }

    @Override
    protected LwM2mResolvedSenMLRecord createResolvedRecord(SenMLRecord unresolvedRecord, String resolvedName,
            BigDecimal resolvedTimestamp) throws SenMLException {
        try {
            PrefixedLwM2mPath path = pathParser.parsePrefixedPath(resolvedName);
            if (removeRootPath) {
                path = path.removePrefix(rootpath);
            } else {
                path.validateStartWith(rootpath);
            }
            return new LwM2mResolvedSenMLRecord(unresolvedRecord, resolvedName, path, resolvedTimestamp);
        } catch (InvalidLwM2mPathException e) {
            throw new SenMLException(e, "Unable to resolve record, invalid path", resolvedName);
        }
    }
};
