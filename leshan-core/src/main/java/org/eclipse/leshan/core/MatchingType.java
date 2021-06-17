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
package org.eclipse.leshan.core;

import org.eclipse.leshan.core.util.datatype.ULong;

/**
 * The Matching Type Resource specifies how the certificate or raw public key in in the Server Public Key is presented.
 * Four values are currently defined:
 * <ul>
 * <li>0: Exact match. This is the default value and also corresponds to the functionality of LwM2M v1.0. Hence, if this
 * resource is not present then the content of the Server Public Key Resource corresponds to this value.
 * <li>1: SHA-256 hash [RFC6234]
 * <li>2: SHA-384 hash [RFC6234]
 * <li>3: SHA-512 hash [RFC6234]
 * </ul>
 */
public enum MatchingType {
    EXACT_MATCH(0), SHA256(1), SHA384(2), SHA512(3);

    public final ULong code;

    private MatchingType(int code) {
        this.code = ULong.valueOf(code);
    }

    public static MatchingType fromCode(int code) {
        return fromCode(ULong.valueOf(code));
    }

    public static MatchingType fromCode(ULong code) {
        for (MatchingType sm : MatchingType.values()) {
            if (sm.code.equals(code)) {
                return sm;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported MatchingType code : %s", code));
    }
}
