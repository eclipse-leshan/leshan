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
import java.util.List;

import org.eclipse.leshan.core.link.attributes.Attribute;
import org.eclipse.leshan.core.link.attributes.AttributeParser;
import org.eclipse.leshan.core.link.attributes.DefaultAttributeParser;
import org.eclipse.leshan.core.link.attributes.InvalidAttributeException;
import org.eclipse.leshan.core.parser.StringParser;

/**
 * A Link as defined in http://tools.ietf.org/html/rfc6690.
 */
public class DefaultLinkParser implements LinkParser {

    private AttributeParser attributeParser;

    public DefaultLinkParser() {
        this(new DefaultAttributeParser());
    }

    public DefaultLinkParser(AttributeParser attributeParser) {
        this.attributeParser = attributeParser;
    }

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
    public Link[] parseCoreLinkFormat(byte[] bytes) throws LinkParseException {
        // manage null/empty case
        if (bytes == null || bytes.length == 0) {
            return new Link[] {};
        }

        // convert input to String
        String strLinks = new String(bytes, StandardCharsets.UTF_8);

        // create a String Parser
        StringParser<LinkParseException> parser = new StringParser<LinkParseException>(strLinks) {
            @Override
            public void raiseException(String message, Exception cause) throws LinkParseException {
                throw new LinkParseException(message, cause);
            }
        };

        // Parse link-value-list
        List<Link> links = new ArrayList<>();
        while (true) {
            // consume link-value
            Link link = consumeLinkValue(parser);
            links.add(link);

            // no more link value we finished
            if (!parser.hasMoreChar()) {
                break;
            }

            // consume separator ','
            parser.consumeChar(',');
        }
        return links.toArray(new Link[links.size()]);
    }

    /**
     * consume a link-value with rules (subset of RFC6690 (https://datatracker.ietf.org/doc/html/RFC6690#section-2)):
     *
     * <pre>
     * {@code
     * link-value     = "<" URI-Reference ">" *( ";" link-param )
     * }
     * </pre>
     */
    protected Link consumeLinkValue(StringParser<LinkParseException> parser) throws LinkParseException {

        // consume URI-Reference
        parser.consumeChar('<');
        String uriReference = consumeUriReference(parser);
        parser.consumeChar('>');

        // consume Attribute
        List<Attribute> attrs = new ArrayList<>();
        while (parser.nextCharIs(';')) {
            parser.consumeNextChar();
            Attribute attr = consumeLinkParam(parser);
            attrs.add(attr);
        }

        try {
            return new Link(uriReference, attrs);
        } catch (IllegalArgumentException e) {
            throw new LinkParseException(e, "Unable to parse %s", parser.getStringToParse());
        }
    }

    /**
     * consume URI-Reference with rules (subset of RFC3986 (https://datatracker.ietf.org/doc/html/rfc3986#appendix-A)):
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
    protected String consumeUriReference(StringParser<LinkParseException> parser) throws LinkParseException {
        int start = parser.getPosition();
        parser.consumeChar('/');
        if (parser.hasMoreChar()) {
            // try to consume a segment
            String segment = consumeSegment(parser);
            if (segment.length() != 0) {
                // segment is not empty, so this is a segment-nz, we continue
                while (parser.nextCharIs('/')) {
                    parser.consumeNextChar();
                    consumeSegment(parser);
                }
            } // else segment is empty and so this is the end of URI reference
        }
        int end = parser.getPosition();
        return parser.substring(start, end);
    }

    /**
     * consume a segment/segment-nz with rules (subset of RFC3986
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
    protected String consumeSegment(StringParser<LinkParseException> parser) throws LinkParseException {
        int start = parser.getPosition();

        while (true) {
            // unreserved
            if (parser.nextCharIsALPHA() || parser.nextCharIsDIGIT() || parser.nextCharIsIn("-._~")) {
                parser.consumeNextChar();
            }
            // pct-encoded
            else if (parser.nextCharIs('%')) {
                parser.consumeNextChar();
                parser.consumeHEXDIG();
                parser.consumeHEXDIG();
            }
            // sub-delims / ":" / "@"
            else if (parser.nextCharIsIn("!$&'()*+,;=:@")) {
                parser.consumeNextChar();
            } else {
                // no more valid char we step out.
                break;
            }
        }
        int end = parser.getPosition();
        return parser.substring(start, end);
    }

    /**
     * Parse a link-extension with rules:
     *
     * <pre>
     * {@code
     * link-param = link-extension
     * link-extension = ( parmname [ "=" ( ptoken / quoted-string ) ] )
     * }
     * </pre>
     */
    protected Attribute consumeLinkParam(StringParser<LinkParseException> parser) throws LinkParseException {

        String parmName = consumeParmName(parser);
        if (!parser.nextCharIs('=')) {
            try {
                return attributeParser.createEmptyAttribute(parmName);
            } catch (InvalidAttributeException e) {
                parser.raiseException(e, "Invalid Link %s :", parser.getStringToParse());
                return null;
            }
        } else {
            // consume '='
            parser.consumeNextChar();
            return attributeParser.consumeAttributeValue(parmName, parser);
        }
    }

    /**
     * consume parmname as defined in RFC5987 (https://datatracker.ietf.org/doc/html/rfc5987#section-3.2.1):
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
    protected String consumeParmName(StringParser<LinkParseException> parser) throws LinkParseException {
        // loop for attr-char
        int start = parser.getPosition();
        while (parser.nextCharIsALPHA() || parser.nextCharIsDIGIT() || parser.nextCharIsIn("!#$&+-.^_`|~")) {
            parser.consumeNextChar();
        }
        int end = parser.getPosition();

        // get parmName
        String parmName = parser.substring(start, end);

        // check parmName is at least 1 char length
        if (parmName.length() == 0) {
            throw new LinkParseException("Unable to parse [%s] : parmname should not be empty after %s",
                    parser.getStringToParse(), parser.getAlreadyParsedString());
        }
        return parmName;
    }
}
