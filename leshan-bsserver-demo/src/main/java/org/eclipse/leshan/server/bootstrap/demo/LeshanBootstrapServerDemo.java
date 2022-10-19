/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - add parameter for
 *                                                     configuration filename
 *******************************************************************************/

package org.eclipse.leshan.server.bootstrap.demo;

import java.io.File;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.cert.Certificate;
import java.util.List;

import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.leshan.core.californium.PrincipalMdcConnectionListener;
import org.eclipse.leshan.core.demo.cli.ShortErrorMessageHandler;
import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.server.bootstrap.EditableBootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.bootstrap.LeshanBootstrapServerBuilder;
import org.eclipse.leshan.server.bootstrap.demo.cli.LeshanBsServerDemoCLI;
import org.eclipse.leshan.server.bootstrap.demo.servlet.BootstrapServlet;
import org.eclipse.leshan.server.bootstrap.demo.servlet.EventServlet;
import org.eclipse.leshan.server.bootstrap.demo.servlet.ServerServlet;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.BootstrapServerProtocolProvider;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.CaliforniumBootstrapServerEndpointFactory;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.CaliforniumBootstrapServerEndpointsProvider;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.coap.CoapBootstrapServerProtocolProvider;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.coap.CoapOscoreBootstrapServerEndpointFactory;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.coaps.CoapsBootstrapServerEndpointFactory;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.coaps.CoapsBootstrapServerProtocolProvider;
import org.eclipse.leshan.server.core.demo.json.servlet.SecurityServlet;
import org.eclipse.leshan.server.model.VersionedBootstrapModelProvider;
import org.eclipse.leshan.server.security.BootstrapSecurityStoreAdapter;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.FileSecurityStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;

public class LeshanBootstrapServerDemo {

