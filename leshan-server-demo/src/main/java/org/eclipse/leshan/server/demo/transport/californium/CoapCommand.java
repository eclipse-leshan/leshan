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

import org.eclipse.leshan.server.californium.endpoint.ServerProtocolProvider;
import org.eclipse.leshan.server.californium.endpoint.coap.CoapServerProtocolProvider;
import org.eclipse.leshan.server.demo.cli.LeshanServerDemoCLI;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParseResult;

/**
 * To add coap support.
 */
@Command(name = "coap",
         description = "Activate coap support.",
         subcommandsRepeatable = true,
         subcommands = { CoapAddCommand.class })
public class CoapCommand implements Runnable, ProtocolProviderFactory, DefaultProtocolProviderFactory {

    @Option(names = { "-h", "--help" }, description = "Display help information.", usageHelp = true)
    private boolean help;

    @Override
    public void run() {
        // nothing todo, some validation could be added here if needed.
    }

    @Override
    public ServerProtocolProvider create(LeshanServerDemoCLI cli, ParseResult result) {
        return new CoapServerProtocolProvider();
    }

    @Override
    public ServerProtocolProvider create(LeshanServerDemoCLI cli, CommandLine cmdLine) {
        return new CoapServerProtocolProvider();
    }
}
