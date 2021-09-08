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
package org.eclipse.leshan.server.demo;

import java.io.File;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.config.NetworkConfig.Keys;
import org.eclipse.californium.elements.util.CertPathUtil;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.SingleNodeConnectionIdGenerator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.leshan.core.LwM2m;
import org.eclipse.leshan.core.demo.LwM2mDemoConstant;
import org.eclipse.leshan.core.demo.cli.ShortErrorMessageHandler;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.demo.cli.LeshanServerDemoCLI;
import org.eclipse.leshan.server.demo.servlet.ClientServlet;
import org.eclipse.leshan.server.demo.servlet.EventServlet;
import org.eclipse.leshan.server.demo.servlet.ObjectSpecServlet;
import org.eclipse.leshan.server.demo.servlet.SecurityServlet;
import org.eclipse.leshan.server.demo.servlet.ServerServlet;
import org.eclipse.leshan.server.demo.utils.MagicLwM2mValueConverter;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.VersionedModelProvider;
import org.eclipse.leshan.server.redis.RedisRegistrationStore;
import org.eclipse.leshan.server.redis.RedisSecurityStore;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.FileSecurityStore;
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

        // use a magic converter to support bad type send by the UI.
        builder.setEncoder(new DefaultLwM2mEncoder(new MagicLwM2mValueConverter()));

        // Create CoAP Config
        NetworkConfig coapConfig;
        File configFile = new File(NetworkConfig.DEFAULT_FILE_NAME);
        if (configFile.isFile()) {
            coapConfig = new NetworkConfig();
            coapConfig.load(configFile);
        } else {
            coapConfig = LeshanServerBuilder.createDefaultNetworkConfig();
            coapConfig.store(configFile);
        }
        builder.setCoapConfig(coapConfig);

        // ports from CoAP Config if needed
        builder.setLocalAddress(cli.main.localAddress,
                cli.main.localPort == null ? coapConfig.getInt(Keys.COAP_PORT, LwM2m.DEFAULT_COAP_PORT)
                        : cli.main.localPort);
        builder.setLocalSecureAddress(cli.main.secureLocalAddress,
                cli.main.secureLocalPort == null
                        ? coapConfig.getInt(Keys.COAP_SECURE_PORT, LwM2m.DEFAULT_COAP_SECURE_PORT)
                        : cli.main.secureLocalPort);

        // Create DTLS Config
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
        dtlsConfig.setRecommendedCipherSuitesOnly(!cli.dtls.supportDeprecatedCiphers);
        if (cli.dtls.cid != null) {
            dtlsConfig.setConnectionIdGenerator(new SingleNodeConnectionIdGenerator(cli.dtls.cid));
        }

        if (cli.identity.isx509()) {
            // use X.509 mode (+ RPK)
            builder.setPrivateKey(cli.identity.getPrivateKey());
            builder.setCertificateChain(cli.identity.getCertChain());

            X509Certificate serverCertificate = cli.identity.getCertChain()[0];
            // autodetect serverOnly
            if (serverCertificate != null) {
                if (CertPathUtil.canBeUsedForAuthentication(serverCertificate, false)) {
                    if (!CertPathUtil.canBeUsedForAuthentication(serverCertificate, true)) {
                        dtlsConfig.setServerOnly(true);
                        LOG.warn("Server certificate does not allow Client Authentication usage."
                                + "\nThis will prevent this LWM2M server to initiate DTLS connection."
                                + "\nSee : https://github.com/eclipse/leshan/wiki/Server-Failover#about-connections");
                    }
                }
            }

            // Define trust store
            List<Certificate> trustStore = cli.identity.getTrustStore();
            builder.setTrustedCertificates(trustStore.toArray(new Certificate[trustStore.size()]));
        } else if (cli.identity.isRPK()) {
            // use RPK only
            builder.setPublicKey(cli.identity.getPublicKey());
            builder.setPrivateKey(cli.identity.getPrivateKey());
        }

        // Set DTLS Config
        builder.setDtlsConfig(dtlsConfig);

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

        // Create LWM2M server
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
        /*
         * TODO this should be added again when old demo will be removed.
         * 
         * WebAppContext root = new WebAppContext(); root.setContextPath("/");
         * root.setResourceBase(LeshanServerDemo.class.getClassLoader().getResource("webapp").toExternalForm());
         * root.setParentLoaderPriority(true); server.setHandler(root);
         */

        /* ******** Temporary code to be able to serve both UI ********** */
        ServletContextHandler root = new ServletContextHandler(null, "/", true, false);
        // Configuration for new demo
        DefaultServlet aServlet = new DefaultServlet();
        ServletHolder aHolder = new ServletHolder(aServlet);
        aHolder.setInitParameter("resourceBase",
                LeshanServerDemo.class.getClassLoader().getResource("webapp2").toExternalForm());
        aHolder.setInitParameter("pathInfoOnly", "true");
        root.addServlet(aHolder, "/*");

        // Configuration for old demo
        DefaultServlet bServlet = new DefaultServlet();
        ServletHolder bHolder = new ServletHolder(bServlet);
        bHolder.setInitParameter("resourceBase",
                LeshanServerDemo.class.getClassLoader().getResource("webapp").toExternalForm());
        bHolder.setInitParameter("pathInfoOnly", "true");
        root.addServlet(bHolder, "/old/*");

        server.setHandler(root);
        /* **************************************************************** */

        // Create Servlet
        EventServlet eventServlet = new EventServlet(lwServer, lwServer.getSecuredAddress().getPort());
        ServletHolder eventServletHolder = new ServletHolder(eventServlet);
        root.addServlet(eventServletHolder, "/event/*"); // Temporary code to be able to serve both UI
        root.addServlet(eventServletHolder, "/api/event/*");
        root.addServlet(eventServletHolder, "/old/api/event/*"); // Temporary code to be able to serve both UI

        ServletHolder clientServletHolder = new ServletHolder(new ClientServlet(lwServer));
        root.addServlet(clientServletHolder, "/api/clients/*");
        root.addServlet(clientServletHolder, "/old/api/clients/*");// Temporary code to be able to serve both UI

        ServletHolder securityServletHolder;
        if (cli.identity.isRPK()) {
            securityServletHolder = new ServletHolder(new SecurityServlet(
                    (EditableSecurityStore) lwServer.getSecurityStore(), cli.identity.getPublicKey()));
        } else {
            securityServletHolder = new ServletHolder(new SecurityServlet(
                    (EditableSecurityStore) lwServer.getSecurityStore(), cli.identity.getCertChain()[0]));
        }
        root.addServlet(securityServletHolder, "/api/security/*");
        root.addServlet(securityServletHolder, "/old/api/security/*");// Temporary code to be able to serve both UI

        ServletHolder serverServletHolder;
        if (cli.identity.isRPK()) {
            serverServletHolder = new ServletHolder(new ServerServlet(lwServer, cli.identity.getPublicKey()));
        } else {
            serverServletHolder = new ServletHolder(new ServerServlet(lwServer, cli.identity.getCertChain()[0]));
        }
        root.addServlet(serverServletHolder, "/api/server/*");
        root.addServlet(serverServletHolder, "/old/api/server/*");// Temporary code to be able to serve both UI

        ServletHolder objectSpecServletHolder = new ServletHolder(
                new ObjectSpecServlet(lwServer.getModelProvider(), lwServer.getRegistrationService()));
        root.addServlet(objectSpecServletHolder, "/api/objectspecs/*");
        root.addServlet(objectSpecServletHolder, "/old/api/objectspecs/*");// Temporary code to be able to serve both UI

        return server;
    }
}
