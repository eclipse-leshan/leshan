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
package org.eclipse.leshan.server.demo.transport;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.leshan.core.demo.cli.PicocliUtil;
import org.eclipse.leshan.server.demo.cli.LeshanServerDemoCLI;
import org.eclipse.leshan.server.demo.transport.californium.CaliforniumCommand;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpointsProvider;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "transport",
         description = { "Configure Leshan transport layer. Can be used to define available server endpoints.", //
                 "%n"//
                         + CaliforniumCommand.DEFAULT_DESCRIPTION //
                         + CaliforniumCommand.DEFAULT_COMMAND_DESCRIPTION //
                         + "%n" //
                         + "@|italic More examples :|@%n" //
                         + CaliforniumCommand.COMMON_USAGE //
                         + "%n" //
                         + "coaps support based on Californium library and coap support based java-coap library: %n" //
                         + "@|bold transport californium coaps java-coap coap|@%n"//
                         + "%n" //
                         + "Launch @|bold transport [californium|java-coap] -h|@ for more details.%n" //
         },
         subcommandsRepeatable = true,
         subcommands = { CaliforniumCommand.class })
public class TransportCommand implements Runnable {

    @Option(names = { "-h", "--help" }, description = "Display help information.", usageHelp = true)
    private boolean help;

    @Override
    public void run() {
    }

    public List<LwM2mServerEndpointsProvider> createEndpointsProviders(LeshanServerDemoCLI cli,
            CommandLine commandLine) {

        // if transport sub-command is called
        if (commandLine.getParseResult() != null && commandLine.getParseResult().hasSubcommand()) {
            List<LwM2mServerEndpointsProvider> providers = new ArrayList<>();

            // add provider from call sub-commands
            PicocliUtil.applyTo(commandLine.getParseResult(), EndpointProviderFactory.class, //
                    (parseResult, endpointProviderFactory) -> {
                        providers.add(endpointProviderFactory.create(cli, parseResult));
                    });
            return providers;
        }
        // else create default providers
        else {
            List<LwM2mServerEndpointsProvider> providers = new ArrayList<>();
            PicocliUtil.applyTo(commandLine, DefaultEndpointProviderFactory.class, //
                    (cmd, endpointProviderFactory) -> {
                        providers.add(endpointProviderFactory.createDefault(cli, cmd));
                    });
            return providers;
        }
    }
}
