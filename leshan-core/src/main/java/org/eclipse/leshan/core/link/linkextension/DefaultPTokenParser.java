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

import org.eclipse.leshan.core.link.LinkParamValue;
import org.eclipse.leshan.core.link.LinkParseException;

/**
 * Validate & parse ptoken with rules (subset of RFC6690 (https://datatracker.ietf.org/doc/html/RFC6690#section-2)):
 *
 * <pre>
 * ptoken         = 1*ptokenchar
 * ptokenchar     = "!" / "#" / "$" / "%" / "{@code &}" / "'" / "("
 *                    / ")" / "*" / "+" / "-" / "." / "/" / DIGIT
 *                    / ":" / "{@code <}" / "=" / "{@code >}" / "?" / "@" / ALPHA
 *                    / "[" / "]" / "^" / "_" / "`" / "{" / "|"
 *                    / "}" / "~"
 * </pre>
 */
public class DefaultPTokenParser implements PTokenParser {

    private final Pattern ptokenPattern = Pattern.compile("[/!#$%&'()*+\\-.:<=>?@\\[\\]^_`{|}~a-zA-Z0-9]+");

    @Override
    public LinkParamValue parse(String content) throws LinkParseException {
        String errorMessage = getValidationErrorMessage(content);
        if (errorMessage != null) {
            throw new LinkParseException(errorMessage);
        } else {
            return new LinkParamValue(content);
        }
    }

    @Override
    public String getValidationErrorMessage(String content) {
        if (!isValid(content)) {
            return String.format("invalid ptoken [%s]", content);
        } else {
            return null;
        }
    }

    @Override
    public boolean isValid(String content) {
        return content != null && ptokenPattern.matcher(content).matches();
    }
}
