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
 *     Bosch Software Innovations - added Redis URL support with authentication
 *     Firis SA - added mDNS services registering
 *******************************************************************************/
package org.eclipse.leshan.demo.server;

import java.io.File;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.CertPathUtil;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.leshan.core.endpoint.DefaultEndPointUriHandler;
import org.eclipse.leshan.core.endpoint.EndPointUriHandler;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.demo.LwM2mDemoConstant;
import org.eclipse.leshan.demo.cli.ShortErrorMessageHandler;
import org.eclipse.leshan.demo.server.cli.LeshanServerDemoCLI;
import org.eclipse.leshan.demo.server.servlet.ClientServlet;
import org.eclipse.leshan.demo.server.servlet.EventServlet;
import org.eclipse.leshan.demo.server.servlet.ObjectSpecServlet;
import org.eclipse.leshan.demo.servers.json.servlet.SecurityServlet;
import org.eclipse.leshan.demo.servers.json.servlet.ServerServlet;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.LeshanServerBuilder;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.VersionedModelProvider;
import org.eclipse.leshan.server.redis.RedisRegistrationStore;
import org.eclipse.leshan.server.redis.RedisSecurityStore;
import org.eclipse.leshan.servers.security.EditableSecurityStore;
import org.eclipse.leshan.servers.security.FileSecurityStore;
import org.eclipse.leshan.transport.californium.PrincipalMdcConnectionListener;
import org.eclipse.leshan.transport.californium.server.endpoint.CaliforniumServerEndpointsProvider;
import org.eclipse.leshan.transport.californium.server.endpoint.coap.CoapOscoreServerEndpointFactory;
import org.eclipse.leshan.transport.californium.server.endpoint.coap.CoapServerProtocolProvider;
import org.eclipse.leshan.transport.californium.server.endpoint.coaps.CoapsServerProtocolProvider;
import org.eclipse.leshan.transport.javacoap.server.coaps.bc.endpoint.JavaCoapsServerEndpointsProvider;
import org.eclipse.leshan.transport.javacoap.server.coaptcp.endpoint.JavaCoapTcpServerEndpointsProvider;
import org.eclipse.leshan.transport.javacoap.server.coaptcp.endpoint.JavaCoapsTcpServerEndpointsProvider;
import org.eclipse.leshan.transport.javacoap.server.endpoint.JavaCoapServerEndpointsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;

public class LeshanServerDemo {

