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
package org.eclipse.leshan.client.demo.cli;

import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.californium.elements.util.Bytes;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.leshan.client.demo.cli.converters.HexadecimalConverter;
import org.eclipse.leshan.client.demo.cli.converters.PrivateKeyConverter;
import org.eclipse.leshan.client.demo.cli.converters.PublicKeyConverter;
import org.eclipse.leshan.client.demo.cli.converters.X509CertificateConverter;
import org.eclipse.leshan.core.CertificateUsage;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;
import picocli.CommandLine.TypeConversionException;

/**
 * Command line Section about DTLS Identity (PSK, RPK or X509).
 */
public class IdentitySection {

    private static final Logger LOG = LoggerFactory.getLogger(IdentitySection.class);

    @ArgGroup(exclusive = false,
              heading = "%n@|bold,underline PSK Options|@ %n%n"//
                      + "@|italic " //
                      + "By default Leshan demo use non secure connection.%n"//
                      + "To use CoAP over DTLS with Pre-Shared Key, -i and -p options should be used together." //
                      + "|@%n%n")
    private PskSection psk;

    @ArgGroup(exclusive = false,
              heading = "@|italic " //
                      + "%n -cprik must be used with RPK or X509%n" //
                      + "|@%n")
    private PrivKeyAndRpkOrX509Section prikAnd;

    public static class PskSection {
        @Option(required = true,
                names = { "-i", "--psk-identity" },
                description = { //
                        "Set the LWM2M or Bootstrap server PSK identity in ascii." })
        public String identity;

        @Option(required = true,
                names = { "-p", "--psk-key" },
                description = { //
                        "Set the LWM2M or Bootstrap server Pre-Shared-Key in hexa." },
                converter = HexadecimalConverter.class,
                type = byte[].class)
        public Bytes sharekey;
    }

    static class PrivKeyAndRpkOrX509Section {
        @Option(required = true,
                names = { "-cprik", "--client-private-key" },
                description = { //
                        "The path to your client private key file", //
                        "The private key should be in PKCS#8 format (DER encoding)." },
                converter = PrivateKeyConverter.class)
        private PrivateKey cprik;

        @ArgGroup(exclusive = true)
        private RpkOrX509Section rpkOrX509;
    }

    static class RpkOrX509Section {
        @ArgGroup(exclusive = false,
                  heading = "%n@|bold,underline RPK Options|@ %n%n"//
                          + "@|italic " //
                          + "By default Leshan demo use non secure connection.%n" //
                          + "To use CoAP over DTLS with Raw Public Key, -cpubk -cprik -spubk options should be used together.%n" //
                          + "To get helps about files format and how to generate it, see :%n" //
                          + "See https://github.com/eclipse/leshan/wiki/Credential-files-format" //
                          + "|@%n%n")
        private RpkSection rpk;

        @ArgGroup(exclusive = false,
                  heading = "%n@|bold,underline X509 Options|@ %n%n"//
                          + "@|italic " //
                          + "By default Leshan demo use non secure connection.%n" //
                          + "To use CoAP over DTLS with X509 certificate, -ccert -cprik -scert options should be used together.%n" //
                          + "To get helps about files format and how to generate it, see :%n" //
                          + "See https://github.com/eclipse/leshan/wiki/Credential-files-format" //
                          + "|@%n%n")
        private X509Section x509;
    }

    public static class RpkSection {
        public PrivateKey cprik; // HACK to access private key

        @Option(required = true,
                names = { "-cpubk", "--client-public-key" },
                description = { //
                        "The path to your client public key file.", //
                        "The public Key should be in SubjectPublicKeyInfo format (DER encoding)." },
                converter = PublicKeyConverter.class)
        public PublicKey cpubk;

        @Option(required = true,
                names = { "-spubk", "--server-public-key" },
                description = { //
                        "The path to your server public key file.", //
                        "The public Key should be in SubjectPublicKeyInfo format (DER encoding)." },
                converter = PublicKeyConverter.class)
        public PublicKey spubk;
    }

    public static class X509Section {
        public PrivateKey cprik; // HACK to access private key

        @Option(required = true,
                names = { "-ccert", "--client-certificate" },
                description = { //
                        "The path to your client certificate file.", //
                        "The certificate Common Name (CN) should generaly be equal to the client endpoint name (see -n option).", //
                        "The certificate should be in X509v3 format (DER encoding)." },
                converter = X509CertificateConverter.class)
        public X509Certificate ccert;

        @Option(required = true,
                names = { "-scert", "--server-certificate" },
                description = { //
                        "The path to your server certificate file (see -certificate-usage option).", //
                        "The certificate should be in X509v3 format (DER encoding)." },
                converter = X509CertificateConverter.class)
        public X509Certificate scert;

        @Option(names = { "-cu", "--certificate-usage" },
                defaultValue = "3",
                description = { //
                        "Certificate Usage (as integer) defining how to use server certificate", //
                        " - 0 : CA constraint", //
                        " - 1 : service certificate constraint", //
                        " - 2 : trust anchor assertion", //
                        " - 3 : domain issued certificate (Default value)", //
                        " (Usage are described at https://tools.ietf.org/html/rfc6698#section-2.1.1)" },
                converter = CertificateUsageConverter.class)
        public CertificateUsage certUsage;

        private static class CertificateUsageConverter implements ITypeConverter<CertificateUsage> {
            @Override
            public CertificateUsage convert(String s) {
                return CertificateUsage.fromCode(Integer.parseInt(s));
            }
        };

        @Option(names = { "-ts", "--truststore" },
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
                        "Default: empty store." })
        private void setTruststore(String trustStoreName) {
            trustStore = new ArrayList<>();

            if (trustStoreName.startsWith("file://")) {
                // Treat argument as Java trust store
                try {
                    Certificate[] trustedCertificates = SslContextUtil.loadTrustedCertificates(trustStoreName);
                    trustStore.addAll(Arrays.asList(trustedCertificates));
                } catch (Exception e) {
                    throw new TypeConversionException("Failed to load trust store : " + e.getMessage());
                }
            } else {
                // Treat argument as file or directory
                File input = new File(trustStoreName);

                // check input exists
                if (!input.exists()) {
                    throw new TypeConversionException(
                            "Failed to load trust store - file or directory does not exist : " + input.toString());
                }

                // get input files.
                File[] files;
                if (input.isDirectory()) {
                    files = input.listFiles();
                } else {
                    files = new File[] { input };
                }
                for (File file : files) {
                    try {
                        trustStore.add(SecurityUtil.certificate.readFromFile(file.getAbsolutePath()));
                    } catch (Exception e) {
                        LOG.warn("Unable to load X509 files {} : {} ", file.getAbsolutePath(), e.getMessage());
                    }
                }
            }
        }

        public List<Certificate> trustStore = Collections.emptyList();
    }

    /* ***** Some convenient method to access to identity easily ****/

    public boolean isPSK() {
        return psk != null;
    }

    public PskSection getPsk() {
        return psk;
    }

    public boolean isRPK() {
        return prikAnd != null && prikAnd.rpkOrX509.rpk != null;
    }

    public RpkSection getRPK() {
        prikAnd.rpkOrX509.rpk.cprik = prikAnd.cprik;
        return prikAnd.rpkOrX509.rpk;
    }

    public boolean isx509() {
        return prikAnd != null && prikAnd.rpkOrX509.x509 != null;
    }

    public X509Section getX509() {
        prikAnd.rpkOrX509.x509.cprik = prikAnd.cprik;
        return prikAnd.rpkOrX509.x509;
    }

}
