/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.endpoint;

import org.eclipse.leshan.core.parser.StringParser;

public class DefaultEndPointUriParser implements EndPointUriParser {

    /**
     * This aims to parse {@link EndpointUri} following this simplified grammar derived from RFC3986 :
     *
     * <pre>
     * URI = scheme "://"  host [ ":" port ]
     * </pre>
     *
     * @see <a href="https://www.ietf.org/rfc/rfc3986.html#section-3">RFC3986 - Uniform Resource Identifier (URI):
     *      Generic Syntax</a>
     */
    @Override
    public EndpointUri parse(String uri) throws InvalidEndpointUriException {
        // manage null/empty case
        if (uri == null || uri.length() == 0) {
            throw new InvalidEndpointUriException("URI can not be null or empty");
        }

        // create a String Parser
        StringParser<InvalidEndpointUriException> parser = new StringParser<InvalidEndpointUriException>(uri) {
            @Override
            public void raiseException(String message, Exception cause) throws InvalidEndpointUriException {
                throw new InvalidEndpointUriException(cause, "Invalid uri [%s] : %s", getStringToParse(), message);
            }
        };

        String scheme = consumeScheme(parser);
        parser.consumeChar(':');
        parser.consumeChar('/');
        parser.consumeChar('/');
        String host = consumeHost(parser);
        Integer port = null;
        if (parser.nextCharIs(':')) {
            parser.consumeNextChar();
            port = consumePort(parser);
        }

        if (parser.hasMoreChar()) {
            parser.raiseException("unexpected characters after [%s]", parser.getAlreadyParsedString());
        }
        return new EndpointUri(scheme, host, port);
    }

    /**
     * This aims to parse SCHEME from URI following RFC3986
     *
     * <pre>
     * scheme = ALPHA * (ALPHA / DIGIT / "+" / "-" / ".")
     * </pre>
     *
     * @see <a href="https://www.ietf.org/rfc/rfc3986.html#section-3.1">RFC3986 - 3.1. Scheme</a>
     */
    protected String consumeScheme(StringParser<InvalidEndpointUriException> parser) {
        int start = parser.getPosition();

        parser.consumeALPHA();
        while (parser.nextCharIsALPHA() || parser.nextCharIsDIGIT() || parser.nextCharIsIn("+-.")) {
            parser.consumeNextChar();
        }

        return parser.substring(start, parser.getPosition());
    }

    /**
     * This aims to parse HOST from URI following RFC3986
     *
     * <pre>
     * host = IPliteral / IPv4address / regname
     *
     * IPliteral = "[" ( IPv6address / IPvFuture  ) "]"
     * </pre>
     *
     * @see <a href="https://www.ietf.org/rfc/rfc3986.html#section-3.2.2">RFC3986 - 3.2.2. Host</a>
     */
    protected String consumeHost(StringParser<InvalidEndpointUriException> parser) {
        // if start by [ consume IpLiteral
        if (parser.nextCharIs('[')) {
            return consumeIPliteral(parser);
        }
        // The code below is not needed as RegName include ipv4 grammar.

//        // else try to consume Ipv4
//        String host = tryToConsumeIPv4address(parser);
//        if (host != null) {
//            return host;
//        }
        // else consume regname
        return consumeRegName(parser);
    }

    /**
     * This aims to parse IPv6address from URI following RFC3986
     *
     * <pre>
     *  IP-literal = "[" ( IPv6address / IPv6addrz / IPvFuture  ) "]"
     *
     *  IPv6addrz = IPv6address "%25" ZoneID
     * </pre>
     *
     * @see <a href="https://www.ietf.org/rfc/rfc3986.html#section-3.2.2">RFC3986 - 3.2.2. Host</a>
     */
    protected String consumeIPliteral(StringParser<InvalidEndpointUriException> parser) {
        int start = parser.getPosition();
        parser.consumeChar('[');
        consumeIPv6address(parser);
        if (parser.nextCharIs('%')) {
            parser.consumeNextChar();
            parser.consumeChar('2');
            parser.consumeChar('5');
            consumeZoneID(parser);
        }
        parser.consumeChar(']');
        return parser.substring(start, parser.getPosition());
    }

