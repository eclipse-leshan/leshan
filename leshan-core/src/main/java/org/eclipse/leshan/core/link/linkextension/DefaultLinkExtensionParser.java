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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.link.LinkParamValue;
import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.util.StringUtils;

/**
 * Parse a link-extension with rules:
 *
 * <pre>
 * {@code
 * link-extension = ( parmname [ "=" ( ptoken / quoted-string ) ] )
 * }
 * </pre>
 */
public class DefaultLinkExtensionParser implements LinkExtensionParser {

    private final ParmnameParser pnameParser;
    private final LinkExtensionValueParser valueParser;

    public DefaultLinkExtensionParser(ParmnameParser pnameParser, LinkExtensionValueParser valueParser) {
        this.pnameParser = pnameParser;
        this.valueParser = valueParser;
    }

    public DefaultLinkExtensionParser() {
        this(new DefaultParmnameParser(), new DefaultLinkExtensionValueParser());
    }

    @Override
    public boolean isValid(String content) {
        if (arePreconditionsInvalid(content)) {
            return false;
        }

        String[] parts = content.split("=", 2);
        if (parts.length == 1) {
            return pnameParser.isValid(parts[0]);
        } else {
            return pnameParser.isValid(parts[0]) && valueParser.isValid(parts[1]);
        }
    }

    @Override
    public String getValidationErrorMessage(String content) {
        if (arePreconditionsInvalid(content)) {
            return String.format("invalid link-extension [%s]", content);
        }

        String[] parts = content.split("=", 2);
        if (parts.length == 1) {
            String errorMessage = pnameParser.getValidationErrorMessage(parts[0]);
            if (errorMessage != null) {
                return buildErrorMessage(content, errorMessage);
            } else {
                return null;
            }
        } else {
            String errorMessage = pnameParser.getValidationErrorMessage(parts[0]);
            if (errorMessage != null) {
                return buildErrorMessage(content, errorMessage);
            }
            errorMessage = valueParser.getValidationErrorMessage(parts[1]);
            if (errorMessage != null) {
                return buildErrorMessage(content, errorMessage);
            }
        }

        return null;
    }

    private String buildErrorMessage(String content, String errorMessage) {
        return String.format("invalid link-extension [%s] : %s", content, errorMessage);
    }

    private boolean arePreconditionsInvalid(String content) {
        return StringUtils.isEmpty(content) || content.charAt(content.length() - 1) == '=';
    }

    @Override
    public Map<String, LinkParamValue> parse(String content) throws LinkParseException {
        String errorMessage = getValidationErrorMessage(content);
        if (errorMessage != null) {
            throw new LinkParseException(errorMessage);
        }

        String[] parts = content.split("=", 2);

        Map<String, LinkParamValue> result = new HashMap<>();

        String name = pnameParser.parse(parts[0]);
        LinkParamValue value = null;
        if (parts.length == 2) {
            value = valueParser.parse(parts[1]);
        }
        result.put(name, value);

        return result;
    }

    @Override
    public List<String> extractLinkExtensions(String content, char delimiter) {
        List<String> parts = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (i == 0 && ch == delimiter) {
                continue;
            }

            if (ch == delimiter) {
                if (isValid(sb.toString())) {
                    parts.add(sb.toString());
                    sb = new StringBuilder();
                    continue;
                }
            }

            sb.append(ch);
        }

//        if (isValid(sb.toString())) {
        parts.add(sb.toString());
//        }

        return parts;
    }
}
