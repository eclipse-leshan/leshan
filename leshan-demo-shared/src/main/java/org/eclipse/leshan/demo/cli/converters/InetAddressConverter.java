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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.demo.cli.converters;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import picocli.CommandLine.ITypeConverter;

public class InetAddressConverter implements ITypeConverter<InetAddress> {

    @Override
    public InetAddress convert(String value) throws Exception {
        if (value == null || value.equals("*")) {
            // create a wildcard address meaning any local address.
            return new InetSocketAddress(0).getAddress();
        }
        return InetAddress.getByName(value);
    }
}