    /**
     * This aims to parse IPv6address from URI following RFC3986
     *
     * <pre>
     *  IPv6address =                           6( h16 ":" ) ls32
     *             /                       "::" 5( h16 ":" ) ls32
     *             / [               h16 ] "::" 4( h16 ":" ) ls32
     *             / [ *1( h16 ":" ) h16 ] "::" 3( h16 ":" ) ls32
     *             / [ *2( h16 ":" ) h16 ] "::" 2( h16 ":" ) ls32
     *             / [ *3( h16 ":" ) h16 ] "::"    h16 ":"   ls32
     *             / [ *4( h16 ":" ) h16 ] "::"              ls32
     *             / [ *5( h16 ":" ) h16 ] "::"              h16
     *             / [ *6( h16 ":" ) h16 ] "::"
     *
     *   ls32        = ( h16 ":" h16 ) / IPv4address
     *               ; least-significant 32 bits of address
     *
     *   h16         = 1*4HEXDIG
     *               ; 16 bits of address represented in hexadecimal
     * </pre>
     *
     * @see <a href="https://www.ietf.org/rfc/rfc3986.html#section-3.2.2">RFC3986 - 3.2.2. Host</a>
     */
    protected String consumeIPv6address(StringParser<InvalidEndpointUriException> parser) {
        int start = parser.getPosition();

        // A 128-bit IPv6 address is divided into eight 16-bit pieces.
        int nb16bitsParsed = 0;
        // "::" can be used only 1 time to represent 1 or many 16-bits pieces with 0 value.
        boolean elipsedConsumed = false;

        // consume first 16-bit pieces.
        if (parser.nextCharIsUPLOHEXDIG()) {
            // parse 16-bit pieces (h16= 1*4HEXDIG)
            int nbHexDig = 0;
            do {
                parser.consumeNextChar();
                nbHexDig++;
            } while (parser.nextCharIsUPLOHEXDIG() && nbHexDig <= 4);
            nb16bitsParsed++;
        }
        while (parser.nextCharIs(':') && (elipsedConsumed ? nb16bitsParsed < 7 : nb16bitsParsed < 8)) {
            parser.consumeNextChar();
            // consume next 16-bit pieces.
            if (parser.nextCharIsUPLOHEXDIG()) {
                // parse 16-bit pieces (h16= 1*4HEXDIG)
                int beforeHex = parser.getPosition();
                int nbHexDig = 0;
                do {
                    parser.consumeNextChar();
                    nbHexDig++;
                } while (parser.nextCharIsUPLOHEXDIG() && nbHexDig <= 4);

                if (parser.nextCharIs('.')) {
                    // oops we start to parse ipv4 address so rollback.
                    parser.backtrackTo(beforeHex);
                    consumeIPv4address(parser);
                    nb16bitsParsed++;
                    nb16bitsParsed++;
                } else {
                    nb16bitsParsed++;
                }
            } else if (!elipsedConsumed && parser.nextCharIs(':')) {
                // parse eclipse ('::')
                // only 1 elipse allowed
                elipsedConsumed = true;
            }
        }
        String ipv6Addr = parser.substring(start, parser.getPosition());
        if (elipsedConsumed ? nb16bitsParsed > 7 : nb16bitsParsed > 8) {
            parser.raiseException("Invalid IPv6 address :  %s is too long", ipv6Addr);
        }
        return ipv6Addr;
    }

    /**
     * This aims to parse zoneID from URI following RFC6874
     *
     * <pre>
     * ZoneID = 1 * (unreserved / pctencoded)
     * unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
     * pct-encoded   = "%" HEXDIG HEXDIG
     * </pre>
     *
     * @see <a href="https://www.rfc-editor.org/rfc/rfc6874#section-2">RFC6874 - 2. Specification</a>
     * @see <a href="https://www.ietf.org/rfc/rfc3986.html#section-2.3">RFC3986 - 2.3. Unreserved Characters</a>
     * @see <a href="https://www.ietf.org/rfc/rfc3986.html#section-2.1">RFC3986 - 2.1. Percent-Encoding</a>
     */
    private void consumeZoneID(StringParser<InvalidEndpointUriException> parser) {
        while (true) {
            if (parser.nextCharIs('%')) {
                // percent encoding
                parser.consumeNextChar();
                parser.consumeHEXDIG();
                parser.consumeHEXDIG();
            } else if (parser.nextCharIsALPHA() || parser.nextCharIsDIGIT() || parser.nextCharIsIn("-._~")) {
                // unreserved
                parser.consumeNextChar();
            } else {
                return;
            }
        }
    }

