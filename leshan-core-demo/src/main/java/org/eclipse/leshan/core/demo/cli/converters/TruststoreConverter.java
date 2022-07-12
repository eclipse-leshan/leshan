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
package org.eclipse.leshan.core.demo.cli.converters;

import java.io.File;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

public class TruststoreConverter implements ITypeConverter<List<Certificate>> {

    private static final Logger LOG = LoggerFactory.getLogger(TruststoreConverter.class);

    @Override
    public List<Certificate> convert(String value) throws Exception {
        List<Certificate> trustStore = new ArrayList<>();

        if (value.startsWith("file://")) {
            // Treat argument as Java trust store
            try {
                Certificate[] trustedCertificates = SslContextUtil.loadTrustedCertificates(value);
                trustStore.addAll(Arrays.asList(trustedCertificates));
            } catch (Exception e) {
                throw new TypeConversionException("Failed to load trust store : " + e.getMessage());
            }
        } else {
            // Treat argument as file or directory
            File input = new File(value);

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

        return trustStore;
    }

    public static List<Certificate> convertValue(String value) throws Exception {
        return new TruststoreConverter().convert(value);
    }
}
