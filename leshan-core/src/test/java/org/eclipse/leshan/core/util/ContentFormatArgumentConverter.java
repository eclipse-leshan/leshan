/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
package org.eclipse.leshan.core.util;

import org.eclipse.leshan.core.request.ContentFormat;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.SimpleArgumentConverter;

public class ContentFormatArgumentConverter extends SimpleArgumentConverter {
    @Override
    protected Object convert(Object source, Class<?> targetType) throws ArgumentConversionException {
        if (targetType.equals(ContentFormat.class)) {
            return ContentFormat.fromName((String) source);
        }
        return null;
    }
}
