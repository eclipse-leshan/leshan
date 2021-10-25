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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690.
 *     Orange - Make LinkParser extensible.
 *******************************************************************************/
package org.eclipse.leshan.core.link;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.leshan.core.link.linkvalue.DefaultLinkValueParser;
import org.eclipse.leshan.core.link.linkvalue.LinkValueParser;
import org.eclipse.leshan.core.util.StringUtils;

/**
 * Validate and parse a CoRE links {@link Link} with rules (subset of RFC6690
 * (https://datatracker.ietf.org/doc/html/RFC6690#section-2)):
 *
 * <pre>
 * {@code
 * Link            = link-value-list
 * link-value-list = [ link-value *[ "," link-value ]]
 * }
 * </pre>
 */
public class DefaultLinkParser implements LinkParser {

    private final LinkValueParser linkValueParser;

    public DefaultLinkParser(LinkValueParser linkValueParser) {
        this.linkValueParser = linkValueParser;
    }

    public DefaultLinkParser() {
        this(new DefaultLinkValueParser());
    }

    /**
     * Validate a byte array representation of a {@code String} encoding with UTF_8 {@link Charset}
     *
     * @param bytes a byte array representing {@code String} encoding with UTF_8 {@link Charset}.
     */
    @Override
    public boolean isValid(byte[] bytes) {
        return isValid(getContent(bytes));
    }

    /**
     * Parse a byte array representation of a {@code String} encoding with UTF_8 {@link Charset}
     *
     * @param bytes a byte array representing {@code String} encoding with UTF_8 {@link Charset}.
     */
    @Override
    public Link[] parse(byte[] bytes) throws LinkParseException {
        return parse(getContent(bytes));
    }

    private String getContent(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public boolean isValid(String content) {
        return getValidationErrorMessage(content) == null;
    }

    @Override
    public String getValidationErrorMessage(String content) {
        if (StringUtils.isEmpty(content)) {
            return null;
        }

        List<String> linkContents = linkValueParser.extractLinks(content, ',');
        if (linkContents.size() == 0) {
            return String.format("invalid links [%s]", content);
        }

        for (String linkContent : linkContents) {
            String errorMessage = linkValueParser.getValidationErrorMessage(linkContent);
            if (errorMessage != null) {
                return buildErrorMessage(content, errorMessage);
            }
        }

        return null;
    }

    private String buildErrorMessage(String content, String errorMessage) {
        return String.format("invalid links [%s] : %s", content, errorMessage);
    }

    @Override
    public Link[] parse(String content) throws LinkParseException {
        if (StringUtils.isEmpty(content)) {
            return new Link[] {};
        }

        String errorMessage = getValidationErrorMessage(content);
        if (errorMessage != null) {
            throw new LinkParseException(errorMessage);
        }

        List<String> linkContents = linkValueParser.extractLinks(content, ',');

        Link[] links = new Link[linkContents.size()];
        for (int i = 0; i < linkContents.size(); i++) {
            String linkContent = linkContents.get(i);
            links[i] = linkValueParser.parse(linkContent);
        }

        return links;
    }
}
