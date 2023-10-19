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

import org.eclipse.leshan.client.californium.endpoint.ClientProtocolProvider;
import org.eclipse.leshan.client.californium.endpoint.coap.CoapOscoreProtocolProvider;
import org.eclipse.leshan.client.demo.cli.LeshanClientDemoCLI;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParseResult;

/**
 * To add coap support.
 */
@Command(name = "coap", description = "Activate coap support.")
public class CoapCommand implements Runnable, ProtocolProviderFactory, DefaultProtocolProviderFactory {

    @Option(names = { "-h", "--help" }, description = "Display help information.", usageHelp = true)
    private boolean help;

    @Override
    public void run() {
        // nothing todo, some validation could be added here if needed.
    }

    @Override
    public ClientProtocolProvider create(LeshanClientDemoCLI cli, ParseResult result) {
        return new CoapOscoreProtocolProvider();
    }

    @Override
    public ClientProtocolProvider create(LeshanClientDemoCLI cli, CommandLine cmdLine) {
        return new CoapOscoreProtocolProvider();
    }
}
