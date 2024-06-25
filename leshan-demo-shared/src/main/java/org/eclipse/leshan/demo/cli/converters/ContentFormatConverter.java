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

import java.util.Arrays;

import org.eclipse.leshan.core.request.ContentFormat;

import picocli.CommandLine.ITypeConverter;

public class ContentFormatConverter implements ITypeConverter<ContentFormat> {

    private final ContentFormat[] allowedContentFormat;

    public ContentFormatConverter() {
        this.allowedContentFormat = ContentFormat.knownContentFormat;
    }

    public ContentFormatConverter(ContentFormat... allowedContentFormats) {
        this.allowedContentFormat = allowedContentFormats;
    }

    @Override
    public ContentFormat convert(String value) throws Exception {
        // try to get format by name
        ContentFormat ct = ContentFormat.fromName(value);

        // if not found try to get format by code
        if (ct == null) {
            try {
                int code = Integer.parseInt(value);
                ct = ContentFormat.fromCode(code);
            } catch (NumberFormatException e) {
                // we do nothing more if value is not a integer, means user probably try to get the content format by
                // name
            }
        }

        if (ct == null) {
            throw new IllegalArgumentException(
                    String.format("%s is not a known content format name. Allowed Content Format are %s.", value,
                            Arrays.toString(allowedContentFormat)));
        }

        if (!Arrays.asList(allowedContentFormat).contains(ct)) {
            throw new IllegalArgumentException(
                    String.format("%s is not allowed for this operation. Allowed content format are %s.", ct,
                            Arrays.toString(allowedContentFormat)));
        }

        return ct;
    }

}
