/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.core.endpoint;

public class Protocol {

    public static final Protocol COAP = new Protocol("COAP", "coap");
    public static final Protocol COAPS = new Protocol("COAPS", "coaps");
    public static final Protocol COAP_TCP = new Protocol("COAP_TCP", "coap+tcp");
    public static final Protocol COAPS_TCP = new Protocol("COAPS_TCP", "coaps+tcp");

    private final String name;
    private final String uriScheme;

    public Protocol(String name, String uriScheme) {
        this.name = name;
        this.uriScheme = uriScheme;
    }

    public String getName() {
        return name;
    }

    public String getUriScheme() {
        return uriScheme;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        Protocol other = (Protocol) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return getName();
    }
}
