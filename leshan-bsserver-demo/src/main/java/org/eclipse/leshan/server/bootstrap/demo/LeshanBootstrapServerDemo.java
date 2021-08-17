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
import java.security.cert.Certificate;
import java.util.List;

import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.config.NetworkConfig.Keys;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.SingleNodeConnectionIdGenerator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.leshan.core.LwM2m;
import org.eclipse.leshan.core.demo.cli.ShortErrorMessageHandler;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.server.bootstrap.EditableBootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.demo.cli.LeshanBsServerDemoCLI;
import org.eclipse.leshan.server.bootstrap.demo.servlet.BootstrapServlet;
import org.eclipse.leshan.server.bootstrap.demo.servlet.EventServlet;
import org.eclipse.leshan.server.bootstrap.demo.servlet.ServerServlet;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServerBuilder;
import org.eclipse.leshan.server.model.VersionedBootstrapModelProvider;
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
            // Create configStore
            EditableBootstrapConfigStore bsConfigStore = new JSONFileBootstrapStore(cli.main.configFilename);

            // Create LWM2M Server
            LeshanBootstrapServer lwm2mBsServer = createBsLeshanServer(cli, bsConfigStore);

            // Create Web Server
            Server webServer = createJettyServer(cli, lwm2mBsServer, bsConfigStore);

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
            EditableBootstrapConfigStore bsConfigStore) throws Exception {
        // Prepare LWM2M server
        LeshanBootstrapServerBuilder builder = new LeshanBootstrapServerBuilder();

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

        // Create Models
        List<ObjectModel> models = ObjectLoader.loadDefault();
        if (cli.main.modelsFolder != null) {
            models.addAll(ObjectLoader.loadObjectsFromDir(cli.main.modelsFolder, true));
        }
        builder.setObjectModelProvider(new VersionedBootstrapModelProvider(models));

        // Set security store & config store
        builder.setConfigStore(bsConfigStore);
        builder.setSecurityStore(new BootstrapConfigSecurityStore(bsConfigStore));

        return builder.build();
    }

    private static Server createJettyServer(LeshanBsServerDemoCLI cli, LeshanBootstrapServer bsServer,
            EditableBootstrapConfigStore bsStore) {
        // Now prepare and start jetty
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
         * root.setResourceBase(LeshanBootstrapServerDemo.class.getClassLoader().getResource("webapp").toExternalForm())
         * ; root.setParentLoaderPriority(true);
         */

        /* ******** Temporary code to be able to serve both UI ********** */
        ServletContextHandler root = new ServletContextHandler(null, "/", true, false);
        // Configuration for new demo
        // Configuration for new demo
        DefaultServlet aServlet = new DefaultServlet();
        ServletHolder aHolder = new ServletHolder(aServlet);
        aHolder.setInitParameter("resourceBase",
                LeshanBootstrapServerDemo.class.getClassLoader().getResource("webapp2").toExternalForm());
        aHolder.setInitParameter("pathInfoOnly", "true");
        root.addServlet(aHolder, "/*");

        // Configuration for old demo
        DefaultServlet bServlet = new DefaultServlet();
        ServletHolder bHolder = new ServletHolder(bServlet);
        bHolder.setInitParameter("resourceBase",
                LeshanBootstrapServerDemo.class.getClassLoader().getResource("webapp").toExternalForm());
        bHolder.setInitParameter("pathInfoOnly", "true");
        root.addServlet(bHolder, "/old/*");
        /* **************************************************************** */

        ServletHolder bsServletHolder = new ServletHolder(new BootstrapServlet(bsStore));
        root.addServlet(bsServletHolder, "/old/api/bootstrap/*"); // Temporary code to be able to serve both UI
        root.addServlet(bsServletHolder, "/api/bootstrap/*");

        ServletHolder serverServletHolder;
        if (cli.identity.isRPK()) {
            serverServletHolder = new ServletHolder(new ServerServlet(bsServer, cli.identity.getPublicKey()));
        } else {
            serverServletHolder = new ServletHolder(new ServerServlet(bsServer, cli.identity.getCertChain()[0]));
        }
        root.addServlet(serverServletHolder, "/old/api/server/*"); // Temporary code to be able to serve both UI
        root.addServlet(serverServletHolder, "/api/server/*");

        EventServlet eventServlet = new EventServlet(bsServer);
        ServletHolder eventServletHolder = new ServletHolder(eventServlet);
        root.addServlet(eventServletHolder, "/old/event/*"); // Temporary code to be able to serve both UI
        root.addServlet(eventServletHolder, "/api/event/*");
        root.addServlet(eventServletHolder, "/old/api/event/*"); // Temporary code to be able to serve both UI

        server.setHandler(root);
        /* **************************************************************** */

        return server;
    }
}
