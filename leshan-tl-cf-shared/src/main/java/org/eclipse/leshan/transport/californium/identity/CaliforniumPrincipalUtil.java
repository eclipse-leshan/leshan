/*******************************************************************************
 * Copyright (c) 2026 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.californium.identity;

import java.security.Principal;

import javax.security.auth.x500.X500Principal;

import org.eclipse.californium.elements.auth.X509CertPath;

public class CaliforniumPrincipalUtil {

    private CaliforniumPrincipalUtil() {
    }

    /**
     * @return <code>true</code> if given principal can be converted with {@link #convertToX500Principal(Principal)}
     */
    public static boolean canBeConvertedToX500Principal(Principal principal) {
        return (principal instanceof X500Principal || principal instanceof X509CertPath);
    }

    /**
     * Convert given principal in {@link X500Principal} or raise an {@link IllegalArgumentException}.
     * <p>
     * {@link #canBeConvertedToX500Principal(Principal)} should be used to avoid exception raising.
     */
    public static X500Principal convertToX500Principal(Principal principal) {
        if (principal instanceof X500Principal) {
            return (X500Principal) principal;
        } else if (principal instanceof X509CertPath) {
            return ((X509CertPath) principal).getTarget().getSubjectX500Principal();
        }
        throw new IllegalArgumentException(
                String.format("%s can not be converted in X500Principal", principal.getClass().getSimpleName()));
    }

}
