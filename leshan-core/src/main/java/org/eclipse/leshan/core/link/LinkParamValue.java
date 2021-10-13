/*******************************************************************************
 * Copyright (c) 2021 Orange.
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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690.
 *******************************************************************************/
package org.eclipse.leshan.core.link;

public class LinkParamValue {

    private final String linkParamValue;

    public LinkParamValue(String linkParamValue) {
        if (linkParamValue == null) {
            throw new IllegalArgumentException("Link-param value can't be null");
        }
        this.linkParamValue = linkParamValue;
    }

    @Override
    public String toString() {
        return linkParamValue;
    }

    /**
     * remove quote from string, only if it begins and ends by a quote.
     *
     * @return unquoted string or the original string if there no quote to remove.
     */
    public String getUnquoted() {
        if (linkParamValue.length() >= 2 && linkParamValue.charAt(0) == '"'
                && linkParamValue.charAt(linkParamValue.length() - 1) == '"') {
            return linkParamValue.substring(1, linkParamValue.length() - 1);
        }
        return linkParamValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((linkParamValue == null) ? 0 : linkParamValue.hashCode());
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
        LinkParamValue other = (LinkParamValue) obj;
        if (linkParamValue == null) {
            if (other.linkParamValue != null)
                return false;
        } else if (!linkParamValue.equals(other.linkParamValue))
            return false;
        return true;
    }
}
