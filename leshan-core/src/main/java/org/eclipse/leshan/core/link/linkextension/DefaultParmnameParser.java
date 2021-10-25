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
 *     Orange - Make LinkParser extensible.
 *******************************************************************************/
package org.eclipse.leshan.core.link.linkextension;

import java.util.regex.Pattern;

import org.eclipse.leshan.core.link.LinkParseException;

/**
 * From RFC5987 (https://datatracker.ietf.org/doc/html/rfc5987#section-3.2.1):
 *
 * <pre>
 * {@code
 * parmname      = 1*attr-char
 * attr-char     = ALPHA / DIGIT
 *                    / "!" / "#" / "$" / "&" / "+" / "-" / "."
 *                    / "^" / "_" / "`" / "|" / "~"
 *                    ; token except ( "*" / "'" / "%" )
 * }
 * </pre>
 */
public class DefaultParmnameParser implements ParmnameParser {

    private static final Pattern parnamePattern = Pattern.compile("[\\\\!#$&+\\-.^_`|~a-zA-Z0-9]+");

    @Override
    public boolean isValid(String content) {
        return content != null && parnamePattern.matcher(content).matches();
    }

    @Override
    public String parse(String content) throws LinkParseException {
        String errorMessage = getValidationErrorMessage(content);
        if (errorMessage != null) {
            throw new LinkParseException(errorMessage);
        } else {
            return content;
        }
    }

    @Override
    public String getValidationErrorMessage(String content) {
        if (!isValid(content)) {
            return String.format("invalid parmname [%s]", content);
        } else {
            return null;
        }
    }
}
