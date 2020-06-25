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
 *     Rikard Höglund (RISE) - additions to support OSCORE
 *******************************************************************************/

package org.eclipse.leshan.server.bootstrap.demo;

import java.io.File;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.config.NetworkConfig.Keys;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.oscore.OSCoreCoapStackFactory;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.SingleNodeConnectionIdGenerator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.leshan.core.LwM2m;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.eclipse.leshan.server.OscoreHandler;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigurationStoreAdapter;
import org.eclipse.leshan.server.bootstrap.demo.servlet.BootstrapServlet;
import org.eclipse.leshan.server.bootstrap.demo.servlet.ServerServlet;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeshanBootstrapServerDemo {

    static {
        // Define a default logback.configurationFile
        String property = System.getProperty("logback.configurationFile");
        if (property == null) {
            System.setProperty("logback.configurationFile", "logback-config.xml");
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(LeshanBootstrapServerDemo.class);

    private final static String USAGE = "java -jar leshan-bsserver-demo.jar [OPTION]";

    public static void main(String[] args) {
        // Define options for command line tools
        Options options = new Options();

        final StringBuilder RPKChapter = new StringBuilder();
        RPKChapter.append("\n .");
        RPKChapter.append("\n .");
        RPKChapter.append("\n================================[ RPK ]=================================");
        RPKChapter.append("\n| By default Leshan demo uses an embedded self-signed certificate and  |");
        RPKChapter.append("\n| trusts any client certificates allowing to use RPK or X509           |");
        RPKChapter.append("\n| at client side.                                                      |");
        RPKChapter.append("\n| To use RPK only with your own keys :                                 |");
        RPKChapter.append("\n|            -pubk -prik options should be used together.              |");
        RPKChapter.append("\n| To get helps about files format and how to generate it, see :        |");
        RPKChapter.append("\n| See https://github.com/eclipse/leshan/wiki/Credential-files-format   |");
        RPKChapter.append("\n------------------------------------------------------------------------");

        final StringBuilder X509Chapter = new StringBuilder();
        X509Chapter.append("\n .");
        X509Chapter.append("\n .");
        X509Chapter.append("\n===============================[ X509 ]=================================");
        X509Chapter.append("\n| By default Leshan demo uses an embedded self-signed certificate and  |");
        X509Chapter.append("\n| trusts any client certificates allowing to use RPK or X509           |");
        X509Chapter.append("\n| at client side.                                                      |");
        X509Chapter.append("\n| To use X509 with your own server key, certificate and truststore :   |");
        X509Chapter.append("\n|               [-cert, -prik], [-truststore] should be used together  |");
        X509Chapter.append("\n| To get helps about files format and how to generate it, see :        |");
        X509Chapter.append("\n| See https://github.com/eclipse/leshan/wiki/Credential-files-format   |");
        X509Chapter.append("\n------------------------------------------------------------------------");

        options.addOption("h", "help", false, "Display help information.");
        options.addOption("lh", "coaphost", true, "Set the local CoAP address.\n  Default: any local address.");
        options.addOption("lp", "coapport", true,
                String.format("Set the local CoAP port.\n  Default: %d.", LwM2m.DEFAULT_COAP_PORT));
        options.addOption("slh", "coapshost", true, "Set the secure local CoAP address.\nDefault: any local address.");
        options.addOption("slp", "coapsport", true,
                String.format("Set the secure local CoAP port.\nDefault: %d.", LwM2m.DEFAULT_COAP_SECURE_PORT));
        options.addOption("wh", "webhost", true, "Set the HTTP address for web server.\nDefault: any local address.");
        options.addOption("wp", "webport", true, "Set the HTTP port for web server.\nDefault: 8080.");
        options.addOption("m", "modelsfolder", true, "A folder which contains object models in OMA DDF(.xml) format.");
        options.addOption("cfg", "configfile", true,
                "Set the filename for the configuration.\nDefault: " + JSONFileBootstrapStore.DEFAULT_FILE + ".");
        options.addOption("oc", "activate support of old/deprecated cipher suites.");
        options.addOption("cid", true, "Control usage of DTLS connection ID." //
                + "\n - 'on' to activate Connection ID support (same as -cid 6)" //
                + "\n - 'off' to deactivate it" //
                + "\n - Positive value define the size in byte of CID generated."
                + "\n - 0 value means we accept to use CID but will not generated one for foreign peer."
                + "\n (Default: on)" + RPKChapter);
        options.addOption("pubk", true,
                "The path to your server public key file.\n The public Key should be in SubjectPublicKeyInfo format (DER encoding).");
        options.addOption("prik", true,
                "The path to your server private key file.\nThe private key should be in PKCS#8 format (DER encoding)."
                        + X509Chapter);
        options.addOption("cert", true,
                "The path to your server certificate or certificate chain file.\n"
                        + "The certificate Common Name (CN) should generally be equal to the server hostname.\n"
                        + "The certificate should be in X509v3 format (DER or PEM encoding).\n"
                        + "The certificate chain should be in X509v3 format (PEM encoding).");

        final StringBuilder trustStoreChapter = new StringBuilder();
        trustStoreChapter.append("\n .");
        trustStoreChapter
                .append("\n URI format: file://<path-to-trust-store-file>#<hex-strore-password>#<alias-pattern>");
        trustStoreChapter.append("\n .");
        trustStoreChapter.append("\n Where:");
        trustStoreChapter.append("\n - path-to-trust-store-file is path to pkcs12 trust store file");
        trustStoreChapter.append("\n - hex-store-password is HEX formatted password for store");
        trustStoreChapter.append(
                "\n - alias-pattern can be used to filter trusted certificates and can also be empty to get all");
        trustStoreChapter.append("\n .");
        trustStoreChapter.append("\n Default: All certificates are trusted which is only OK for a demo.");

        options.addOption("truststore", true,
                "The path to a root certificate file to trust or a folder containing all the trusted certificates in X509v3 format (DER encoding) or trust store URI."
                        + trustStoreChapter);

        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(120);
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

        // Abort if all RPK config is not complete
        boolean rpkConfig = false;
        if (cl.hasOption("pubk")) {
            if (!cl.hasOption("prik")) {
                System.err.println("pubk, prik should be used together to connect using RPK");
                formatter.printHelp(USAGE, options);
                return;
            } else {
                rpkConfig = true;
            }
        }

        // Abort if all X509 config is not complete
        boolean x509Config = false;
        if (cl.hasOption("cert")) {
            if (!cl.hasOption("prik")) {
                System.err.println("cert, prik should be used together to connect using X509");
                formatter.printHelp(USAGE, options);
                return;
            } else {
                x509Config = true;
            }
        }

        // Abort if prik is used without complete RPK or X509 config
        if (cl.hasOption("prik")) {
            if (!rpkConfig && !x509Config) {
                System.err.println("prik should be used with cert for X509 config OR pubk for RPK config");
                formatter.printHelp(USAGE, options);
                return;
            }
        }

        // Get local address
        String localAddress = cl.getOptionValue("lh");
        String localPortOption = cl.getOptionValue("lp");
        Integer localPort = null;
        if (localPortOption != null) {
            localPort = Integer.parseInt(localPortOption);
        }

        // Get secure local address
        String secureLocalAddress = cl.getOptionValue("slh");
        String secureLocalPortOption = cl.getOptionValue("slp");
        Integer secureLocalPort = null;
        if (secureLocalPortOption != null) {
            secureLocalPort = Integer.parseInt(secureLocalPortOption);
        }

        // get http address
        String webAddress = cl.getOptionValue("wh");
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
            configFilename = JSONFileBootstrapStore.DEFAULT_FILE;
        }

        // Get CID config
        String cidOption = cl.getOptionValue("cid");
        Integer cid = 6;
        if (cidOption != null) {
            if ("off".equals(cidOption)) {
                cid = null;
            } else if ("on".equals(cidOption)) {
                // we keep default value
            } else {
                cid = Integer.parseInt(cidOption);
                cid = cid < 0 ? null : cid;
            }
        }

        // get RPK info
        PublicKey publicKey = null;
        PrivateKey privateKey = null;
        if (rpkConfig) {
            try {
                privateKey = SecurityUtil.privateKey.readFromFile(cl.getOptionValue("prik"));
                publicKey = SecurityUtil.publicKey.readFromFile(cl.getOptionValue("pubk"));
            } catch (Exception e) {
                System.err.println("Unable to load RPK files : " + e.getMessage());
                e.printStackTrace();
                formatter.printHelp(USAGE, options);
                return;
            }
        }

        // get X509 info
        X509Certificate[] certificate = null;
        if (cl.hasOption("cert")) {
            try {
                privateKey = SecurityUtil.privateKey.readFromFile(cl.getOptionValue("prik"));
                certificate = SecurityUtil.certificateChain.readFromFile(cl.getOptionValue("cert"));
            } catch (Exception e) {
                System.err.println("Unable to load X509 files : " + e.getMessage());
                e.printStackTrace();
                formatter.printHelp(USAGE, options);
                return;
            }
        }

        // configure trust store if given
        List<Certificate> trustStore = null;
        if (cl.hasOption("truststore")) {
            trustStore = new ArrayList<>();

            String trustStoreName = cl.getOptionValue("truststore");

            if (trustStoreName.startsWith("file://")) {
                // Treat argument as Java trust store
                try {
                    Certificate[] trustedCertificates = SslContextUtil.loadTrustedCertificates(trustStoreName);
                    trustStore.addAll(Arrays.asList(trustedCertificates));
                } catch (Exception e) {
                    System.err.println("Failed to load trust store : " + e.getMessage());
                    e.printStackTrace();
                    formatter.printHelp(USAGE, options);
                    return;
                }
            } else {
                // Treat argument as file or directory
                File input = new File(cl.getOptionValue("truststore"));

                // check input exists
                if (!input.exists()) {
                    System.err.println(
                            "Failed to load trust store - file or directory does not exist : " + input.toString());
                    formatter.printHelp(USAGE, options);
                    return;
                }

                // get input files.
                File[] files;
                if (input.isDirectory()) {
                    files = input.listFiles();
                } else {
                    files = new File[] { input };
                }
                for (File file : files) {
                    try {
                        trustStore.add(SecurityUtil.certificate.readFromFile(file.getAbsolutePath()));
                    } catch (Exception e) {
                        LOG.warn("Unable to load X509 files {} : {} ", file.getAbsolutePath(), e.getMessage());
                    }
                }
            }
        }

        try {
            createAndStartServer(webAddress, webPort, localAddress, localPort, secureLocalAddress, secureLocalPort,
                    modelsFolderPath, configFilename, cl.hasOption("oc"), publicKey, privateKey, certificate,
                    trustStore, cid);
        } catch (BindException e) {
            System.err.println(String
                    .format("Web port %s is already in use, you can change it using the 'webport' option.", webPort));
            formatter.printHelp(USAGE, options);
        } catch (Exception e) {
            LOG.error("Jetty stopped with unexpected error ...", e);
        }
    }

    public static void createAndStartServer(String webAddress, int webPort, String localAddress, Integer localPort,
            String secureLocalAddress, Integer secureLocalPort, String modelsFolderPath, String configFilename,
            boolean supportDeprecatedCiphers, PublicKey publicKey, PrivateKey privateKey, X509Certificate[] certificate,
            List<Certificate> trustStore, Integer cid) throws Exception {

        // Enable OSCORE stack (fine to do even when using DTLS or only CoAP)
        // TODO OSCORE : this should be done in DefaultEndpointFactory ?
        OSCoreCoapStackFactory.useAsDefault(OscoreHandler.getContextDB());

        // Create Models
        List<ObjectModel> models = ObjectLoader.loadDefault();
        if (modelsFolderPath != null) {
            models.addAll(ObjectLoader.loadObjectsFromDir(new File(modelsFolderPath)));
        }

        // Prepare and start bootstrap server
        LeshanBootstrapServerBuilder builder = new LeshanBootstrapServerBuilder();
        JSONFileBootstrapStore bsStore = new JSONFileBootstrapStore(configFilename);
        builder.setConfigStore(new BootstrapConfigurationStoreAdapter(bsStore));
        builder.setSecurityStore(new BootstrapConfigSecurityStore(bsStore));
        builder.setModel(new StaticModel(models));

        // Create DTLS Config
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
        dtlsConfig.setRecommendedCipherSuitesOnly(!supportDeprecatedCiphers);
        if (cid != null) {
            dtlsConfig.setConnectionIdGenerator(new SingleNodeConnectionIdGenerator(cid));
        }

        // Create credentials;
        X509Certificate[] serverCertificateChain = null;
        if (certificate != null) {
            // use X.509 mode (+ RPK)
            serverCertificateChain = certificate;
            builder.setPrivateKey(privateKey);
            builder.setCertificateChain(serverCertificateChain);
        } else if (publicKey != null) {
            // use RPK
            builder.setPublicKey(publicKey);
            builder.setPrivateKey(privateKey);
        } else {
            try {
                PrivateKey embeddedPrivateKey = SecurityUtil.privateKey
                        .readFromResource("credentials/bsserver_privkey.der");
                serverCertificateChain = SecurityUtil.certificateChain
                        .readFromResource("credentials/bsserver_cert.der");
                builder.setPrivateKey(embeddedPrivateKey);
                builder.setCertificateChain(serverCertificateChain);
            } catch (Exception e) {
                LOG.error("Unable to load embedded X.509 certificate.", e);
                System.exit(-1);
            }
        }

        // Define trust store
        if (serverCertificateChain != null) {
            if (trustStore != null && !trustStore.isEmpty()) {
                builder.setTrustedCertificates(trustStore.toArray(new Certificate[trustStore.size()]));
            } else {
                // by default trust all
                builder.setTrustedCertificates(new X509Certificate[0]);
            }
        }

        // Set DTLS Config
        builder.setDtlsConfig(dtlsConfig);

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
        builder.setLocalAddress(localAddress,
                localPort == null ? coapConfig.getInt(Keys.COAP_PORT, LwM2m.DEFAULT_COAP_PORT) : localPort);
        builder.setLocalSecureAddress(secureLocalAddress,
                secureLocalPort == null ? coapConfig.getInt(Keys.COAP_SECURE_PORT, LwM2m.DEFAULT_COAP_SECURE_PORT)
                        : secureLocalPort);

        LeshanBootstrapServer bsServer = builder.build();
        bsServer.start();

        // Now prepare and start jetty
        InetSocketAddress jettyAddr;
        if (webAddress == null) {
            jettyAddr = new InetSocketAddress(webPort);
        } else {
            jettyAddr = new InetSocketAddress(webAddress, webPort);
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
        root.addServlet(aHolder, "/v2/*");

        // Configuration for old demo
        DefaultServlet bServlet = new DefaultServlet();
        ServletHolder bHolder = new ServletHolder(bServlet);
        bHolder.setInitParameter("resourceBase",
                LeshanBootstrapServerDemo.class.getClassLoader().getResource("webapp").toExternalForm());
        bHolder.setInitParameter("pathInfoOnly", "true");
        root.addServlet(bHolder, "/*");
        /* **************************************************************** */

        ServletHolder bsServletHolder = new ServletHolder(new BootstrapServlet(bsStore));
        root.addServlet(bsServletHolder, "/api/bootstrap/*");
        root.addServlet(bsServletHolder, "/v2/api/bootstrap/*"); // Temporary code to be able to serve both UI

        ServletHolder serverServletHolder;
        if (publicKey != null) {
            serverServletHolder = new ServletHolder(new ServerServlet(bsServer, publicKey));
        } else {
            serverServletHolder = new ServletHolder(new ServerServlet(bsServer, serverCertificateChain[0]));
        }
        root.addServlet(serverServletHolder, "/api/server/*");
        root.addServlet(serverServletHolder, "/v2/api/server/*"); // Temporary code to be able to serve both UI

        server.setHandler(root);
        /* **************************************************************** */

        server.start();
        LOG.info("Web server started at {}.", server.getURI());
    }
}
