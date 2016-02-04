/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.bootstrap;

import java.net.BindException;
import java.net.InetSocketAddress;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.leshan.bootstrap.servlet.BootstrapServlet;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.LwM2mBootstrapServerImpl;
import org.eclipse.leshan.server.security.SecurityStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BootstrapMain {

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapMain.class);

    private final static String USAGE = "java -jar leshan-bs-server.jar [OPTION]";
    private final static String FOOTER = "All options could be passed using environment variables.(using long option name in uppercase)";

    public static void main(String[] args) {

        // Define options for command line tools
        Options options = new Options();

        options.addOption("h", "help", false, "Display help information.");
        options.addOption("lh", "coaphost", true, "Set the local CoAP address.\n  Default: any local address.");
        options.addOption("lp", "coapport", true, "Set the local CoAP port.\n  Default: 5683.");
        options.addOption("slh", "coapshost", true, "Set the secure local CoAP address.\nDefault: any local address.");
        options.addOption("slp", "coapsport", true, "Set the secure local CoAP port.\nDefault: 5684.");
        options.addOption("wp", "webport", true, "Set the HTTP port for web server.\nDefault: 8080.");
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null);

        // Parse arguments
        CommandLine cl = null;
        try {
            cl = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            System.err.println("Parsing failed.  Reason: " + e.getMessage());
            formatter.printHelp(USAGE, null, options, FOOTER);
            return;
        }

        // Print help
        if (cl.hasOption("help")) {
            formatter.printHelp(USAGE, null, options, FOOTER);
            return;
        }

        // Abort if unexpected options
        if (cl.getArgs().length > 0) {
            System.out.println("Unexpected option or arguments : " + cl.getArgList());
            formatter.printHelp(USAGE, null, options, FOOTER);
            return;
        }

        // Get local address
        String localAddress = System.getenv("COAPHOST");
        if (cl.hasOption("lh")) {
            localAddress = cl.getOptionValue("lh");
        }
        if (localAddress == null)
            localAddress = "0.0.0.0";
        String localPortOption = System.getenv("COAPPORT");
        if (cl.hasOption("lp")) {
            localPortOption = cl.getOptionValue("lp");
        }
        int localPort = LeshanServerBuilder.PORT;
        if (localPortOption != null) {
            localPort = Integer.parseInt(localPortOption);
        }

        // Get secure local address
        String secureLocalAddress = System.getenv("COAPSHOST");
        if (cl.hasOption("slh")) {
            secureLocalAddress = cl.getOptionValue("slh");
        }
        if (secureLocalAddress == null)
            secureLocalAddress = "0.0.0.0";
        String secureLocalPortOption = System.getenv("COAPSPORT");
        if (cl.hasOption("slp")) {
            secureLocalPortOption = cl.getOptionValue("slp");
        }
        int secureLocalPort = LeshanServerBuilder.PORT_DTLS;
        if (secureLocalPortOption != null) {
            secureLocalPort = Integer.parseInt(secureLocalPortOption);
        }

        // Get http port
        String webPortOption = System.getenv("WEBPORT");
        if (cl.hasOption("wp")) {
            webPortOption = cl.getOptionValue("wp");
        }
        int webPort = 8080;
        if (webPortOption != null) {
            webPort = Integer.parseInt(webPortOption);
        }

        try {
            createAndStartServer(webPort, localAddress, localPort, secureLocalAddress, secureLocalPort);
        } catch (BindException e) {
            System.out.println(String.format("Web port %s is alreay used, you could change it using 'webport' option.",
                    webPort));
            formatter.printHelp(USAGE, null, options, FOOTER);
        } catch (Exception e) {
            LOG.error("Jetty stopped with unexcepted error ...", e);
        }
    }

    public static void createAndStartServer(int webPort, String localAddress, int localPort, String secureLocalAddress,
            int secureLocalPort) throws Exception {

        // Prepare and start bootstrap server
        BootstrapStoreImpl bsStore = new BootstrapStoreImpl();
        SecurityStore securityStore = new BootstrapSecurityStore(bsStore);

        LwM2mBootstrapServerImpl bsServer = new LwM2mBootstrapServerImpl(
                new InetSocketAddress(localAddress, localPort), new InetSocketAddress(secureLocalAddress,
                        secureLocalPort), bsStore, securityStore);
        bsServer.start();

        // Now prepare and start jetty
        Server server = new Server(webPort);
        WebAppContext root = new WebAppContext();

        root.setContextPath("/");
        root.setResourceBase(BootstrapMain.class.getClassLoader().getResource("webapp").toExternalForm());
        root.setParentLoaderPriority(true);

        ServletHolder bsServletHolder = new ServletHolder(new BootstrapServlet(bsStore));
        root.addServlet(bsServletHolder, "/api/bootstrap/*");

        server.setHandler(root);

        server.start();
        LOG.info("Web server started at {}.", server.getURI());
    }
}
