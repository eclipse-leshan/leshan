/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.demo.cli.converters;

import picocli.CommandLine.ITypeConverter;

public class StrictlyPositiveIntegerConverter implements ITypeConverter<Integer> {

    @Override
    public Integer convert(String value) throws Exception {
        Integer res = Integer.parseInt(value);

        if (res <= 0)
            throw new IllegalArgumentException(String.format("%s is not a strictly positive integer", value));

        return res;
    }
}
