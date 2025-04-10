/*******************************************************************************
 * Copyright (c) 2025 Sierra Wireless and others.
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
package org.eclipse.leshan.core.security.util;

import static org.eclipse.leshan.core.util.TestCredentialsUtil.CLIENT_CERT_DER;
import static org.eclipse.leshan.core.util.TestCredentialsUtil.CLIENT_CERT_PEM;
import static org.eclipse.leshan.core.util.TestCredentialsUtil.CLIENT_CERT_WITH_GARBAGE_DATA_AFTER_DER;
import static org.eclipse.leshan.core.util.TestCredentialsUtil.CLIENT_CERT_WITH_GARBAGE_DATA_AFTER_PEM;
import static org.eclipse.leshan.core.util.TestCredentialsUtil.CLIENT_CERT_WITH_GARBAGE_DATA_BEFORE_DER;
import static org.eclipse.leshan.core.util.TestCredentialsUtil.CLIENT_CERT_WITH_GARBAGE_DATA_BEFORE_PEM;
import static org.eclipse.leshan.core.util.TestCredentialsUtil.CLIENT_CHAIN_DER;
import static org.eclipse.leshan.core.util.TestCredentialsUtil.CLIENT_CHAIN_PEM;
import static org.eclipse.leshan.core.util.TestCredentialsUtil.CLIENT_CHAIN_WITH_GARBAGE_DATA_AFTER_DER;
import static org.eclipse.leshan.core.util.TestCredentialsUtil.CLIENT_CHAIN_WITH_GARBAGE_DATA_AFTER_PEM;
import static org.eclipse.leshan.core.util.TestCredentialsUtil.CLIENT_CHAIN_WITH_GARBAGE_DATA_BEFORE_DER;
import static org.eclipse.leshan.core.util.TestCredentialsUtil.CLIENT_CHAIN_WITH_GARBAGE_DATA_BEFORE_PEM;
import static org.eclipse.leshan.core.util.TestCredentialsUtil.CLIENT_CHAIN_WITH_GARBAGE_DATA_BETWEEN_DER;
import static org.eclipse.leshan.core.util.TestCredentialsUtil.CLIENT_CHAIN_WITH_GARBAGE_DATA_BETWEEN_PEM;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.leshan.core.credentials.CredentialsReader;
import org.eclipse.leshan.core.util.TestCredentialsUtil.Credential;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SecurityUtilTest {

    static List<Credential> allCredentials = Arrays.asList( //
            CLIENT_CERT_DER, //
            CLIENT_CERT_PEM, //
            CLIENT_CERT_WITH_GARBAGE_DATA_BEFORE_DER, //
            CLIENT_CERT_WITH_GARBAGE_DATA_BEFORE_PEM, //
            CLIENT_CERT_WITH_GARBAGE_DATA_AFTER_DER, //
            CLIENT_CERT_WITH_GARBAGE_DATA_AFTER_PEM, //
            //
            CLIENT_CHAIN_DER, //
            CLIENT_CHAIN_PEM, //
            CLIENT_CHAIN_WITH_GARBAGE_DATA_BEFORE_DER, //
            CLIENT_CHAIN_WITH_GARBAGE_DATA_BEFORE_PEM, //
            CLIENT_CHAIN_WITH_GARBAGE_DATA_BETWEEN_DER, //
            CLIENT_CHAIN_WITH_GARBAGE_DATA_BETWEEN_PEM, //
            CLIENT_CHAIN_WITH_GARBAGE_DATA_AFTER_DER, //
            CLIENT_CHAIN_WITH_GARBAGE_DATA_AFTER_PEM);

    static List<Credential> allCredentialsWithout(List<Credential> credentialsToExclude) {
        ArrayList<Credential> l = new ArrayList<>(allCredentials);
        l.removeAll(credentialsToExclude);
        return l;
    }

    static List<Reader> readerToTests() {
        return Arrays.asList( //
                givenReader("DER Certificate Reader", SecurityUtil.derCertificate) //
                        .canRead(CLIENT_CERT_DER), //
                givenReader("PEM Certificate Reader", SecurityUtil.pemCertificate) //
                        .canRead(CLIENT_CERT_PEM), //
                givenReader("DER/PEM Certificate Reader", SecurityUtil.certificate) //
                        .canRead(CLIENT_CERT_DER) //
                        .canRead(CLIENT_CERT_PEM), //
                givenReader("DER/PEM Certificate Chain Reader", SecurityUtil.certificateChain) //
                        .canRead(CLIENT_CERT_DER) //
                        .canRead(CLIENT_CERT_PEM) //
                        .canRead(CLIENT_CHAIN_DER) //
                        .canRead(CLIENT_CHAIN_PEM));
    }

    static Stream<Arguments> successCase() {
        // create 1 success test by file supported for each reader
        return readerToTests().stream().flatMap(reader -> reader.readableFiles.stream()
                .map(credential -> Arguments.of(reader.readerName, reader.reader, credential.getPath())));
    }

    @ParameterizedTest(name = "{0} - {2}")
    @MethodSource("successCase")
    void reader_successfully_read_file(String name, CredentialsReader<?> reader, String credentialPath) {
        assertDoesNotThrow(() -> {
            return reader.readFromResource(credentialPath);
        });
    }

    static Stream<Arguments> failureCase() {
        // create 1 fail test by file not supported for each reader
        // file not supported are all files minus supported one
        return readerToTests().stream().flatMap(reader -> allCredentialsWithout(reader.readableFiles).stream()
                .map(credential -> Arguments.of(reader.readerName, reader.reader, credential.getPath())));
    }

    @ParameterizedTest(name = "{0} - {2}")
    @MethodSource("failureCase")
    void reader_failed_to_read_file(String name, CredentialsReader<?> reader, String credentialPath) {
        assertThrows(GeneralSecurityException.class, () -> {
            reader.readFromResource(credentialPath);
        });
    }

    static class Reader {
        String readerName;
        CredentialsReader<?> reader;
        List<Credential> readableFiles;

        Reader canRead(Credential credential) {
            readableFiles.add(credential);
            return this;
        }
    }

    static Reader givenReader(String readerName, CredentialsReader<?> reader) {
        Reader r = new Reader();
        r.readerName = readerName;
        r.reader = reader;
        r.readableFiles = new ArrayList<Credential>();
        return r;
    }
}
