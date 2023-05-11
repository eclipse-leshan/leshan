/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
 *     Sierra Wireless, Orange Polska S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.peer;

import java.util.Arrays;

import org.eclipse.leshan.core.util.Validate;

public class OscoreIdentity implements LwM2mIdentity {

    private final byte[] RecipientId;

    public OscoreIdentity(byte[] recipientId) {
        Validate.notNull(recipientId);
        if (recipientId.length == 0)
            throw new IllegalArgumentException("recipient Id MUST NOT be empty");
        this.RecipientId = recipientId;
    }

    public byte[] getRecipientId() {
        return RecipientId;
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    public String toString() {
        return String.format("Identity [oscore=%s]", RecipientId);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(RecipientId);
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
        return Arrays.equals(RecipientId, other.RecipientId);
    }

}
