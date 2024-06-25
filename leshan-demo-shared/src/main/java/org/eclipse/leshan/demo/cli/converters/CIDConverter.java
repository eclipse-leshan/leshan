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
package org.eclipse.leshan.core.demo.cli.converters;

import picocli.CommandLine.ITypeConverter;

public class CIDConverter implements ITypeConverter<Integer> {

    private Integer onValue;

    public CIDConverter(Integer onValue) {
        this.onValue = onValue;
    }

    @Override
    public Integer convert(String cid) {
        if ("off".equals(cid)) {
            return null;
        } else if ("on".equals(cid)) {
            return onValue;
        } else {
            Integer res = Integer.parseInt(cid);
            return res < 0 ? null : res;
        }
    }
};
