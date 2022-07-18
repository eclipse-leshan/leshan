/*******************************************************************************
 * Copyright (c) 2022    Sierra Wireless and others.
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
package org.eclipse.leshan.core.oscore;

import java.io.Serializable;
import java.util.Arrays;

import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.Validate;

/**
 * An OSCORE Object identifying a foreign peer.
 *
 */
public class OscoreIdentity implements Serializable {

    private static final long serialVersionUID = 1L;
    protected final byte[] recipientId;

    public OscoreIdentity(byte[] recipientId) {
        Validate.notNull(recipientId);
        if (recipientId.length == 0) {
            throw new IllegalArgumentException("recipient can not be empty");
        }
        this.recipientId = recipientId;
    }

    public byte[] getRecipientId() {
        return recipientId;
    }

    @Override
    public String toString() {
        return String.format("OscoreIdentity [%s]", Hex.encodeHexString(recipientId));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(recipientId);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OscoreIdentity other = (OscoreIdentity) obj;
        if (!Arrays.equals(recipientId, other.recipientId))
            return false;
        return true;
    }
}
