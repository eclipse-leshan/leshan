/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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

package org.eclipse.leshan.client.demo.cli.transport.californium;

import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpointFactory;
import org.eclipse.leshan.client.californium.endpoint.ClientProtocolProvider;
import org.eclipse.leshan.client.californium.endpoint.coaps.CoapsClientEndpointFactory;
import org.eclipse.leshan.client.californium.endpoint.coaps.CoapsClientProtocolProvider;
import org.eclipse.leshan.client.demo.cli.LeshanClientDemoCLI;
import org.eclipse.leshan.core.californium.PrincipalMdcConnectionListener;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParseResult;

/**
 * To add coap support.
 */
@Command(name = "coaps", description = "Activate coaps support.")
public class CoapsCommand implements Runnable, ProtocolProviderFactory, DefaultProtocolProviderFactory {

    @Option(names = { "-h", "--help" }, description = "Display help information.", usageHelp = true)
    private boolean help;

    @Override
    public void run() {
        // nothing todo, some validation could be added here if needed.
    }

    @Override
    public ClientProtocolProvider create(LeshanClientDemoCLI cli, ParseResult result) {
        return create(cli);
    }

    @Override
    public ClientProtocolProvider create(LeshanClientDemoCLI cli, CommandLine cmdLine) {
        return create(cli);
    }

    private ClientProtocolProvider create(LeshanClientDemoCLI cli) {
        return new CoapsClientProtocolProvider() {

            @Override
            public void applyDefaultValue(Configuration configuration) {
                super.applyDefaultValue(configuration);
                // These configuration values are always overwritten by CLI therefore set them to transient.
                configuration.setTransient(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY);
                configuration.setTransient(DtlsConfig.DTLS_CONNECTION_ID_LENGTH);
                configuration.set(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, !cli.dtls.supportDeprecatedCiphers);
                configuration.set(DtlsConfig.DTLS_CONNECTION_ID_LENGTH, cli.dtls.cid);
                if (cli.dtls.ciphers != null) {
                    configuration.set(DtlsConfig.DTLS_CIPHER_SUITES, cli.dtls.ciphers);
                }
            }

            @Override
            public CaliforniumClientEndpointFactory createDefaultEndpointFactory() {
                return new CoapsClientEndpointFactory() {

                    @Override
                    protected DtlsConnectorConfig.Builder createRootDtlsConnectorConfigBuilder(
                            Configuration configuration) {
                        Builder builder = super.createRootDtlsConnectorConfigBuilder(configuration);

                        // Add DTLS Session lifecycle logger
                        builder.setSessionListener(new DtlsSessionLogger());

                        // Add MDC for connection logs
                        if (cli.helpsOptions.getVerboseLevel() > 0)
                            builder.setConnectionListener(new PrincipalMdcConnectionListener());
                        return builder;
                    };
                };
            };
        };
    }
}
