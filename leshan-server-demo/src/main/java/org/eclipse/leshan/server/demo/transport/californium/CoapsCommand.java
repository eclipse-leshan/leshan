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

package org.eclipse.leshan.server.demo.transport.californium;

import java.net.URI;

import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.leshan.core.californium.PrincipalMdcConnectionListener;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumServerEndpointFactory;
import org.eclipse.leshan.server.californium.endpoint.ServerProtocolProvider;
import org.eclipse.leshan.server.californium.endpoint.coaps.CoapsServerEndpointFactoryBuilder;
import org.eclipse.leshan.server.californium.endpoint.coaps.CoapsServerProtocolProvider;
import org.eclipse.leshan.server.demo.cli.LeshanServerDemoCLI;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParseResult;

/**
 * To add coap support.
 */
@Command(name = "coaps",
         description = "Activate coaps support.",
         subcommandsRepeatable = true,
         subcommands = { CoapAddCommand.class })
public class CoapsCommand implements Runnable, ProtocolProviderFactory, DefaultProtocolProviderFactory {

    @Option(names = { "-h", "--help" }, description = "Display help information.", usageHelp = true)
    private boolean help;

    @Override
    public void run() {
        // nothing todo, some validation could be added here if needed.
    }

    @Override
    public ServerProtocolProvider create(LeshanServerDemoCLI cli, ParseResult result) {
        return create(cli);
    }

    @Override
    public ServerProtocolProvider create(LeshanServerDemoCLI cli, CommandLine cmdLine) {
        return create(cli);
    }

    private ServerProtocolProvider create(LeshanServerDemoCLI cli) {
        return new CoapsServerProtocolProvider() {

            @Override
            public void applyDefaultValue(Configuration configuration) {
                super.applyDefaultValue(configuration);
                // These configuration values are always overwritten by CLI therefore set them to transient.
                configuration.setTransient(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY);
                configuration.setTransient(DtlsConfig.DTLS_CONNECTION_ID_LENGTH);
                configuration.set(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, !cli.dtls.supportDeprecatedCiphers);
                configuration.set(DtlsConfig.DTLS_CONNECTION_ID_LENGTH, cli.dtls.cid);
            }

            @Override
            public CaliforniumServerEndpointFactory createDefaultEndpointFactory(URI uri) {
                return new CoapsServerEndpointFactoryBuilder().setURI(uri).setDtlsConnectorConfig(b -> {
                    // Add MDC for connection logs
                    if (cli.helpsOptions.getVerboseLevel() > 0)
                        b.setConnectionListener(new PrincipalMdcConnectionListener());
                }).build();
            }
        };
    }
}
