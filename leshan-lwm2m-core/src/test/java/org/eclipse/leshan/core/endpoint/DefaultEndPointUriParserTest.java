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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.util.stream.Stream;

import org.eclipse.leshan.core.link.attributes.InvalidAttributeException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class DefaultEndPointUriParserTest {

    private final EndPointUriParser parser = new DefaultEndPointUriParser();

    private static Stream<Arguments> validAttributes() throws InvalidAttributeException {
        return Stream.of(//
                Arguments.of("coap", "localhost", null), //
                Arguments.of("coaps", "localhost", null), //
                Arguments.of("coap+tcp", "localhost", null), //
                Arguments.of("coaps+tcp", "localhost", null), //
                Arguments.of("coap", "localhost", 3), //
                Arguments.of("coap", "domain.org", 80), //
                Arguments.of("coap", "domain.org", null), //
                Arguments.of("coap", "sub.domain.org", 888), //
                Arguments.of("coap", "sub.domain.org", null), //
                Arguments.of("coap", "0.0.0.0", 8888), //
                Arguments.of("coap", "0.0.0.0", null), //
                Arguments.of("coap", "sub-sub-domain.sub----domain.org", null), //
                Arguments.of("coap", "15.200.199.255", 8888), //
                Arguments.of("coap", "sub.domain.org", null), //
                Arguments.of("http", "[2001:db8::192.168.1.1]", 8080), //
                Arguments.of("https", "[2001:db8:85a3::8a2e:370:192.168.0.1]", 443), //
                Arguments.of("ftp", "[2001:0db8:0000:0042::172.16.0.1]", 21), //
                Arguments.of("ws", "[2607:f0d0:1002:51::192.168.0.100]", 9090), //
                Arguments.of("wss", "[2001:db8::10.1.1.1]", 8443), //
                Arguments.of("smtp", "[2001:4860:4860::8888]", 25), //
                Arguments.of("ssh", "[::1]", 22), //
                Arguments.of("telnet", "[2001:db8::ff00:42:8329]", 23), //
                Arguments.of("telnet", "[2001:db8::ff00:42:8329%25zoneid]", 23), //
                Arguments.of("telnet", "[2001:db8::ff00:42:8329%25%AA%FFzoneid]", 23) //
        );
    }

    @ParameterizedTest(name = "Tests {index} : {0}://{1}:{2}")
    @MethodSource("validAttributes")
    public void check_one_valid_uri(String scheme, String host, Integer port) throws InvalidEndpointUriException {
        EndpointUri uri = parser.parse(scheme + "://" + host + (port != null ? ':' + port.toString() : ""));

        assertEquals(scheme, uri.getScheme());
        assertEquals(host, uri.getHost());
        assertEquals(port, uri.getPort());
    }

    @ParameterizedTest
    @ValueSource(strings = { //
            "coap", //
            "coap:localhost", //
            "coap:/localhost", //
            "coap://localhost:dazd", //
            "coap://domain.org:da80", //
            "coap://domain----.org:80", //
            "coap://----domain.org:80", //
            "coap://[dzqd]:80", //
            "coap://[aa:aa:aa:aa:aa:aa:aa:aa:aa:aa]:80", //
            "coap://[aa:aa:aa:aa::aa:aa:aa:aa]:80", //
            "coap://[aa:aa:aa:aa:aa:aa:aa:255.255.255.255]:80", //
            "coap://[aa:aa:aa:aa:aa:aa::255.255.255.255]:80", //
            "coap://[aa:aa:aa::aa:aa:aa:255.255.255.255]:80", //
            "coap://[aa:aa:aa::aa:aa:aa:260.255.255.255]:80", //
            "coap://[aa:aa:aa::aa:aa:aa:255.256.255.255]:80", //
            "coap://[aa:aa:aa::aa:aa:aa:2555.256.255.255]:80", //
            "coap://[aa:aa:aa::aa:aa:aa:255.255.255.255.255]:80", //
            "coap://[aa:aa:aa::aa:aa:aa%zoneid]:80", //
            "coap://[aa:aa:aa::aa:aa:aa%25%zoneid]:80", //
    })
    public void parse_invalid_formats(String uri) {
        assertThrowsExactly(InvalidEndpointUriException.class, () -> {
            parser.parse(uri);
        });
    }
}
