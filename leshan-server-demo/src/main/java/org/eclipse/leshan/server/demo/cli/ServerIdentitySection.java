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
package org.eclipse.leshan.server.demo.cli;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.leshan.core.demo.cli.MultiParameterException;
import org.eclipse.leshan.server.core.demo.cli.IdentitySection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

/**
 * Command line Section about DTLS Identity (RPK or X509).
 */
public class ServerIdentitySection extends IdentitySection {

    private static final Logger LOG = LoggerFactory.getLogger(ServerIdentitySection.class);
    @ArgGroup(exclusive = false,
              heading = "%n@|bold,underline X509 (deprecated way)|@ %n%n"//
                      + "@|italic " //
                      + "By default Leshan demo uses an embedded self-signed certificate and " //
                      + "trusts any client certificates allowing to use RPK or X509 at client side.%n" //
                      + "If you want to use your own server keys, certificates and truststore, " //
                      + "you can provide a keystore using : %n" //
                      + "    -ks, -ksp, [-kst], [-ksa], [-ksap] should be used together%n" //
                      + "To get helps about files format and how to generate it, see : %n" //
                      + "See https://github.com/eclipse/leshan/wiki/Credential-files-format" //
                      + "|@%n%n")
    private X509KeystoreSection x509KeyStore;

    private static class X509KeystoreSection {

        @Option(names = { "-ks", "--keystore" },
                required = true,
                description = { //
                        "Set the key store file." })
        private String keystore;

        @Option(names = { "-ksp", "--storepass" },
                required = true,
                description = { //
                        "Set the key store password." })
        private String storepass;

        @Option(names = { "-kst", "--storetype" },
                required = true,
                defaultValue = "jks",
                description = { //
                        "Set the key store type.", //
                        "Default: ${DEFAULT-VALUE}" })
        private String storetype = "jks";

        @Option(names = { "-ksa", "--alias" },
                required = true,
                defaultValue = "leshan",
                description = { //
                        "Set the key store alias to use for server credentials.", //
                        "All other alias referencing a certificate will be trusted.", //
                        "Default: ${DEFAULT-VALUE}." })
        private String alias;

        @Option(names = { "-ksap", "--keypass" },
                required = false,
                description = { //
                        "Set the key store alias password to use.", //
                        "Default: use --storepass value as default." })
        private String keypass;
    }

    /* ***** Some convenient method to access to identity easily ****/
    @Override
    public void build(CommandLine cmd) {
        if (x509KeyStore != null) {
            LOG.warn(
                    "Keystore way [-ks, -ksp, -kst, -ksa, -ksap] is DEPRECATED for leshan demo and will probably be removed soon, please use [-cert, -prik, -truststore] options");

            // Deprecated way : Set up X.509 mode (+ RPK)
            try {
                KeyStore keyStore = KeyStore.getInstance(x509KeyStore.storetype);
                try (FileInputStream fis = new FileInputStream(x509KeyStore.keystore)) {
                    keyStore.load(fis, x509KeyStore.keypass == null ? null : x509KeyStore.storepass.toCharArray());
                    List<Certificate> trustedCertificates = new ArrayList<>();
                    for (Enumeration<String> aliases = keyStore.aliases(); aliases.hasMoreElements();) {
                        String alias = aliases.nextElement();
                        if (keyStore.isCertificateEntry(alias)) {
                            trustedCertificates.add(keyStore.getCertificate(alias));
                        } else if (keyStore.isKeyEntry(alias) && alias.equals(x509KeyStore.alias)) {
                            List<X509Certificate> x509CertificateChain = new ArrayList<>();
                            Certificate[] certificateChain = keyStore.getCertificateChain(alias);
                            if (certificateChain == null || certificateChain.length == 0) {
                                throw new MultiParameterException(cmd,
                                        String.format(
                                                "Keystore alias [%s] must have a non-empty chain of X509Certificates.",
                                                x509KeyStore.alias),
                                        "-ksa");
                            }

                            for (Certificate cert : certificateChain) {
                                if (!(cert instanceof X509Certificate)) {
                                    throw new MultiParameterException(cmd,
                                            String.format(
                                                    "Non-X.509 certificate in alias [%s] chain is not supported: %s",
                                                    x509KeyStore.alias, cert),
                                            "-ksa");
                                }
                                x509CertificateChain.add((X509Certificate) cert);
                            }

                            Key key = keyStore.getKey(alias,
                                    x509KeyStore.alias == null ? new char[0] : x509KeyStore.alias.toCharArray());
                            if (!(key instanceof PrivateKey)) {
                                throw new MultiParameterException(cmd,
                                        String.format("Keystore alias [%s] must have a PrivateKey entry, was %s",
                                                x509KeyStore.alias, key == null ? "null" : key.getClass().getName()),
                                        "-ksa");
                            }
                            this.privateKey = (PrivateKey) key;
                            this.certChain = x509CertificateChain
                                    .toArray(new X509Certificate[x509CertificateChain.size()]);
                        }
                    }
                    this.trustStore = trustedCertificates;
                }
            } catch (KeyStoreException | IOException | NoSuchAlgorithmException | UnrecoverableKeyException
                    | CertificateException | IllegalArgumentException e) {
                throw new MultiParameterException(cmd, "Unable to load data from keystore", e, "-ks", "-ksp", "-kst",
                        "-ksa", "-ksap");
            }
        } else {
            super.build(cmd);
        }
    }
}
