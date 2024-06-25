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
 * For details about Certificate Usage please see:
 * <a href="https://tools.ietf.org/html/rfc6698#section-2.1.1">rfc6698#section-2.1.1</a> - The Certificate Usage Field
 */
public enum CertificateUsage {
    CA_CONSTRAINT(0), SERVICE_CERTIFICATE_CONSTRAINT(1), TRUST_ANCHOR_ASSERTION(2), DOMAIN_ISSUER_CERTIFICATE(3);

    public final ULong code;

    private CertificateUsage(int code) {
        this.code = ULong.valueOf(code);
    }

    public static CertificateUsage fromCode(int code) {
        return fromCode(ULong.valueOf(code));
    }

    public static CertificateUsage fromCode(ULong code) {
        for (CertificateUsage sm : CertificateUsage.values()) {
            if (sm.code.equals(code)) {
                return sm;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported certificate usage code : %s", code));
    }
}
