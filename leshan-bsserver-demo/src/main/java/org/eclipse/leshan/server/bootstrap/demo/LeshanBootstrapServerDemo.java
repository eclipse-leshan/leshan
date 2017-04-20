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
 *     Achim Kraus (Bosch Software Innovations GmbH) - add parameter for 
 *                                                     configuration filename
 *******************************************************************************/

package org.eclipse.leshan.server.bootstrap.demo;

import java.io.File;
import java.io.FileInputStream;
import java.net.BindException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.leshan.LwM2m;
import org.eclipse.leshan.SecurityMode;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerSecurity;
import org.eclipse.leshan.server.bootstrap.demo.ConfigurationChecker.ConfigurationException;
import org.eclipse.leshan.server.bootstrap.demo.servlet.BootstrapServlet;
import org.eclipse.leshan.server.californium.LeshanBootstrapServerBuilder;
import org.eclipse.leshan.server.californium.impl.LeshanBootstrapServer;
import org.eclipse.leshan.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeshanBootstrapServerDemo {

    private static final Logger LOG = LoggerFactory.getLogger(LeshanBootstrapServerDemo.class);

    private final static String USAGE = "java -jar leshan-bsserver-demo.jar [OPTION]";

    public static void main(String[] args) {
        // Define options for command line tools
        Options options = new Options();

        options.addOption("h", "help", false, "Display help information.");
        options.addOption("lh", "coaphost", true, "Set the local CoAP address.\n  Default: any local address.");
        options.addOption("lp", "coapport", true,
                String.format("Set the local CoAP port.\n  Default: %d.", LwM2m.DEFAULT_COAP_PORT));
        options.addOption("slh", "coapshost", true, "Set the secure local CoAP address.\nDefault: any local address.");
        options.addOption("slp", "coapsport", true,
                String.format("Set the secure local CoAP port.\nDefault: %d.", LwM2m.DEFAULT_COAP_SECURE_PORT));
        options.addOption("wp", "webport", true, "Set the HTTP port for web server.\nDefault: 8080.");
        options.addOption("m", "modelsfolder", true, "A folder which contains object models in OMA DDF(.xml) format.");
        options.addOption("cfg", "configfile", true,
                "Set the filename for the configuration.\nDefault: " + BootstrapStoreImpl.DEFAULT_FILE + ".");
        options.addOption("pkiks", "pkikspath", true,
                "Set the keystore file to load the CA key/certificate for the PKI service mock");
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null);

        // Parse arguments
        CommandLine cl;
        try {
            cl = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            System.err.println("Parsing failed.  Reason: " + e.getMessage());
            formatter.printHelp(USAGE, options);
            return;
        }

        // Print help
        if (cl.hasOption("help")) {
            formatter.printHelp(USAGE, options);
            return;
        }

        // Abort if unexpected options
        if (cl.getArgs().length > 0) {
            System.err.println("Unexpected option or arguments : " + cl.getArgList());
            formatter.printHelp(USAGE, options);
            return;
        }

        // Get local address
        String localAddress = cl.getOptionValue("lh");
        if (localAddress == null)
            localAddress = "0.0.0.0";
        String localPortOption = cl.getOptionValue("lp");
        int localPort = LwM2m.DEFAULT_COAP_PORT;
        if (localPortOption != null) {
            localPort = Integer.parseInt(localPortOption);
        }

        // Get secure local address
        String secureLocalAddress = cl.getOptionValue("slh");
        if (secureLocalAddress == null)
            secureLocalAddress = "0.0.0.0";
        String secureLocalPortOption = cl.getOptionValue("slp");
        int secureLocalPort = LwM2m.DEFAULT_COAP_SECURE_PORT;
        if (secureLocalPortOption != null) {
            secureLocalPort = Integer.parseInt(secureLocalPortOption);
        }

        // Get http port
        String webPortOption = cl.getOptionValue("wp");
        int webPort = 8080;
        if (webPortOption != null) {
            webPort = Integer.parseInt(webPortOption);
        }

        // Get models folder
        String modelsFolderPath = cl.getOptionValue("m");

        // Get config file
        String configFilename = cl.getOptionValue("cfg");
        if (configFilename == null) {
            configFilename = BootstrapStoreImpl.DEFAULT_FILE;
        }

        String pkiKeystorePath = cl.getOptionValue("pkiks");

        try {
            createAndStartServer(webPort, localAddress, localPort, secureLocalAddress, secureLocalPort,
                    modelsFolderPath, configFilename, pkiKeystorePath);
        } catch (BindException e) {
            System.err.println(String
                    .format("Web port %s is already in use, you can change it using the 'webport' option.", webPort));
            formatter.printHelp(USAGE, options);
        } catch (Exception e) {
            LOG.error("Jetty stopped with unexpected error ...", e);
        }
    }

    public static void createAndStartServer(int webPort, String localAddress, int localPort, String secureLocalAddress,
            int secureLocalPort, String modelsFolderPath, String configFilename, String pkiKeystorePath)
            throws Exception {
        // Create Models
        List<ObjectModel> models = ObjectLoader.loadDefault();
        if (modelsFolderPath != null) {
            models.addAll(ObjectLoader.loadObjectsFromDir(new File(modelsFolderPath)));
        }

        // Prepare and start bootstrap server
        LeshanBootstrapServerBuilder builder = new LeshanBootstrapServerBuilder();
        BootstrapStoreImpl bsStore = new BootstrapStoreImpl(configFilename);
        builder.setConfigStore(bsStore);
        builder.setSecurityStore(new BootstrapSecurityStoreImpl(bsStore));
        builder.setLocalAddress(localAddress, localPort);
        builder.setLocalSecureAddress(secureLocalAddress, secureLocalPort);
        builder.setModel(new LwM2mModel(models));
        builder.setCoapConfig(NetworkConfig.getStandard());

        if (pkiKeystorePath != null) {
            // PKI service mock using a self-signed certificate to sign the CSR
            String caAlias = "cacert";
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (FileInputStream fis = new FileInputStream(pkiKeystorePath)) {
                keyStore.load(fis, "123456".toCharArray());
            }
            builder.setPkiService(new PkiServiceMock((PrivateKey) keyStore.getKey(caAlias, "123456".toCharArray()),
                    (X509Certificate) keyStore.getCertificate(caAlias)));

            // preload a config for tests
            BootstrapConfig bsConfig = getTestConfig((X509Certificate) keyStore.getCertificate("dmcert"));
            bsStore.addConfig("endpoint123", bsConfig);
        }

        LeshanBootstrapServer bsServer = builder.build();
        bsServer.start();

        // Now prepare and start jetty
        Server server = new Server(webPort);
        WebAppContext root = new WebAppContext();

        root.setContextPath("/");
        root.setResourceBase(LeshanBootstrapServerDemo.class.getClassLoader().getResource("webapp").toExternalForm());
        root.setParentLoaderPriority(true);

        ServletHolder bsServletHolder = new ServletHolder(new BootstrapServlet(bsStore));
        root.addServlet(bsServletHolder, "/api/bootstrap/*");

        server.setHandler(root);

        server.start();
        LOG.info("Web server started at {}.", server.getURI());
    }

    // load a bootstrap configuration for test purpose
    private static BootstrapConfig getTestConfig(X509Certificate dmPublicKey)
            throws ConfigurationException, CertificateEncodingException {
        BootstrapConfig bsConfig = new BootstrapConfig();

        // --- security object ---
        Map<Integer, ServerSecurity> secs = new HashMap<>();

        ServerSecurity bsSecurity = new ServerSecurity();
        bsSecurity.bootstrapServer = true;
        bsSecurity.securityMode = SecurityMode.PSK;
        bsSecurity.uri = "coaps://localhost:5684";
        bsSecurity.publicKeyOrId = "identity123".getBytes();
        bsSecurity.secretKey = Hex.decodeHex("CAFEBABE".toCharArray());
        bsSecurity.serverId = 0; // should not be needed
        secs.put(0, bsSecurity);

        ServerSecurity dmSecurity = new ServerSecurity();
        dmSecurity.bootstrapServer = false;
        dmSecurity.securityMode = SecurityMode.EST;
        dmSecurity.uri = "coaps://localhost:5686";
        dmSecurity.serverPublicKey = dmPublicKey.getEncoded();
        dmSecurity.serverId = 1;
        secs.put(1, dmSecurity);

        bsConfig.security = secs;

        // --- server object ---
        ServerConfig dmServer = new ServerConfig();
        dmServer.shortId = 1;
        bsConfig.servers = Collections.singletonMap(0, dmServer);

        return bsConfig;
    }

}
