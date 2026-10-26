/*******************************************************************************
 * Copyright (c) 2026    Sierra Wireless and others.
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
package org.eclipse.leshan.transport.californium.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.net.InetSocketAddress;
import java.util.stream.Stream;

import javax.security.auth.x500.X500Principal;

import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.auth.PreSharedKeyIdentity;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.peer.X509Identity;
import org.eclipse.leshan.core.security.certificate.util.X509CertUtil;
import org.eclipse.leshan.transport.californium.Lwm2mEndpointContextMatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * When the server wants to send a request to an X.509 client, {@link DefaultCoapsIdentityHandler} describes the
 * expected peer, and {@link Lwm2mEndpointContextMatcher} checks it against the connection which really exists.
 */
class DefaultCoapsIdentityHandlerTest {

    private static final InetSocketAddress ADDRESS = new InetSocketAddress("127.0.0.1", 5684);

    private final DefaultCoapsIdentityHandler handler = new DefaultCoapsIdentityHandler();
    private final Lwm2mEndpointContextMatcher matcher = new Lwm2mEndpointContextMatcher();

    /**
     * A common name, and a certificate subject with this common name written as an escaped distinguished name. The
     * subjects are not built with the code under test.
     */
    static Stream<Arguments> clients() {
        return Stream.of(//
                arguments("simple", "device-1", "CN=device-1"), //
                arguments("urn", "urn:imei:123456789012345", "CN=urn:imei:123456789012345"), //
                arguments("comma", "dev,ice", "CN=dev\\,ice"), //
                arguments("plus", "a+b", "CN=a\\+b"), //
                arguments("semicolon", "a;b", "CN=a\\;b"), //
                arguments("angle brackets", "<a>", "CN=\\<a\\>"), //
                arguments("quotes", "say \"hi\"", "CN=say \\\"hi\\\""), //
                arguments("backslash", "back\\slash", "CN=back\\\\slash"), //
                arguments("leading #", "#hash", "CN=\\#hash"), //
                arguments("leading space", " lead", "CN=\\ lead"), //
                arguments("trailing space", "trail ", "CN=trail\\ "), //
                arguments("equals", "a=b", "CN=a\\=b"), //
                arguments("looks like 2 attributes", "x,CN=victim", "CN=x\\,CN\\=victim"), //
                arguments("non ASCII", "\u010dlovek", "CN=\u010dlovek"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("clients")
    void request_matches_the_connection_of_the_same_x509_client(String label, String commonName,
            String subjectOfCertificate) {
        X500Principal subject = new X500Principal(subjectOfCertificate);
        // check the test data
        assertEquals(commonName, X509CertUtil.extractCN(subject));

        EndpointContext requested = handler.createEndpointContext(new IpPeer(ADDRESS, new X509Identity(commonName)),
                false);

        assertTrue(matcher.isToBeSent(requested, new AddressEndpointContext(ADDRESS, subject)));
    }

    @ParameterizedTest
    @ValueSource(strings = { "CN=victim", "CN=x", "CN=x,CN=victim" })
    void request_for_a_common_name_looking_like_several_attributes_does_not_match_other_subjects(String otherSubject) {
        EndpointContext requested = handler.createEndpointContext(new IpPeer(ADDRESS, new X509Identity("x,CN=victim")),
                false);

        assertFalse(
                matcher.isToBeSent(requested, new AddressEndpointContext(ADDRESS, new X500Principal(otherSubject))));
    }

    @Test
    void request_does_not_match_the_connection_of_a_certificate_with_several_common_names() {
        EndpointContext requested = handler.createEndpointContext(new IpPeer(ADDRESS, new X509Identity("device-1")),
                false);

        assertFalse(matcher.isToBeSent(requested,
                new AddressEndpointContext(ADDRESS, new X500Principal("CN=device-2,CN=device-1"))));
    }

    @Test
    void request_does_not_match_a_connection_which_is_not_x509() {
        EndpointContext requested = handler.createEndpointContext(new IpPeer(ADDRESS, new X509Identity("device-1")),
                false);

        assertFalse(matcher.isToBeSent(requested,
                new AddressEndpointContext(ADDRESS, new PreSharedKeyIdentity("device-1"))));
    }
}
