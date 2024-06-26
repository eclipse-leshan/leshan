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
package org.eclipse.leshan.demo.server.cli;

import java.net.URI;

import org.eclipse.leshan.demo.cli.StandardHelpOptions;
import org.eclipse.leshan.demo.cli.VersionProvider;
import org.eclipse.leshan.demo.cli.converters.PortConverter;
import org.eclipse.leshan.demo.servers.cli.DtlsSection;
import org.eclipse.leshan.demo.servers.cli.GeneralSection;
import org.eclipse.leshan.demo.servers.cli.IdentitySection;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import redis.clients.jedis.JedisPool;

/**
 * This is the class defining the Command Line Interface of Leshan Server Demo.
 */
@Command(name = "leshan-demo-server",
         sortOptions = false,
         description = "%n"//
                 + "@|italic " //
                 + "This is a LWM2M Server demo implemented with Leshan library.%n" //
                 + "You can launch it without any option.%n" //
                 + "%n" //
                 + "Californium is used as CoAP library and some CoAP parameters can be tweaked in 'Californium.properties' file." //
                 + "|@%n%n",
         versionProvider = VersionProvider.class)
public class LeshanServerDemoCLI implements Runnable {

    @Mixin
    public StandardHelpOptions helpsOptions;

    /* ********************************** General Section ******************************** */
    @ArgGroup(validate = false, heading = "%n")
    public ServerGeneralSection main = new ServerGeneralSection();

    public static class ServerGeneralSection extends GeneralSection {
        @Option(names = { "-jh", "--java-coap-host" },
                description = { //
                        "Set the local CoAP address of endpoint based on java-coap library.", //
                        "Default: any local address." })
        public String jlocalAddress;

        @Option(names = { "-jp", "--java-coap-port" },
                description = { //
                        "Set the local CoAP port of endpoint based on java-coap library.", //
                        "Default: ${DEFAULT-VALUE}" },
                converter = PortConverter.class)
        public Integer jlocalPort = 5685;

        @Option(names = { "-th", "--java-coap-tcp-host" },
                description = { //
                        "Set the local CoAP over TCP address of endpoint based on java-coap library.", //
                        "Default: any local address." })
        public String jTcpLocalAddress;

        @Option(names = { "-tp", "--java-coap-tcp-port" },
                description = { //
                        "Set the local CoAP over TCP port of endpoint based on java-coap library.", //
                        "Default: ${DEFAULT-VALUE}" },
                converter = PortConverter.class)
        public Integer jTcpLocalPort = 5683;

        @Option(names = { "-tsh", "--java-coaps-tcp-host" },
                description = { //
                        "Set the local CoAP over TLS address of endpoint based on java-coap library.", //
                        "Default: any local address." })
        public String jTlsLocalAddress;

        @Option(names = { "-tsp", "--java-coaps-tcp-port" },
                description = { //
                        "Set the local CoAP over TLS port of endpoint based on java-coap library.", //
                        "Default: ${DEFAULT-VALUE}" },
                converter = PortConverter.class)
        public Integer jTlsLocalPort = 5684;

        @Option(names = { "-r", "--redis" },
                description = { //
                        "Use redis to store registration and securityInfo.", //
                        "The URL of the redis server should be given using this format :", //
                        "     redis://:password@hostname:port/db_number", //
                        "Example without DB and password: ", //
                        "   redis://localhost:6379", //
                        "Default: redis is not used." },
                converter = JedisPoolConverter.class)
        public JedisPool redis;

        private static class JedisPoolConverter implements ITypeConverter<JedisPool> {
            @Override
            public JedisPool convert(String value) throws Exception {
                return new JedisPool(new URI(value));
            }
        }

        @Option(names = { "-mdns", "--publish-DNS-SD-services" },
                description = { //
                        "Publish leshan's services to DNS Service discovery." })
        public Boolean mdns;

        @Option(names = { "-no", "--disable-oscore" },
                description = { //
                        "Disable experimental OSCORE feature." })
        public Boolean disableOscore = false;
    }

    /* ********************************** DTLS Section ******************************** */
    @ArgGroup(validate = false,
              heading = "%n@|bold,underline DTLS Options|@ %n%n"//
                      + "@|italic " //
                      + "Here some options aiming to configure the server behavior when it uses CoAP over DTLS." //
                      + "%n" //
                      + "Scandium is used as DTLS library and some DTLS parameters can be tweaked in 'Californium.properties' file." //
                      + "|@%n%n")
    public DtlsSection dtls = new DtlsSection();

    /* ********************************** Identity Section ******************************** */
    @ArgGroup(exclusive = true)
    public IdentitySection identity = new IdentitySection();

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        // Some post-validation which imply several options or input consolidation.
        identity.build(spec.commandLine());
    }
}
