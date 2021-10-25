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
 * Validate & parse value of link-extension
 *
 * <pre>
 * {@code
 * link-extension = ( parmname [ "=" ( ptoken / quoted-string ) ] )
 * }
 * </pre>
 */
public class DefaultLinkExtensionValueParser implements LinkExtensionValueParser {

    private final PTokenParser ptokenParser;
    private final QuotedStringParser quotedStringParser;

    public DefaultLinkExtensionValueParser(PTokenParser ptokenParser, QuotedStringParser quotedStringParser) {
        this.ptokenParser = ptokenParser;
        this.quotedStringParser = quotedStringParser;
    }

    public DefaultLinkExtensionValueParser() {
        this(new DefaultPTokenParser(), new DefaultQuotedStringParser());
    }

    @Override
    public boolean isValid(String content) {
        return ptokenParser.isValid(content) || quotedStringParser.isValid(content);
    }

    @Override
    public LinkParamValue parse(String content) throws LinkParseException {
        String errorMessage = getValidationErrorMessage(content);
        if (errorMessage != null) {
            throw new LinkParseException(errorMessage);
        }

        if (ptokenParser.isValid(content)) {
            return ptokenParser.parse(content);
        } else if (quotedStringParser.isValid(content)) {
            return quotedStringParser.parse(content);
        } else {
            return null;
        }
    }

    @Override
    public String getValidationErrorMessage(String content) {
        if (!isValid(content)) {
            return String.format("invalid link-extension value [%s]", content);
        } else {
            return null;
        }
    }
}
