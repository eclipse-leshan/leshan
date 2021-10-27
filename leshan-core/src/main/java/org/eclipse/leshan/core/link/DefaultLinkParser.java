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

    private static final Pattern parnamePattern = Pattern.compile("[!#$&+\\-.^_`|~a-zA-Z0-9]+");
    private final Pattern ptokenPattern = Pattern.compile("[/!#$%&'()*+\\-.:<=>?@\\[\\]^_`{|}~a-zA-Z0-9]+");
    private final Pattern uriSegmentPattern = Pattern
            .compile("([\\-._~a-zA-Z0-9:@!$&'()*+,;=]|%[a-fA-F0-9][a-fA-F0-9])+");

    /**
     * Parse a byte arrays representation of a {@code String} encoding with UTF_8 {@link Charset} with rules (subset of
     * RFC6690 (https://datatracker.ietf.org/doc/html/RFC6690#section-2)):
     * 
     * <pre>
     * {@code
     * Link            = link-value-list
     * link-value-list = [ link-value *[ "," link-value ]]
     * }
     * </pre>
     * 
     * @param bytes a byte arrays representing {@code String} encoding with UTF_8 {@link Charset}.
     * @return an array of {@code Link}
     */
    @Override
    public Link[] parse(byte[] bytes) throws LinkParseException {
        if (bytes == null || bytes.length == 0) {
            return new Link[] {};
        }

        String content = new String(bytes, StandardCharsets.UTF_8);
        List<String> linkValueList = splitIgnoringEscaped(content, ',');

        try {
            Link[] result = new Link[linkValueList.size()];
            for (int i = 0; i < linkValueList.size(); i++) {
                String linkValue = linkValueList.get(i);
                result[i] = parseLinkValue(linkValue);
            }
            return result;

        } catch (LinkParseException e) {
            throw new LinkParseException(e, "Invalid Links [%s] : %s", content, e.getMessage());
        }
    }

    /**
     * Creates a {@link Link} from a link-value with rules (subset of RFC6690
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
    protected Link parseLinkValue(String linkValue) throws LinkParseException {
        List<String> parts = splitIgnoringEscaped(linkValue, ';');

        String uriReferenceDecorated = parts.get(0);

        validateUriReferenceDecorated(uriReferenceDecorated);

        Map<String, LinkParamValue> linkParams = new HashMap<>();

        for (int i = 1; i < parts.size(); i++) {
            parseLinkExtension(parts, linkParams, i);
        }

        return new Link(removeUriReferenceDecoration(uriReferenceDecorated), linkParams);
    }

    /**
     * Validate with rule: {@code "<" URI-Reference ">"}
     *
     * @param uriReferenceDecorated URI-Reference with extra {@code "<"} and {@code ">"} tags.
     */
    protected void validateUriReferenceDecorated(String uriReferenceDecorated) throws LinkParseException {
        if (uriReferenceDecorated.length() <= 2) {
            throw new LinkParseException("URI-Reference [%s] is too short", uriReferenceDecorated);
        }
        if (uriReferenceDecorated.charAt(0) != '<'
                || uriReferenceDecorated.charAt(uriReferenceDecorated.length() - 1) != '>') {
            throw new LinkParseException("URI-Reference [%s] should begin with \"<\" and ends with \">\"",
                    uriReferenceDecorated);
        }
        String UriReference = removeUriReferenceDecoration(uriReferenceDecorated);

        validateUriReference(UriReference);
    }

    /**
     * Parse a link-extension with rules:
     * 
     * <pre>
     * {@code
     * link-extension = ( parmname [ "=" ( ptoken / quoted-string ) ] )
     * }
     * </pre>
     */
    private void parseLinkExtension(List<String> parts, Map<String, LinkParamValue> linkParams, int i)
            throws LinkParseException {
        String linkExtension = parts.get(i);
        String[] attParts = linkExtension.split("=", 2);

        String key = attParts[0];

        try {
            validateParmname(key);

            String value = null;
            if (attParts.length > 1) {
                value = attParts[1];
                validateLinkExtensionValue(value);
            }
            linkParams.put(key, applyCharEscaping(value));

        } catch (LinkParseException e) {
            throw new LinkParseException(e, "invalid link-extension [%s] : %s", linkExtension, e.getMessage());
        }
    }

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
    protected static void validateParmname(String parmname) throws LinkParseException {
        if (!parnamePattern.matcher(parmname).matches()) {
            throw new LinkParseException("invalid parmname [%s]", parmname);
        }
    }

    /**
     * Validate value of link-extension
     * 
     * <pre>
     * {@code
     * link-extension = ( parmname [ "=" ( ptoken / quoted-string ) ] )
     * }
     * </pre>
     */
    protected void validateLinkExtensionValue(String value) throws LinkParseException {
        if (value.length() == 0) {
            throw new LinkParseException("invalid parmname value [%s] : no value passed", value);
        }

        if (value.charAt(0) == '\"') {
            validateQuotedString(value);
        } else {
            validatePtoken(value);
        }
    }

    /**
     * Validate a quoted-string with rules (subset of RFC2616
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
    protected void validateQuotedString(String value) throws LinkParseException {
        value = removeEscapedChars(value);
        if (value.charAt(value.length() - 1) != '\"') {
            throw new LinkParseException("invalid quoted-string [%s]", value);
        }
    }

    /**
     * Validate ptoken with rules (subset of RFC6690 (https://datatracker.ietf.org/doc/html/RFC6690#section-2)):
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
    protected void validatePtoken(String value) throws LinkParseException {
        if (!ptokenPattern.matcher(value).matches()) {
            throw new LinkParseException("invalid ptoken [%s]", value);
        }
    }

    /**
     * Validate URI-Reference with rules (subset of RFC3986 (https://datatracker.ietf.org/doc/html/rfc3986#appendix-A)):
     * 
     * <pre>
     * {@code
     * URI-reference = relative-ref
     *
     * relative-ref  = relative-part
     * relative-part =  path-absolute
     *
     * path-absolute = "/" [ segment-nz *( "/" segment ) ]   ; begins with "/" but not "//"
     * }
     * </pre>
     */
    private void validateUriReference(String uriReference) throws LinkParseException {
        if (uriReference.length() > 0) {
            if (uriReference.charAt(0) != '/') {
                throw new LinkParseException("URI-Reference [%s] should begins with \"/\"", uriReference);
            } else if (uriReference.length() > 1 && uriReference.charAt(1) == '/') {
                throw new LinkParseException("URI-Reference [%s] should not begins with \"//\"", uriReference);
            }

            String[] segments = uriReference.substring(1).split("/");

            try {
                for (String segment : segments) {
                    if (segment.length() > 0) {
                        validateUriSegment(segment);
                    }
                }
            } catch (LinkParseException e) {
                throw new LinkParseException(e, "invalid URI-Reference [%s] : %s", uriReference, e.getMessage());
            }
        }
    }

    /**
     * Validate a segment/segment-nz with rules (subset of RFC3986
     * (https://datatracker.ietf.org/doc/html/rfc3986#appendix-A)):
     * 
     * <pre>
     * {@code
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
    private void validateUriSegment(String segment) throws LinkParseException {
        if (!uriSegmentPattern.matcher(segment).matches()) {
            throw new LinkParseException("invalid segment: [%s]", segment);
        }
    }

    /**
     * Splits content by delimiter into multiple chunks considering that delimiter may be part of link or quoted text.
     */
    protected List<String> splitIgnoringEscaped(String content, char delimiter) {
        List<String> linkValueList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean quote = false;
        boolean uriPart = false;
        boolean escape = false;
        boolean uriPartCanBegin = true;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == '\\') {
                escape = !escape;
                sb.append(ch);
                uriPartCanBegin = false;
                continue;
            }

            if (ch == delimiter && !quote && !uriPart) {
                linkValueList.add(sb.toString());
                sb = new StringBuilder();
                escape = false;
                uriPartCanBegin = true;
                continue;
            }
            if (!escape) {
                if (!quote) {
                    if (ch == '<' && uriPartCanBegin) {
                        uriPart = true;
                    }
                    if (ch == '>') {
                        uriPart = false;
                    }
                }
                if (ch == '"') {
                    quote = !quote;
                }

            }
            sb.append(ch);
            escape = false;
            uriPartCanBegin = false;
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