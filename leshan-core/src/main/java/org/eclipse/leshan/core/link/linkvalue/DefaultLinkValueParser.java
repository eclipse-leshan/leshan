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
package org.eclipse.leshan.core.link.linkvalue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.LinkParamValue;
import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.link.linkextension.DefaultLinkExtensionParser;
import org.eclipse.leshan.core.link.linkextension.LinkExtensionParser;

/**
 * Validate and parse a {@link Link} from a link-value with rules (subset of RFC6690
 * (https://datatracker.ietf.org/doc/html/RFC6690#section-2)):
 *
 * <pre>
 * {@code
 * link-value     = "<" URI-Reference ">" *( ";" link-param )
 * link-param = link-extension
 * link-extension = ( parmname [ "=" ( ptoken / quoted-string ) ] )
 * }
 * </pre>
 */
public class DefaultLinkValueParser implements LinkValueParser {

    private final UriReferenceParser uriReferenceParser;
    private final LinkExtensionParser linkExtensionParser;

    public DefaultLinkValueParser(UriReferenceParser uriReferenceParser, LinkExtensionParser linkExtensionParser) {
        this.uriReferenceParser = uriReferenceParser;
        this.linkExtensionParser = linkExtensionParser;
    }

    public DefaultLinkValueParser() {
        this(new DefaultUriReferenceParser(), new DefaultLinkExtensionParser());
    }

    @Override
    public boolean isValid(String content) {
        return getValidationErrorMessage(content) == null;
    }

    @Override
    public String getValidationErrorMessage(String content) {
        UriReferenceParser.ExtractionResult extractionResult = uriReferenceParser.extractUriReference(content);
        if (extractionResult.uriReference == null) {
            return String.format("invalid link-value [%s]", content);
        }

        String errorMessage = uriReferenceParser.getValidationErrorMessage(extractionResult.uriReference);
        if (errorMessage != null) {
            return buildErrorMessage(content, errorMessage);
        }

        if (extractionResult.remaining != null) {
            List<String> parts = linkExtensionParser.extractLinkExtensions(extractionResult.remaining, ';');
            for (String part : parts) {
                errorMessage = linkExtensionParser.getValidationErrorMessage(part);
                if (errorMessage != null) {
                    return buildErrorMessage(content, errorMessage);
                }
            }
        }

        return null;
    }

    private String buildErrorMessage(String content, String errorMessage) {
        return String.format("invalid link-value [%s] : %s", content, errorMessage);
    }

    @Override
    public Link parse(String content) throws LinkParseException {
        String errorMessage = getValidationErrorMessage(content);
        if (errorMessage != null) {
            throw new LinkParseException(errorMessage);
        }
        UriReferenceParser.ExtractionResult extractionResult = uriReferenceParser.extractUriReference(content);
        String uriReference = uriReferenceParser.parse(extractionResult.uriReference);

        Map<String, LinkParamValue> linkExtensions = new HashMap<>();

        if (extractionResult.remaining != null) {
            List<String> parts = linkExtensionParser.extractLinkExtensions(extractionResult.remaining, ';');

            for (String part : parts) {
                Map<String, LinkParamValue> parsedLinkExtension = linkExtensionParser.parse(part);
                linkExtensions.putAll(parsedLinkExtension);
            }
        }

        return new Link(uriReference, linkExtensions);
    }

    @Override
    public List<String> extractLinks(String content, char delimiter) {
        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);

            if (ch == delimiter) {
                if (isValid(sb.toString())) {
                    result.add(sb.toString());
                    sb = new StringBuilder();
                    continue;
                }
            }

            sb.append(ch);
        }

        result.add(sb.toString());

        return result;
    }
}
