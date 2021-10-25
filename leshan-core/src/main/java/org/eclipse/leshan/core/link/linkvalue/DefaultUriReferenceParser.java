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

import java.util.regex.Pattern;

import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.util.StringUtils;

/**
 * Validate and parse decorated URI-Reference {@code "<" URI-Reference ">"} with rules (subset of RFC3986
 * (https://datatracker.ietf.org/doc/html/rfc3986#appendix-A)):
 *
 * <pre>
 * {@code
 * URI-reference = relative-ref
 *
 * relative-ref  = relative-part
 * relative-part =  path-absolute
 *
 * path-absolute = "/" [ segment-nz *( "/" segment ) ]   ; begins with "/" but not "//"
 * segment       = *pchar
 * segment-nz    = 1*pchar
 *
 * pchar         = unreserved / pct-encoded / sub-delims / ":" / "@"
 * pct-encoded   = "%" HEXDIG HEXDIG
 * unreserved    = ALPHA / DIGIT / "-" / "." / "_" / "~"
 * sub-delims    = "!" / "$" / "&" / "'" / "(" / ")"
 *              / "*" / "+" / "," / ";" / "="
 * }
 * </pre>
 */
public class DefaultUriReferenceParser implements UriReferenceParser {

    private final Pattern uriSegmentPattern = Pattern
            .compile("([\\-._~a-zA-Z0-9:@!$&'()*+,;=]|%[a-fA-F0-9][a-fA-F0-9])+");

    @Override
    public boolean isValid(String content) {
        return getValidationErrorMessage(content) == null;
    }

    @Override
    public String getValidationErrorMessage(String uriReferenceDecorated) {
        if (uriReferenceDecorated.length() <= 2) {
            return String.format("URI-Reference [%s] is too short", uriReferenceDecorated);
        }
        if (uriReferenceDecorated.charAt(0) != '<'
                || uriReferenceDecorated.charAt(uriReferenceDecorated.length() - 1) != '>') {
            return String.format("URI-Reference [%s] should begin with \"<\" and ends with \">\"",
                    uriReferenceDecorated);
        }
        String UriReference = removeUriReferenceDecoration(uriReferenceDecorated);

        return validateUriReference(UriReference);
    }

    private String validateUriReference(String uriReference) {
        if (uriReference.length() > 0) {
            if (uriReference.charAt(0) != '/') {
                return String.format("URI-Reference [%s] should begins with \"/\"", uriReference);
            } else if (uriReference.length() > 1 && uriReference.charAt(1) == '/') {
                return String.format("URI-Reference [%s] should not begins with \"//\"", uriReference);
            }

            String[] segments = uriReference.substring(1).split("/");

            for (String segment : segments) {
                if (segment.length() > 0) {
                    String errorMessage = validateUriSegment(segment);
                    if (errorMessage != null) {
                        return String.format("invalid URI-Reference [%s] : %s", uriReference, errorMessage);
                    }
                }
            }
        }
        return null;
    }

    private String validateUriSegment(String segment) {
        if (!uriSegmentPattern.matcher(segment).matches()) {
            return String.format("invalid segment: [%s]", segment);
        }
        return null;
    }

    @Override
    public String parse(String content) throws LinkParseException {
        String errorMessage = getValidationErrorMessage(content);
        if (errorMessage != null) {
            throw new LinkParseException(errorMessage);
        }
        return removeUriReferenceDecoration(content);
    }

    protected String removeUriReferenceDecoration(String uriReferenceDecorated) {
        return StringUtils.removeStart(StringUtils.removeEnd(uriReferenceDecorated, ">"), "<");
    }

    @Override
    public ExtractionResult extractUriReference(String content) {
        ExtractionResult result = new ExtractionResult();
        StringBuilder sb = new StringBuilder();
        boolean uriPartCompleted = false;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);

            sb.append(ch);

            if (i == 0 && ch != '<') {
                break;
            }

            if (!uriPartCompleted && ch == '>') {
                result.uriReference = sb.toString();
                sb = new StringBuilder();
                uriPartCompleted = true;
            }
        }

        if (sb.length() > 0) {
            result.remaining = sb.toString();
        }

        return result;
    }

}
