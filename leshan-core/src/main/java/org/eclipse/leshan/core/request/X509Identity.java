/*******************************************************************************
 * Copyright (c) 2022    Sierra Wireless and others.
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

package org.eclipse.leshan.core.request;

import java.util.Objects;

import org.eclipse.leshan.core.util.Validate;

public class X509Identity implements LwM2MIdentity {

    private final String x509CommonName;

    public X509Identity(String x509CommonName) {
        Validate.notNull(x509CommonName);
        this.x509CommonName = x509CommonName;
    }

    @Override
    public String getKeyIdentifier() {
        return null;

    }

    public String getX509CommonName() {
        return x509CommonName;
    }

    @Override
    public String toString() {
        return String.format("Identity [x509=%s]", x509CommonName);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((x509CommonName == null) ? 0 : x509CommonName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        X509Identity that = (X509Identity) o;
        return Objects.equals(x509CommonName, that.x509CommonName);
    }

}