    static {
        // Define a default logback.configurationFile
        String property = System.getProperty("logback.configurationFile");
        if (property == null) {
            System.setProperty("logback.configurationFile", "logback-config.xml");
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(LeshanBootstrapServerDemo.class);
    private static final String CF_CONFIGURATION_FILENAME = "Californium3.bsserver.properties";
    private static final String CF_CONFIGURATION_HEADER = "Leshan Bootstrap Server Demo - "
            + Configuration.DEFAULT_HEADER;

    public static void main(String[] args) {

        // Parse command line
        LeshanBsServerDemoCLI cli = new LeshanBsServerDemoCLI();
        CommandLine command = new CommandLine(cli).setParameterExceptionHandler(new ShortErrorMessageHandler());
        // Handle exit code error
        int exitCode = command.execute(args);
        if (exitCode != 0)
            System.exit(exitCode);
        // Handle help or version command
        if (command.isUsageHelpRequested() || command.isVersionHelpRequested())
            System.exit(0);

        try {
            // Create Stores
            EditableBootstrapConfigStore bsConfigStore = new JSONFileBootstrapStore(cli.main.configFilename);
            EditableSecurityStore securityStore = new FileSecurityStore("data/bssecurity.data");

            // Create LWM2M Server
            LeshanBootstrapServer lwm2mBsServer = createBsLeshanServer(cli, bsConfigStore, securityStore);

            // Create Web Server
            Server webServer = createJettyServer(cli, lwm2mBsServer, bsConfigStore, securityStore);

            // Start servers
            lwm2mBsServer.start();
            webServer.start();
            LOG.info("Web server started at {}.", webServer.getURI());

        } catch (Exception e) {

            // Handler Execution Error
            PrintWriter printer = command.getErr();
            printer.print(command.getColorScheme().errorText("Unable to create and start server ..."));
            printer.printf("%n%n");
            printer.print(command.getColorScheme().stackTraceText(e));
            printer.flush();
            System.exit(1);
        }
    }

    public static LeshanBootstrapServer createBsLeshanServer(LeshanBsServerDemoCLI cli,
            EditableBootstrapConfigStore bsConfigStore, EditableSecurityStore securityStore) throws Exception {
        // Prepare LWM2M server
        LeshanBootstrapServerBuilder builder = new LeshanBootstrapServerBuilder();

        // Create Models
        List<ObjectModel> models = ObjectLoader.loadDefault();
        if (cli.main.modelsFolder != null) {
            models.addAll(ObjectLoader.loadObjectsFromDir(cli.main.modelsFolder, true));
        }
        builder.setObjectModelProvider(new VersionedBootstrapModelProvider(models));

        builder.setConfigStore(bsConfigStore);
        builder.setSecurityStore(new BootstrapSecurityStoreAdapter(securityStore));

        if (cli.identity.isx509()) {
            // use X.509 mode (+ RPK)
            builder.setPrivateKey(cli.identity.getPrivateKey());
            builder.setCertificateChain(cli.identity.getCertChain());

            // Define trust store
            List<Certificate> trustStore = cli.identity.getTrustStore();
            builder.setTrustedCertificates(trustStore.toArray(new Certificate[trustStore.size()]));
        } else if (cli.identity.isRPK()) {
            // use RPK only
            builder.setPublicKey(cli.identity.getPublicKey());
            builder.setPrivateKey(cli.identity.getPrivateKey());
        }

        // Create Californium Endpoints Provider:
        // ------------------
        // Create Custom CoAPS protocol provider to add MDC logger :
        BootstrapServerProtocolProvider coapsProtocolProvider = new CoapsBootstrapServerProtocolProvider() {
            @Override
            public CaliforniumBootstrapServerEndpointFactory createDefaultEndpointFactory(URI uri) {
                return new CoapsBootstrapServerEndpointFactory(uri) {

                    @Override
                    protected Builder createDtlsConnectorConfigBuilder(Configuration configuration) {
                        Builder dtlsConfigBuilder = super.createDtlsConnectorConfigBuilder(configuration);

                        // Add MDC for connection logs
                        if (cli.helpsOptions.getVerboseLevel() > 0)
                            dtlsConfigBuilder.setConnectionListener(new PrincipalMdcConnectionListener());

                        return dtlsConfigBuilder;
                    }
                };
            }
        };

        // Create Bootstrap Server Endpoints Provider
        CaliforniumBootstrapServerEndpointsProvider.Builder endpointsBuilder = new CaliforniumBootstrapServerEndpointsProvider.Builder(
                new CoapBootstrapServerProtocolProvider(), coapsProtocolProvider);

        // Create Californium Configuration
        Configuration serverCoapConfig = endpointsBuilder.createDefaultConfiguration();

        // Set some DTLS stuff
        // These configuration values are always overwritten by CLI therefore set them to transient.
        serverCoapConfig.setTransient(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY);
        serverCoapConfig.setTransient(DtlsConfig.DTLS_CONNECTION_ID_LENGTH);
        serverCoapConfig.set(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, !cli.dtls.supportDeprecatedCiphers);
        if (cli.dtls.cid != null) {
            serverCoapConfig.set(DtlsConfig.DTLS_CONNECTION_ID_LENGTH, cli.dtls.cid);
        }

        // Persist configuration
        File configFile = new File(CF_CONFIGURATION_FILENAME);
        if (configFile.isFile()) {
            serverCoapConfig.load(configFile);
        } else {
            serverCoapConfig.store(configFile, CF_CONFIGURATION_HEADER);
        }

        // Set Californium Configuration
        endpointsBuilder.setConfiguration(serverCoapConfig);

        // Create CoAP endpoint
        int coapPort = cli.main.localPort == null ? serverCoapConfig.get(CoapConfig.COAP_PORT) : cli.main.localPort;
        InetSocketAddress coapAddr = cli.main.localAddress == null ? new InetSocketAddress(coapPort)
                : new InetSocketAddress(cli.main.localAddress, coapPort);
        if (cli.main.disableOscore) {
            endpointsBuilder.addEndpoint(coapAddr, Protocol.COAP);
        } else {
            endpointsBuilder.addEndpoint(new CoapOscoreBootstrapServerEndpointFactory(
                    EndpointUriUtil.createUri(Protocol.COAP.getUriScheme(), coapAddr)));
        }

        // Create CoAP over DTLS endpoint
        int coapsPort = cli.main.secureLocalPort == null ? serverCoapConfig.get(CoapConfig.COAP_SECURE_PORT)
                : cli.main.secureLocalPort;
        InetSocketAddress coapsAddr = cli.main.secureLocalAddress == null ? new InetSocketAddress(coapsPort)
                : new InetSocketAddress(cli.main.secureLocalAddress, coapsPort);
        endpointsBuilder.addEndpoint(coapsAddr, Protocol.COAPS);

        // Create LWM2M server
        builder.setEndpointsProvider(endpointsBuilder.build());
        return builder.build();
    }

    private static Server createJettyServer(LeshanBsServerDemoCLI cli, LeshanBootstrapServer bsServer,
            EditableBootstrapConfigStore bsStore, EditableSecurityStore securityStore) {
        // Now prepare and start jetty
        InetSocketAddress jettyAddr;
        if (cli.main.webhost == null) {
            jettyAddr = new InetSocketAddress(cli.main.webPort);
        } else {
            jettyAddr = new InetSocketAddress(cli.main.webhost, cli.main.webPort);
        }
        Server server = new Server(jettyAddr);
        WebAppContext root = new WebAppContext();
        root.setContextPath("/");
        root.setResourceBase(LeshanBootstrapServerDemo.class.getClassLoader().getResource("webapp").toExternalForm());
        root.setParentLoaderPriority(true);

        ServletHolder bsServletHolder = new ServletHolder(new BootstrapServlet(bsStore));
        root.addServlet(bsServletHolder, "/api/bootstrap/*");

        ServletHolder serverServletHolder;
        if (cli.identity.isRPK()) {
            serverServletHolder = new ServletHolder(new ServerServlet(bsServer, cli.identity.getPublicKey()));
        } else {
            serverServletHolder = new ServletHolder(new ServerServlet(bsServer, cli.identity.getCertChain()[0]));
        }
        root.addServlet(serverServletHolder, "/api/server/*");

        ServletHolder securityServletHolder;
        if (cli.identity.isRPK()) {
            securityServletHolder = new ServletHolder(new SecurityServlet(securityStore, cli.identity.getPublicKey()));
        } else {
            securityServletHolder = new ServletHolder(
                    new SecurityServlet(securityStore, cli.identity.getCertChain()[0]));
        }
        root.addServlet(securityServletHolder, "/api/security/*");

        EventServlet eventServlet = new EventServlet(bsServer);
        ServletHolder eventServletHolder = new ServletHolder(eventServlet);
        root.addServlet(eventServletHolder, "/api/event/*");

        server.setHandler(root);

        return server;
    }
}
