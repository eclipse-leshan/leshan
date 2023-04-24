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
import org.eclipse.leshan.transport.javacoap.client.endpoint.JavaCoapClientEndpointsProvider;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParseResult;

/**
 * To add coap support.
 */
@Command(name = "coap", description = "Activate coap support.")
public class CoapCommand implements Runnable, EndpointProviderFactory, DefaultEndpointProviderFactory {

    @Option(names = { "-h", "--help" }, description = "Display help information.", usageHelp = true)
    private boolean help;

    @Override
    public void run() {
        // nothing todo, some validation could be added here if needed.
    }

    @Override
    public LwM2mClientEndpointsProvider create(LeshanClientDemoCLI cli, ParseResult result) {
        return new JavaCoapClientEndpointsProvider();
    }

    @Override
    public LwM2mClientEndpointsProvider createDefault(LeshanClientDemoCLI cli, CommandLine commandLine) {
        return new JavaCoapClientEndpointsProvider();
    }
}
