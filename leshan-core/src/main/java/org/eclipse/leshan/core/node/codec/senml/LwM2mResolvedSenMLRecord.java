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

import org.eclipse.leshan.core.node.LwM2mNodeException;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.senml.ResolvedSenMLRecord;
import org.eclipse.leshan.senml.SenMLRecord;

/**
 * A {@link ResolvedSenMLRecord} dedicated for LWM2M as a new {@link #getPath()} is available to get the
 * {@link LwM2mPath} of this resolved SenML record.
 *
 */
public class LwM2mResolvedSenMLRecord extends ResolvedSenMLRecord {

    private LwM2mPath path;

    /**
     * @throws IllegalArgumentException if path is invalid
     * @throws LwM2mNodeException if path is invalid
     */
    public LwM2mResolvedSenMLRecord(SenMLRecord unresolvedRecord, String resolvedName, Long resolvedTimestamp)
            throws IllegalArgumentException, LwM2mNodeException {
        super(unresolvedRecord, resolvedName, resolvedTimestamp);
        this.path = new LwM2mPath(resolvedName);
    }

    /**
     * @return the resolved {@link LwM2mPath} of this record created from resolved record name.
     */
    public LwM2mPath getPath() {
        return path;
    }

}