    /**
     * This aims to parse IPv6address from URI following RFC3986
     *
     * <pre>
     * IPv4address = dec-octet "." dec-octet "." dec-octet "." dec-octet
     *
     * dec-octet   = DIGIT                 ; 0-9
     *             / %x31-39 DIGIT         ; 10-99
     *             / "1" 2DIGIT            ; 100-199
     *             / "2" %x30-34 DIGIT     ; 200-249
     *             / "25" %x30-35          ; 250-255
     * </pre>
     *
     * @see <a href="https://www.ietf.org/rfc/rfc3986.html#section-3.2.2">RFC3986 - 3.2.2. Host</a>
     */
    protected String tryToConsumeIPv4address(StringParser<InvalidEndpointUriException> parser) {
        int start = parser.getPosition();
        try {
            return consumeIPv4address(parser);
        } catch (InvalidEndpointUriException e) {
            parser.backtrackTo(start);
            return null;
        }
    }

    /**
     * This aims to parse IPv6address from URI following RFC3986
     *
     * <pre>
     * IPv4address = dec-octet "." dec-octet "." dec-octet "." dec-octet
     *
     * </pre>
     *
     * @see <a href="https://www.ietf.org/rfc/rfc3986.html#section-3.2.2">RFC3986 - 3.2.2. Host</a>
     */
    protected String consumeIPv4address(StringParser<InvalidEndpointUriException> parser) {
        int start = parser.getPosition();

        parser.consumeDecOctet();
        parser.consumeChar('.');
        parser.consumeDecOctet();
        parser.consumeChar('.');
        parser.consumeDecOctet();
        parser.consumeChar('.');
        parser.consumeDecOctet();

        return parser.substring(start, parser.getPosition());
    }

    /**
     * This aims to parse regName from URI following RFC3986 but for now we limit parsing to HostName from DNS.
     *
     * <pre>
     * domain ::= subdomain
     * subdomain ::= label | subdomain "." label
     * </pre>
     *
     * @see <a href="https://www.ietf.org/rfc/rfc3986.html#section-3.2.2">RFC1034 - 3.2.2. Host</a>
     * @see <a href="https://www.rfc-editor.org/rfc/rfc1034.html#section-3.5">RFC1034 - 3.5. Preferred name syntax</a>
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc1123#section-2">RFC1123 - 2. GENERAL ISSUES</a>
     * @see <a href="https://cs.uwaterloo.ca/twiki/view/CF/HostNamingRules">Host Naming Rules and RFC Documents</a>
     */
    protected String consumeRegName(StringParser<InvalidEndpointUriException> parser) {
        int start = parser.getPosition();

        // consume domain
        consumeLabel(parser);
        while (parser.nextCharIs('.')) {
            parser.consumeNextChar();
            consumeLabel(parser);
        }

        // return domain value as regname
        int end = parser.getPosition();
        if (end > start) {
            return parser.substring(start, end);
        } else {
            return null;
        }
    }

    /**
     * This aims to consume label from HostName from DNS.
     *
     * <pre>
     * label ::= let-dig [ [ ldh-str ] let-dig ]
     * ldh-str ::= let-dig-hyp | let-dig-hyp ldh-str
     * let-dig-hyp ::= let-dig | "-"
     * let-dig ::= letter | digit
     * letter ::= any one of the 52 alphabetic characters A through Z in upper case and a through z in lower case
     * digit ::= any one of the ten digits 0 through 9
     * </pre>
     *
     * @see <a href="https://www.ietf.org/rfc/rfc3986.html#section-3.2.2">RFC1034 - 3.2.2. Host</a>
     * @see <a href="https://www.rfc-editor.org/rfc/rfc1034.html#section-3.5">RFC1034 - 3.5. Preferred name syntax</a>
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc1123#section-2">RFC1123 - 2. GENERAL ISSUES</a>
     * @see <a href="https://cs.uwaterloo.ca/twiki/view/CF/HostNamingRules">Host Naming Rules and RFC Documents</a>
     */
    protected void consumeLabel(StringParser<InvalidEndpointUriException> parser) {
        // consume first char (letter or digit)
        if (parser.nextCharIsALPHA() || parser.nextCharIsDIGIT()) {
            parser.consumeNextChar();
        } else {
            return;
        }
        while (true) {
            // hyphen ('-')
            if (parser.nextCharIs('-')) {
                int beforeHyp = parser.getPosition();
                // consume all '-'
                do {
                    parser.consumeNextChar(); // consume '-'
                } while (parser.nextCharIs('-'));

                if (parser.nextCharIsALPHA() || parser.nextCharIsDIGIT()) {
                    // '-' should be followed by letter or digit
                    parser.consumeNextChar();
                } else {
                    // if next char is not letter or digit, label ends before the first '-'
                    parser.backtrackTo(beforeHyp);
                    return;
                }
            } else
            // letter or digit
            if (parser.nextCharIsALPHA() || parser.nextCharIsDIGIT()) {
                parser.consumeNextChar();
            } else {
                // we fing the end of label
                return;
            }
        }
    }

