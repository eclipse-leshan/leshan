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
 *******************************************************************************/
package org.eclipse.leshan.core.link;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.leshan.core.util.StringUtils;

/**
 * A Link as defined in http://tools.ietf.org/html/rfc6690.
 */
public class DefaultLinkParser implements LinkParser {

    /**
     * Parse a byte arrays representation of a {@code String} encoding with UTF_8 {@link Charset} with rules (subset of
     * RFC6690 (https://datatracker.ietf.org/doc/html/RFC6690#section-2):
     *
     * Link            = link-value-list
     * link-value-list = [ link-value *[ "," link-value ]]
     *
     * @param bytes a byte arrays representing {@code String} encoding with UTF_8 {@link Charset}.
     * @return an array of {@code Link}
     */
    @Override
    public Link[] parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new Link[] {};
        }

        List<String> linkValueList = splitIgnoringEscaped(new String(bytes, StandardCharsets.UTF_8), ',');

        Link[] result = new Link[linkValueList.size()];
        for (int i = 0; i < linkValueList.size(); i++) {
            String linkValue = linkValueList.get(i);
            result[i] = parseLinkValue(linkValue);
        }

        return result;
    }

    /**
     * Creates a (@link Link} from a link-value with rules:
     *
     * link-value     = "<" URI-Reference ">" *( ";" link-param )
     * link-param = link-extension
     * link-extension = ( parmname [ "=" ( ptoken / quoted-string ) ] )
     */
    protected Link parseLinkValue(String linkValue) {
        List<String> parts = splitIgnoringEscaped(linkValue, ';');

        String uriReferenceDecorated = parts.get(0);

        validateUriReferenceDecorated(uriReferenceDecorated);

        Map<String, LinkParamValue> linkParams = new HashMap<>();

        if (parts.size() > 1) {
            for (int i = 1; i < parts.size(); i++) {
                parseLinkExtension(parts, linkParams, i);
            }
        }

        return new Link(removeUriReferenceDecoration(uriReferenceDecorated), linkParams);
    }

    /**
     * Validate with rule: "<" URI-Reference ">"
     *
     * @param uriReferenceDecorated
     */
    protected void validateUriReferenceDecorated(String uriReferenceDecorated) {
        if (uriReferenceDecorated.length() <= 2) {
            throw new IllegalArgumentException("Invalid URI-Reference");
        }
        if (uriReferenceDecorated.charAt(0) != '<'
                || uriReferenceDecorated.charAt(uriReferenceDecorated.length() - 1) != '>') {
            throw new IllegalArgumentException("Invalid URI-Reference");
        }
        String UriReference = removeUriReferenceDecoration(uriReferenceDecorated);

        validateUriReference(UriReference);
    }

    /**
     * Parse a link-extension with rules:
     *
     * link-extension = ( parmname [ "=" ( ptoken / quoted-string ) ] )
     * ptoken         = 1*ptokenchar
     * ptokenchar     = "!" / "#" / "$" / "%" / "&" / "'" / "("
     *                    / ")" / "*" / "+" / "-" / "." / "/" / DIGIT
     *                    / ":" / "<" / "=" / ">" / "?" / "@" / ALPHA
     *                    / "[" / "]" / "^" / "_" / "`" / "{" / "|"
     *                    / "}" / "~"
     */
    private void parseLinkExtension(List<String> parts, Map<String, LinkParamValue> linkParams, int i) {
        String[] attParts = parts.get(i).split("=", 2);

        if (attParts.length > 0) {
            String key = attParts[0];

            validateParmname(key);

            String value = null;
            if (attParts.length > 1) {
                value = attParts[1];
                validateLinkExtensionValue(value);
            }
            linkParams.put(key, applyCharEscaping(value));
        }
    }

    /**
     * From RFC5987 (https://datatracker.ietf.org/doc/html/rfc5987#section-3.2.1):
     *
     * parmname      = 1*attr-char
     * attr-char     = ALPHA / DIGIT
     *                    / "!" / "#" / "$" / "&" / "+" / "-" / "."
     *                    / "^" / "_" / "`" / "|" / "~"
     *                    ; token except ( "*" / "'" / "%" )
     *
     * @param parmname
     */
    protected static void validateParmname(String parmname) {
        Pattern pattern = Pattern.compile("[!#$&+\\-.^_`|~a-zA-Z0-9]+");
        if (!pattern.matcher(parmname).matches()) {
            throw new IllegalArgumentException("Invalid link-extension");
        }
    }

    /**
     * Validate value of link-extension
     *
     * link-extension = ( parmname [ "=" ( ptoken / quoted-string ) ] )
     */
    protected void validateLinkExtensionValue(String value) {
        if (value.length() == 0) {
            throw new IllegalArgumentException("Invalid link-extension");
        }

        if (value.charAt(0) == '\"') {
            validateQuotedString(value);
        } else {
            validatePtoken(value);
        }
    }

    /**
     * Validate a quoted-string with rules (subset of RFC2616
     * (https://datatracker.ietf.org/doc/html/rfc2616#section-2.2):
     *
     * quoted-string  = ( <"> *(qdtext | quoted-pair ) <"> )
     * qdtext         = <any TEXT except <">>
     * quoted-pair    = "\" CHAR
     *
     * @param value
     */
    protected void validateQuotedString(String value) {
        value = removeEscapedChars(value);
        if (value.charAt(value.length() - 1) != '\"') {
            throw new IllegalArgumentException("Invalid link-extension");
        }
    }

    /**
     * Validate ptoken with rules:
     *
     * ptoken         = 1*ptokenchar
     * ptokenchar     = "!" / "#" / "$" / "%" / "&" / "'" / "("
     *                    / ")" / "*" / "+" / "-" / "." / "/" / DIGIT
     *                    / ":" / "<" / "=" / ">" / "?" / "@" / ALPHA
     *                    / "[" / "]" / "^" / "_" / "`" / "{" / "|"
     *                    / "}" / "~"
     *
     * @param value
     */
    protected void validatePtoken(String value) {
        Pattern pattern = Pattern.compile("[!#$%&'()*+\\-.:<=>?@\\[\\]^_`{|}~a-zA-Z0-9]+");
        if (!pattern.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid link-extension");
        }
    }

    /**
     * Validate URI-Reference with rules (subset of RFC3986 https://datatracker.ietf.org/doc/html/rfc3986#appendix-A):
     *
     * URI-reference = relative-ref
     *
     * relative-ref  = relative-part
     * relative-part =  path-absolute
     *
     * path-absolute = "/" [ segment-nz *( "/" segment ) ]   ; begins with "/" but not "//"
     *
     * @param uriReference
     */
    private void validateUriReference(String uriReference) {
        if (uriReference.length() > 0) {
            if (uriReference.charAt(0) != '/' || (uriReference.length() > 1 && uriReference.charAt(1) == '/')) {
                throw new IllegalArgumentException("Invalid URI-reference");
            }

            String[] segments = uriReference.substring(1).split("/");

            for (String segment : segments) {
                if (segment.length() > 0) {
                    validateUriSegment(segment);
                }
            }
        }
    }

    /**
     * Validate a segment/segment-nz with rules:
     *
     * segment       = *pchar
     * segment-nz    = 1*pchar
     *
     * pchar         = unreserved / pct-encoded / sub-delims / ":" / "@"
     * pct-encoded   = "%" HEXDIG HEXDIG
     * unreserved    = ALPHA / DIGIT / "-" / "." / "_" / "~"
     * sub-delims    = "!" / "$" / "&" / "'" / "(" / ")"
     *              / "*" / "+" / "," / ";" / "="
     *
     * @param segment
     */
    private void validateUriSegment(String segment) {
        Pattern pattern = Pattern.compile("([\\-._~a-zA-Z0-9:@!$&'()*+,;=]|%[a-fA-F0-9][a-fA-F0-9])+");
        if (!pattern.matcher(segment).matches()) {
            throw new IllegalArgumentException("Invalid URI-reference");
        }
    }

    /**
     * Splits content by delimiter into multiple chunks considering that delimiter may be part of link or quoted text.
     */
    protected List<String> splitIgnoringEscaped(String content, char delimiter) {
        List<String> linkValueList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean quote = false;
        boolean parname = false;
        boolean escape = false;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == '\\') {
                escape = !escape;
                sb.append(ch);
                continue;
            }

            if (ch == delimiter && !quote && !parname) {
                linkValueList.add(sb.toString());
                sb = new StringBuilder();
                escape = false;
                continue;
            }
            if (!escape) {
                if (!quote) {
                    if (ch == '<') {
                        parname = true;
                    }
                    if (ch == '>') {
                        parname = false;
                    }
                }
                if (ch == '"') {
                    quote = !quote;
                }

            }
            sb.append(ch);
            escape = false;
        }

        linkValueList.add(sb.toString());

        return linkValueList;
    }

    protected String removeUriReferenceDecoration(String uriReferenceDecorated) {
        return StringUtils.removeStart(StringUtils.removeEnd(uriReferenceDecorated, ">"), "<");
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