    static {
        // Define a default logback.configurationFile
        String property = System.getProperty("logback.configurationFile");
        if (property == null) {
            System.setProperty("logback.configurationFile", "logback-config.xml");
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(LeshanServerDemo.class);
    private static final String CF_CONFIGURATION_FILENAME = "Californium3.server.properties";
    private static final String CF_CONFIGURATION_HEADER = "Leshan Server Demo - " + Configuration.DEFAULT_HEADER;
    private static final EndPointUriHandler uriHandler = new DefaultEndPointUriHandler();

    public static void main(String[] args) {

        // Parse command line
        LeshanServerDemoCLI cli = new LeshanServerDemoCLI();
        CommandLine command = new CommandLine(cli).setParameterExceptionHandler(new ShortErrorMessageHandler());
        // Handle exit code error
        int exitCode = command.execute(args);
        if (exitCode != 0)
            System.exit(exitCode);
        // Handle help or version command
        if (command.isUsageHelpRequested() || command.isVersionHelpRequested())
            System.exit(0);

        try {
            // Create LWM2M Server
            LeshanServer lwm2mServer = createLeshanServer(cli);

            // Create Web Server
            Server webServer = createJettyServer(cli, lwm2mServer);

            // Register a service to DNS-SD
            if (cli.main.mdns != null) {

                // Create a JmDNS instance
                JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());

                // Publish Leshan HTTP Service
                ServiceInfo httpServiceInfo = ServiceInfo.create("_http._tcp.local.", "leshan", cli.main.webPort, "");
                jmdns.registerService(httpServiceInfo);

                // Publish Leshan CoAP Service
                ServiceInfo coapServiceInfo = ServiceInfo.create("_coap._udp.local.", "leshan", cli.main.localPort, "");
                jmdns.registerService(coapServiceInfo);

                // Publish Leshan Secure CoAP Service
                ServiceInfo coapSecureServiceInfo = ServiceInfo.create("_coaps._udp.local.", "leshan",
                        cli.main.secureLocalPort, "");
                jmdns.registerService(coapSecureServiceInfo);
            }

            // Start servers
            lwm2mServer.start();
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

    public static LeshanServer createLeshanServer(LeshanServerDemoCLI cli) throws Exception {
        // Prepare LWM2M server
        LeshanServerBuilder builder = new LeshanServerBuilder();

        // Define model provider
        List<ObjectModel> models = ObjectLoader.loadAllDefault();
        models.addAll(ObjectLoader.loadDdfResources("/models/", LwM2mDemoConstant.modelPaths));
        if (cli.main.modelsFolder != null) {
            models.addAll(ObjectLoader.loadObjectsFromDir(cli.main.modelsFolder, true));
        }
        LwM2mModelProvider modelProvider = new VersionedModelProvider(models);
        builder.setObjectModelProvider(modelProvider);

        // Set securityStore & registrationStore
        EditableSecurityStore securityStore;
        if (cli.main.redis == null) {
            // use file persistence
            securityStore = new FileSecurityStore();
        } else {
            // use Redis Store
            securityStore = new RedisSecurityStore(cli.main.redis);
            builder.setRegistrationStore(new RedisRegistrationStore(cli.main.redis));
        }
        builder.setSecurityStore(securityStore);

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
        // Create Server Endpoints Provider
        CaliforniumServerEndpointsProvider.Builder endpointsBuilder = new CaliforniumServerEndpointsProvider.Builder(
                // Add coap Protocol support
                new CoapServerProtocolProvider(),

                // Add coaps protocol support
                new CoapsServerProtocolProvider(c -> {
                    // Add MDC for connection logs
                    if (cli.helpsOptions.getVerboseLevel() > 0)
                        c.setConnectionListener(new PrincipalMdcConnectionListener());

                }));

        // Create Californium Configuration
        Configuration serverCoapConfig = endpointsBuilder.createDefaultConfiguration();

        // Set some DTLS stuff
        // These configuration values are always overwritten by CLI therefore set them to transient.
        serverCoapConfig.setTransient(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY);
        serverCoapConfig.set(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, !cli.dtls.supportDeprecatedCiphers);
        serverCoapConfig.setTransient(DtlsConfig.DTLS_CONNECTION_ID_LENGTH);
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

        // Enforce DTLS role to ServerOnly if needed
        if (cli.identity.isx509()) {
            X509Certificate serverCertificate = cli.identity.getCertChain()[0];
            if (serverCoapConfig.get(DtlsConfig.DTLS_ROLE) == DtlsRole.BOTH) {
                if (serverCertificate != null) {
                    if (CertPathUtil.canBeUsedForAuthentication(serverCertificate, false)) {
                        if (!CertPathUtil.canBeUsedForAuthentication(serverCertificate, true)) {
                            serverCoapConfig.set(DtlsConfig.DTLS_ROLE, DtlsRole.SERVER_ONLY);
                            LOG.warn("Server certificate does not allow Client Authentication usage."
                                    + "\nThis will prevent this LWM2M server to initiate DTLS connection."
                                    + "\nSee : https://github.com/eclipse/leshan/wiki/Server-Failover#about-connections");
                        }
                    }
                }
            }
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
            endpointsBuilder.addEndpoint(
                    new CoapOscoreServerEndpointFactory(uriHandler.createUri(Protocol.COAP.getUriScheme(), coapAddr)));
        }

        // Create CoAP over DTLS endpoint
        int coapsPort = cli.main.secureLocalPort == null ? serverCoapConfig.get(CoapConfig.COAP_SECURE_PORT)
                : cli.main.secureLocalPort;
        InetSocketAddress coapsAddr = cli.main.secureLocalAddress == null ? new InetSocketAddress(coapsPort)
                : new InetSocketAddress(cli.main.secureLocalAddress, coapsPort);
        endpointsBuilder.addEndpoint(coapsAddr, Protocol.COAPS);

        // Create CoAP endpoint based on java-coap
        int jcoapPort = cli.main.jlocalPort;
        InetSocketAddress jcoapAddr = cli.main.jlocalAddress == null ? new InetSocketAddress(jcoapPort)
                : new InetSocketAddress(cli.main.jlocalAddress, jcoapPort);
        JavaCoapServerEndpointsProvider javacoapEndpointsProvider = new JavaCoapServerEndpointsProvider(jcoapAddr);

        // Create CoAP over endpoint based on java-coap
        int jcoapsPort = cli.main.jCoapsLocalPort;
        InetSocketAddress jcoapsAddr = cli.main.jCoapsLocalAddess == null ? new InetSocketAddress(jcoapsPort)
                : new InetSocketAddress(cli.main.jlocalAddress, jcoapPort);
        JavaCoapsServerEndpointsProvider javacoapsEndpointsProvider = new JavaCoapsServerEndpointsProvider(jcoapsAddr);

        // Create CoAP over TCP endpoint based on java-coap
        int coapTcpPort = cli.main.jTcpLocalPort;
        InetSocketAddress coapTcpAddr = cli.main.jTcpLocalAddress == null ? new InetSocketAddress(coapTcpPort)
                : new InetSocketAddress(cli.main.jTcpLocalAddress, coapTcpPort);
        JavaCoapTcpServerEndpointsProvider javacoapTcpEndpointsProvider = new JavaCoapTcpServerEndpointsProvider(
                coapTcpAddr);

        // Create CoAP over TLS endpoint based on java-coap
        int coapsTcpPort = cli.main.jTlsLocalPort;
        InetSocketAddress coapsTcpAddr = cli.main.jTlsLocalAddress == null ? new InetSocketAddress(coapsTcpPort)
                : new InetSocketAddress(cli.main.jTlsLocalAddress, coapTcpPort);
        JavaCoapsTcpServerEndpointsProvider javacoapsTcpEndpointsProvider = new JavaCoapsTcpServerEndpointsProvider(
                coapsTcpAddr);

        // Create LWM2M server
        builder.setEndpointsProviders(endpointsBuilder.build(), javacoapEndpointsProvider, javacoapsEndpointsProvider,
                javacoapTcpEndpointsProvider, javacoapsTcpEndpointsProvider);
        return builder.build();
    }

    private static Server createJettyServer(LeshanServerDemoCLI cli, LeshanServer lwServer) {
        // Now prepare Jetty
        InetSocketAddress jettyAddr;
        if (cli.main.webhost == null) {
            jettyAddr = new InetSocketAddress(cli.main.webPort);
        } else {
            jettyAddr = new InetSocketAddress(cli.main.webhost, cli.main.webPort);
        }
        Server server = new Server(jettyAddr);
        ServletContextHandler root = new ServletContextHandler("/", true, false);
        server.setHandler(root);

        // Create static Servlet
        DefaultServlet staticServelt = new DefaultServlet();
        ServletHolder staticHolder = new ServletHolder(staticServelt);
        staticHolder.setInitParameter("baseResource",
                LeshanServerDemo.class.getClassLoader().getResource("webapp").toExternalForm());
        staticHolder.setInitParameter("gzip", "true");
        staticHolder.setInitParameter("cacheControl", "public, max-age=31536000");
        root.addServlet(staticHolder, "/");

        // Create REST API Servlets
        EventServlet eventServlet = new EventServlet(lwServer);
        ServletHolder eventServletHolder = new ServletHolder(eventServlet);
        root.addServlet(eventServletHolder, "/api/event/*");

        ServletHolder clientServletHolder = new ServletHolder(new ClientServlet(lwServer, eventServlet));
        root.addServlet(clientServletHolder, "/api/clients/*");

        ServletHolder securityServletHolder;
        if (cli.identity.isRPK()) {
            securityServletHolder = new ServletHolder(new SecurityServlet(
                    (EditableSecurityStore) lwServer.getSecurityStore(), cli.identity.getPublicKey()));
        } else {
            securityServletHolder = new ServletHolder(new SecurityServlet(
                    (EditableSecurityStore) lwServer.getSecurityStore(), cli.identity.getCertChain()[0]));
        }
        root.addServlet(securityServletHolder, "/api/security/*");

        ServletHolder serverServletHolder;
        if (cli.identity.isRPK()) {
            serverServletHolder = new ServletHolder(
                    new ServerServlet(lwServer.getEndpoints(), cli.identity.getPublicKey()));
        } else {
            serverServletHolder = new ServletHolder(
                    new ServerServlet(lwServer.getEndpoints(), cli.identity.getCertChain()[0]));
        }
        root.addServlet(serverServletHolder, "/api/server/*");

        ServletHolder objectSpecServletHolder = new ServletHolder(
                new ObjectSpecServlet(lwServer.getModelProvider(), lwServer.getRegistrationService()));
        root.addServlet(objectSpecServletHolder, "/api/objectspecs/*");

        return server;
    }
}