    /**
     * This aims to parse PORT from URI following RFC3986
     *
     * Official Grammar is :
     *
     * <pre>
     * port        = *DIGIT
     * </pre>
     *
     * But It seems CARDINAL makes more sense :
     *
     * <pre>
     * port        = cardinal
     * cardinal       = "0" / ( %x31-39 *DIGIT )
     * </pre>
     *
     * @see <a href="https://www.ietf.org/rfc/rfc3986.html#section-3.2.3">RFC3986 - 3.2.3. Port</a>
     */
    protected Integer consumePort(StringParser<InvalidEndpointUriException> parser) {
        String portStr = parser.consumeCardinal();
        try {
            Integer port = new Integer(portStr);
            validatePort(port, parser);
            return port;
        } catch (NumberFormatException e) {
            parser.raiseException(e, "port %s is not a Number", portStr);
            return null;
        }
    }

    @Override
    public void validateScheme(String scheme) {
        // manage null/empty case
        if (scheme == null || scheme.length() == 0) {
            throw new InvalidEndpointUriException("scheme must not be null");
        }

        // create a String Parser
        StringParser<InvalidEndpointUriException> parser = new StringParser<InvalidEndpointUriException>(scheme) {
            @Override
            public void raiseException(String message, Exception cause) throws InvalidEndpointUriException {
                throw new InvalidEndpointUriException(cause, "Invalid scheme [%s] : %s", getStringToParse(), message);
            }
        };

        consumeScheme(parser);
        if (parser.hasMoreChar()) {
            parser.raiseException("unexpected characters after [%s]", parser.getAlreadyParsedString());
        }
    }

    @Override
    public void validateHost(String host) {
        // manage null/empty case
        if (host == null || host.length() == 0) {
            throw new InvalidEndpointUriException("host must not be null");
        }

        // create a String Parser
        StringParser<InvalidEndpointUriException> parser = new StringParser<InvalidEndpointUriException>(host) {
            @Override
            public void raiseException(String message, Exception cause) throws InvalidEndpointUriException {
                throw new InvalidEndpointUriException(cause, "Invalid host [%s] : %s", getStringToParse(), message);
            }
        };

        consumeHost(parser);
        if (parser.hasMoreChar()) {
            parser.raiseException("unexpected characters after [%s]", parser.getAlreadyParsedString());
        }
    }

    @Override
    public void validatePort(String port) {
        // manage null/empty case
        if (port == null || port.length() == 0) {
            throw new InvalidEndpointUriException("scheme must not be null");
        }

        // create a String Parser
        StringParser<InvalidEndpointUriException> parser = new StringParser<InvalidEndpointUriException>(port) {
            @Override
            public void raiseException(String message, Exception cause) throws InvalidEndpointUriException {
                throw new InvalidEndpointUriException(cause, "Invalid port [%s] : %s", getStringToParse(), message);
            }
        };

        consumePort(parser);
        if (parser.hasMoreChar()) {
            parser.raiseException("unexpected characters after [%s]", parser.getAlreadyParsedString());
        }
    }

    protected void validatePort(Integer port, StringParser<InvalidEndpointUriException> parser) {
        // manage null/empty case
        if (port == null) {
            if (parser != null) {
                parser.raiseException("port must not be null");
            } else {
                throw new InvalidEndpointUriException("port must not be null");
            }
        }

        if (port > 65535 || port < 0) {
            if (parser != null) {
                parser.raiseException("Invalid port [%d] : port must be an integer between 0 and 65535", port);
            } else {
                throw new InvalidEndpointUriException("Invalid port [%d] : port must be an integer between 0 and 65535",
                        port);
            }
        }
    }

    @Override
    public void validatePort(Integer port) {
        validatePort(port, null);
    }
}
