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

import java.util.Objects;

import org.eclipse.leshan.core.util.Validate;

public class X509Identity implements LwM2mIdentity {

    private final String x509CommonName;

    public X509Identity(String x509CommonName) {
        Validate.notNull(x509CommonName);
        Validate.notEmpty(x509CommonName);
        this.x509CommonName = x509CommonName;
    }

    public String getX509CommonName() {
        return x509CommonName;
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    public String toString() {
        return String.format("Identity [x509=%s]", x509CommonName);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof X509Identity))
            return false;
        X509Identity that = (X509Identity) o;
        return Objects.equals(x509CommonName, that.x509CommonName);
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(x509CommonName);
    }
}
