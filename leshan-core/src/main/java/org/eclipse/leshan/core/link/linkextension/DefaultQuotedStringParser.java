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

import org.eclipse.leshan.core.link.LinkParamValue;
import org.eclipse.leshan.core.link.LinkParseException;

/**
 * Validate & parse a quoted-string with rules (subset of RFC2616
 * (https://datatracker.ietf.org/doc/html/rfc2616#section-2.2)):
 *
 * <pre>
 * {@code
 * quoted-string  = ( <"> *(qdtext | quoted-pair ) <"> )
 * qdtext         = <any TEXT except <">>
 * quoted-pair    = "\" CHAR
 * }
 * </pre>
 */
public class DefaultQuotedStringParser implements QuotedStringParser {

    @Override
    public LinkParamValue parse(String content) throws LinkParseException {
        String errorMessage = getValidationErrorMessage(content);
        if (errorMessage != null) {
            throw new LinkParseException(errorMessage);
        } else {
            return applyCharEscaping(content);
        }
    }

    @Override
    public String getValidationErrorMessage(String content) {
        if (!isValid(content)) {
            return String.format("invalid quoted-string [%s]", content);
        } else {
            return null;
        }
    }

    @Override
    public boolean isValid(String content) {
        if (content == null) {
            return false;
        }
        content = removeEscapedChars(content);
        return content.length() >= 2 && content.charAt(0) == '\"' && content.charAt(content.length() - 1) == '\"';
    }

    protected LinkParamValue applyCharEscaping(String value) {
        if (value == null) {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        boolean escape = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\\') {
                escape = !escape;
                if (escape) {
                    continue;
                }
            }

            if (escape && ch > 127) {
                sb.append("\\");
            }

            sb.append(ch);
        }
        return new LinkParamValue(sb.toString());
    }

    protected String removeEscapedChars(String value) {
        StringBuffer sb = new StringBuffer();
        boolean escape = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\\') {
                escape = !escape;
                continue;
            }

            if (!escape) {
                sb.append(ch);
            } else {
                escape = false;
            }
        }
        return sb.toString();
    }
}
