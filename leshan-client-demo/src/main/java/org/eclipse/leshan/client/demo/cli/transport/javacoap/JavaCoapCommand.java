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
package org.eclipse.leshan.client.demo.cli.transport.javacoap;

import org.eclipse.leshan.client.demo.cli.LeshanClientDemoCLI;
import org.eclipse.leshan.client.demo.cli.transport.DefaultEndpointProviderFactory;
import org.eclipse.leshan.client.demo.cli.transport.EndpointProviderFactory;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpointsProvider;
import org.eclipse.leshan.core.demo.cli.PicocliUtil;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParseResult;

@Command(name = "java-coap",
         description = { "Configure transport based on java-coap.", //
                 "%n"//
                         + "@|italic Example :|@%n" //
                         + JavaCoapCommand.COMMON_USAGE //
         },
         subcommands = { CoapCommand.class })
public class JavaCoapCommand implements Runnable, EndpointProviderFactory {

    public static final String COMMON_USAGE = "coap support based on java-coap library : %n" //
            + "@|bold transport java-coap coap|@%n";

    @Option(names = { "-h", "--help" }, description = "Display help information.", usageHelp = true)
    private boolean help;

    @Override
    public void run() {
        // nothing todo, some validation could be added here if needed.
    }

    @Override
    public LwM2mClientEndpointsProvider create(LeshanClientDemoCLI cli, ParseResult result) {
        // if one sub-command is called
        if (result.hasSubcommand()) {
            // java-coap command support only 1 sub-command call (subcommandsRepeatable = false)
            EndpointProviderFactory providerFactory = (EndpointProviderFactory) result.subcommand().commandSpec()
                    .userObject();
            return providerFactory.create(cli, result.subcommand());
        } else {
            // no sub-command is called we just create default one provider for java coap.
            return PicocliUtil.reduceTo(result.commandSpec().commandLine(), DefaultEndpointProviderFactory.class, //
                    (cmd, endpointProviderFactory) -> endpointProviderFactory.createDefault(cli, cmd));
        }
    }
}
