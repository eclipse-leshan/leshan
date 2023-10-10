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

import java.net.InetSocketAddress;

import org.eclipse.leshan.core.demo.cli.converters.PortConverter;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumServerEndpointsProvider;

import picocli.CommandLine.Option;

public abstract class AbstractAddCommand implements Runnable, EndpointAdder {

    @Option(names = { "-lh", "--address" },
            description = { //
                    "Set the local address of the endpoint.", //
                    "Default: any local address." })
    public String localAddress;

    @Option(names = { "-lp", "--port" },
            description = { //
                    "Set the local port of the endpoint.", //
                    "Default: Port value from 'Californium.properties' file or ${DEFAULT-VALUE} if absent" },
            converter = PortConverter.class)
    public Integer localPort;

    @Option(names = { "-h", "--help" }, description = "Display help information.", usageHelp = true)
    private boolean help;

    public AbstractAddCommand(Integer defaultPort) {
        this.localPort = defaultPort;
    }

    @Override
    public void run() {
        // nothing todo, some validation could be added here if needed.
    }

    @Override
    public void addEndpointTo(CaliforniumServerEndpointsProvider.Builder provider) {
        // int coapPort = cli.main.localPort == null ? serverCoapConfig.get(CoapConfig.COAP_PORT) : cli.main.localPort;

        InetSocketAddress addr = localAddress == null ? new InetSocketAddress(localPort)
                : new InetSocketAddress(localAddress, localPort);
//        if (cli.main.disableOscore) {
//            endpointsBuilder.addEndpoint(coapAddr, Protocol.COAP);
//        } else {
//            endpointsBuilder.addEndpoint(new CoapOscoreServerEndpointFactory(
//                    EndpointUriUtil.createUri(Protocol.COAP.getUriScheme(), coapAddr)));
//        }

        provider.addEndpoint(addr, getProtocol());
    }

    protected abstract Protocol getProtocol();
}
