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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpointsProvider;
import org.eclipse.leshan.client.californium.endpoint.ClientProtocolProvider;
import org.eclipse.leshan.client.demo.cli.LeshanClientDemoCLI;
import org.eclipse.leshan.client.demo.cli.transport.DefaultEndpointProviderFactory;
import org.eclipse.leshan.client.demo.cli.transport.EndpointProviderFactory;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpointsProvider;
import org.eclipse.leshan.core.demo.cli.PicocliUtil;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParseResult;

@Command(name = "californium",
         description = { "Configure transport based on Californium.", //
                 "%n"//
                         + CaliforniumCommand.DEFAULT_DESCRIPTION //
                         + CaliforniumCommand.DEFAULT_COMMAND_DESCRIPTION //
                         + "%n" //
                         + "@|italic More examples :|@%n" //
                         + CaliforniumCommand.COMMON_USAGE //
         },
         subcommandsRepeatable = true,
         subcommands = { //
                 CoapCommand.class, //
                 CoapsCommand.class })
public class CaliforniumCommand implements Runnable, EndpointProviderFactory, DefaultEndpointProviderFactory {

    // Configuration file constant
    private static final String CF_CONFIGURATION_FILENAME = "Californium3.client.properties";
    private static final String CF_CONFIGURATION_HEADER = "Leshan Client Demo - " + Configuration.DEFAULT_HEADER;

    // Description constant
    public static final String DEFAULT_DESCRIPTION = "By default, Californium library is used for coap/coaps. Some Californium parameters can be tweaked in " //
            + CF_CONFIGURATION_FILENAME + " file." + "%n";
    public static final String DEFAULT_COMMAND = "transport californium coap coaps";
    public static final String DEFAULT_COMMAND_DESCRIPTION = "Default behavior is like using : %n@|bold "
            + DEFAULT_COMMAND + "|@%n";
    public static final String COMMON_USAGE = "coaps support only based on Californium library : %n" //
            + "@|bold transport californium coaps|@%n";

    @Option(names = { "-h", "--help" }, description = "Display help information.", usageHelp = true)
    private boolean help;

    @Override
    public void run() {
        // nothing todo, some validation could be added here if needed.
    }

    @Override
    public LwM2mClientEndpointsProvider create(LeshanClientDemoCLI cli, ParseResult parseResult) {

        // if sub command was called
        if (parseResult.hasSubcommand()) {
            // Get Protocols providers
            List<ClientProtocolProvider> protocolProviders = new ArrayList<>();
            PicocliUtil.applyTo(parseResult, ProtocolProviderFactory.class, (result, protocolProviderFactory) -> {
                protocolProviders.add(protocolProviderFactory.create(cli, result));
            });

            return create(cli, protocolProviders);
        }
        // else create default provider
        else {
            return createDefault(cli, parseResult.commandSpec().commandLine());
        }
    }

    @Override
    public LwM2mClientEndpointsProvider createDefault(LeshanClientDemoCLI cli, CommandLine commandLine) {
        // Create default protocols providers
        List<ClientProtocolProvider> protocolProviders = new ArrayList<>();
        PicocliUtil.applyTo(commandLine, DefaultProtocolProviderFactory.class, (cmdLine, protocolProviderFactory) -> {
            protocolProviders.add(protocolProviderFactory.create(cli, cmdLine));
        });

        return create(cli, protocolProviders);
    }

    private LwM2mClientEndpointsProvider create(LeshanClientDemoCLI cli,
            List<ClientProtocolProvider> protocolProvider) {
        // Create client endpoints Provider
        CaliforniumClientEndpointsProvider.Builder endpointsBuilder = new CaliforniumClientEndpointsProvider.Builder(
                protocolProvider.toArray(new ClientProtocolProvider[protocolProvider.size()]));

        // Create Californium Configuration
        Configuration clientCoapConfig = endpointsBuilder.createDefaultConfiguration();
        // Persist configuration
        File configFile = new File(CF_CONFIGURATION_FILENAME);
        if (configFile.isFile()) {
            clientCoapConfig.load(configFile);
        } else {
            clientCoapConfig.store(configFile, CF_CONFIGURATION_HEADER);
        }
        // Set Californium Configuration
        endpointsBuilder.setConfiguration(clientCoapConfig);

        // define local address
        endpointsBuilder.setClientAddress(cli.main.localAddress);
        return endpointsBuilder.build();
    }
}
