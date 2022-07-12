/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.server.core.demo.cli;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import org.eclipse.leshan.core.demo.cli.MultiParameterException;
import org.eclipse.leshan.core.demo.cli.converters.PrivateKeyConverter;
import org.eclipse.leshan.core.demo.cli.converters.PublicKeyConverter;
import org.eclipse.leshan.core.demo.cli.converters.TruststoreConverter;
import org.eclipse.leshan.core.util.SecurityUtil;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

/**
 * Command line Section about DTLS Identity (RPK or X509).
 *
 * This class is used by bsserver-demo and server-demo
 */
public class IdentitySection {

    @ArgGroup(exclusive = false,
              heading = "%n@|bold,underline X509 Options|@ %n%n"//
                      + "@|italic " //
                      + "By default Leshan demo uses an embedded self-signed certificate and " //
                      + "trusts any client certificates allowing to use RPK or X509 " //
                      + "at client side.%n" //
                      + "To use X509 with your own server key, certificate and truststore : %n" //
                      + "     [-xcert, -xprik], [-truststore] should be used together.%n" //
                      + "To get helps about files format and how to generate it, see : %n" //
                      + "See https://github.com/eclipse/leshan/wiki/Credential-files-format" //
                      + "|@%n%n")
    private X509Section x509;

    @ArgGroup(exclusive = false,
              heading = "%n@|bold,underline RPK Options|@ %n%n"//
                      + "@|italic " //
                      + "By default Leshan demo uses an embedded self-signed certificate and " //
                      + "trusts any client certificates allowing to use RPK or X509 " //
                      + "at client side.%n" //
                      + "To allow RPK only with your own keys : %n" //
                      + "     -rpubk -rprik options should be used together.%n" //
                      + "To get helps about files format and how to generate it, see : %n" //
                      + "See https://github.com/eclipse/leshan/wiki/Credential-files-format" //
                      + "|@%n%n")
    private RpkSection rpk;

    private static class RpkSection {
        @Option(required = true,
                names = { "-rpubk", "--rpk-public-key" },
                description = { //
                        "The path to your server public key file.", //
                        "The public Key should be in SubjectPublicKeyInfo format (DER encoding)." },
                converter = PublicKeyConverter.class)
        public PublicKey pubk;

        @Option(required = true,
                names = { "-rprik", "--rpk-private-key" },
                description = { //
                        "The path to your server private key file", //
                        "The private key should be in PKCS#8 format (DER encoding)." },
                converter = PrivateKeyConverter.class)
        private PrivateKey prik;
    }

    private static class X509Section {
        @Option(names = { "-xcert", "--x509-certificate-chain" },
                order = 1,
                description = { //
                        "The path to your server certificate or certificate chain file.", //
                        "The certificate Common Name (CN) should generally be equal to the server hostname.", //
                        "The certificate should be in X509v3 format (DER or PEM encoding).", //
                        "The certificate chain should be in X509v3 format (PEM encoding)." })
        private void setCertChain(String value) throws IOException, GeneralSecurityException {
            certchain = SecurityUtil.certificateChain.readFromFile(value);
        }

        private X509Certificate[] certchain;

        @Option(names = { "-xprik", "--x509-private-key" },
                order = 2,
                description = { //
                        "The path to your server private key file", //
                        "The private key should be in PKCS#8 format (DER encoding)." },
                converter = PrivateKeyConverter.class)
        private PrivateKey prik;

        @Option(names = { "-ts", "--truststore" },
                order = 3,
                description = { //
                        "The path to  : ", //
                        " - a root certificate file to trust, ", //
                        " - OR a folder containing trusted certificates,", //
                        " - OR trust store URI.", //
                        "", //
                        "Certificates must be in in X509v3 format (DER encoding)", //
                        "", //
                        "URI format:", //
                        "  file://<path-to-store>#<password>#<alias-pattern>", //
                        "Where : ", //
                        "- path-to-store is path to pkcs12 trust store file", //
                        "- password is HEX formatted password for store", //
                        "- alias-pattern can be used to filter trusted certificates and can also be empty to get all", //
                        "", //
                        "Default: trust all certificates (only OK for demos)." })
        private void setTruststore(String trustStoreName) throws Exception {
            trustStore = TruststoreConverter.convertValue(trustStoreName);
        }

        private List<Certificate> trustStore = Collections.emptyList();
    }

    /* ***** Some convenient method to access to identity easily ****/
    protected PublicKey publicKey;
    protected PrivateKey privateKey;
    protected X509Certificate[] certChain;
    protected List<Certificate> trustStore;

    public void build(CommandLine cmd) {
        if (isRPK()) {
            publicKey = rpk.pubk;
            privateKey = rpk.prik;
        } else {
            try {
                if (x509 != null) {
                    if (x509.certchain != null && x509.prik == null || x509.certchain == null && x509.prik != null) {
                        throw new MultiParameterException(cmd, "-xprik and -xcert MUST be used together", "-xprik",
                                "-xcert");
                    }
                    certChain = x509.certchain;
                    privateKey = x509.prik;
                    trustStore = x509.trustStore;
                }
                // assign default value
                if (certChain == null) {
                    certChain = SecurityUtil.certificateChain.readFromResource("credentials/server_cert.der");
                }
                if (privateKey == null) {
                    privateKey = SecurityUtil.privateKey.readFromResource("credentials/server_privkey.der");
                }
                if (trustStore == null) {
                    trustStore = Collections.emptyList();
                }
            } catch (IOException | GeneralSecurityException e) {
                throw new IllegalStateException("Unable to load default credentials", e);
            }
        }
    }

    public boolean isRPK() {
        return rpk != null;
    }

    public boolean isx509() {
        return !isRPK();
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public X509Certificate[] getCertChain() {
        return certChain;
    }

    public List<Certificate> getTrustStore() {
        return trustStore;
    }
}
