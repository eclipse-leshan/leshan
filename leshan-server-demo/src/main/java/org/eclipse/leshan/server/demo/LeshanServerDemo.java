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
 *     Rikard Höglund (RISE SICS) - Additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.server.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

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
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.eclipse.leshan.server.OscoreHandler;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.demo.servlet.ClientServlet;
import org.eclipse.leshan.server.demo.servlet.EventServlet;
import org.eclipse.leshan.server.demo.servlet.ObjectSpecServlet;
import org.eclipse.leshan.server.demo.servlet.SecurityServlet;
import org.eclipse.leshan.server.demo.utils.MagicLwM2mValueConverter;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.VersionedModelProvider;
import org.eclipse.leshan.server.redis.RedisRegistrationStore;
import org.eclipse.leshan.server.redis.RedisSecurityStore;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.FileSecurityStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.util.Pool;

public class LeshanServerDemo {

    static {
        // Define a default logback.configurationFile
        String property = System.getProperty("logback.configurationFile");
        if (property == null) {
            System.setProperty("logback.configurationFile", "logback-config.xml");
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(LeshanServerDemo.class);

    private final static String USAGE = "java -jar leshan-server-demo.jar [OPTION]\n\n";

    private final static String DEFAULT_KEYSTORE_TYPE = KeyStore.getDefaultType();

    private final static String DEFAULT_KEYSTORE_ALIAS = "leshan";

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

        final StringBuilder X509ChapterDeprecated = new StringBuilder();
        X509ChapterDeprecated.append("\n .");
        X509ChapterDeprecated.append("\n .");
        X509ChapterDeprecated.append("\n=======================[ X509 deprecated way]===========================");
        X509ChapterDeprecated.append("\n| By default Leshan demo uses an embedded self-signed certificate and  |");
        X509ChapterDeprecated.append("\n| trusts any client certificates allowing to use RPK or X509           |");
        X509ChapterDeprecated.append("\n| at client side.                                                      |");
        X509ChapterDeprecated.append("\n| If you want to use your own server keys, certificates and truststore,|");
        X509ChapterDeprecated.append("\n| you can provide a keystore using :                                   |");
        X509ChapterDeprecated.append("\n|         -ks, -ksp, [-kst], [-ksa], -ksap should be used together     |");
        X509ChapterDeprecated.append("\n| To get helps about files format and how to generate it, see :        |");
        X509ChapterDeprecated.append("\n| See https://github.com/eclipse/leshan/wiki/Credential-files-format   |");
        X509ChapterDeprecated.append("\n------------------------------------------------------------------------");

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
        options.addOption("oc", "activate support of old/deprecated cipher suites.");
        options.addOption("cid", true, "Control usage of DTLS connection ID." //
                + "\n - 'on' to activate Connection ID support (same as -cid 6)" //
                + "\n - 'off' to deactivate it" //
                + "\n - Positive value define the size in byte of CID generated."
                + "\n - 0 value means we accept to use CID but will not generated one for foreign peer."
                + "\n (Default: on)");
        options.addOption("r", "redis", true,
                "Use redis to store registration and securityInfo. \nThe URL of the redis server should be given using this format : 'redis://:password@hostname:port/db_number'\nExample without DB and password: 'redis://localhost:6379'\nDefault: redis is not used.");
        options.addOption("mdns", "publishDNSSdServices", false,
                "Publish leshan's services to DNS Service discovery" + RPKChapter);
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
                        + trustStoreChapter + X509ChapterDeprecated);
        options.addOption("ks", "keystore", true,
                "Set the key store file.\nIf set, X.509 mode is enabled, otherwise built-in RPK credentials are used.");
        options.addOption("ksp", "storepass", true, "Set the key store password.");
        options.addOption("kst", "storetype", true,
                String.format("Set the key store type.\nDefault: %s.", DEFAULT_KEYSTORE_TYPE));
        options.addOption("ksa", "alias", true, String.format(
                "Set the key store alias to use for server credentials.\nDefault: %s.\n All other alias referencing a certificate will be trusted.",
                DEFAULT_KEYSTORE_ALIAS));
        options.addOption("ksap", "keypass", true, "Set the key store alias password to use.");

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

        // get local address
        String localAddress = cl.getOptionValue("lh");
        String localPortOption = cl.getOptionValue("lp");
        Integer localPort = null;
        if (localPortOption != null) {
            localPort = Integer.parseInt(localPortOption);
        }

        // get secure local address
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

        // get the Redis hostname:port
        String redisUrl = cl.getOptionValue("r");

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

        // Get keystore parameters
        String keyStorePath = cl.getOptionValue("ks");
        String keyStoreType = cl.getOptionValue("kst", KeyStore.getDefaultType());
        String keyStorePass = cl.getOptionValue("ksp");
        String keyStoreAlias = cl.getOptionValue("ksa", "leshan");
        String keyStoreAliasPass = cl.getOptionValue("ksap");

        // Get mDNS publish switch
        Boolean publishDNSSdServices = cl.hasOption("mdns");

        try {
            createAndStartServer(webAddress, webPort, localAddress, localPort, secureLocalAddress, secureLocalPort,
                    modelsFolderPath, redisUrl, publicKey, privateKey, certificate, trustStore, keyStorePath,
                    keyStoreType, keyStorePass, keyStoreAlias, keyStoreAliasPass, publishDNSSdServices,
                    cl.hasOption("oc"), cid);
        } catch (BindException e) {
            System.err.println(
                    String.format("Web port %s is already used, you could change it using 'webport' option.", webPort));
            formatter.printHelp(USAGE, options);
        } catch (Exception e) {
            LOG.error("Jetty stopped with unexpected error ...", e);
        }
    }

    public static void createAndStartServer(String webAddress, int webPort, String localAddress, Integer localPort,
            String secureLocalAddress, Integer secureLocalPort, String modelsFolderPath, String redisUrl,
            PublicKey publicKey, PrivateKey privateKey, X509Certificate[] certificate, List<Certificate> trustStore,
            String keyStorePath, String keyStoreType, String keyStorePass, String keyStoreAlias,
            String keyStoreAliasPass, Boolean publishDNSSdServices, boolean supportDeprecatedCiphers, Integer cid)
            throws Exception {
        // Prepare LWM2M server
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setEncoder(new DefaultLwM2mNodeEncoder());
        LwM2mNodeDecoder decoder = new DefaultLwM2mNodeDecoder();
        builder.setDecoder(decoder);

        // Enable OSCORE stack (fine to do even when using DTLS or only CoAP)
        // TODO OSCORE : OSCoreCoapStack should be created in DefaultEndpointFactory.
        OSCoreCoapStackFactory.useAsDefault(OscoreHandler.getContextDB());

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

        // Connect to redis if needed
        Pool<Jedis> jedis = null;
        if (redisUrl != null) {
            // TODO support sentinel pool and make pool configurable
            jedis = new JedisPool(new URI(redisUrl));
        }

        // Create DTLS Config
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
        dtlsConfig.setRecommendedCipherSuitesOnly(!supportDeprecatedCiphers);
        if (cid != null) {
            dtlsConfig.setConnectionIdGenerator(new SingleNodeConnectionIdGenerator(cid));
        }

        X509Certificate[] serverCertificateChain = null;
        if (certificate != null) {
            // use X.509 mode (+ RPK)
            serverCertificateChain = certificate;
            builder.setPrivateKey(privateKey);
            builder.setCertificateChain(serverCertificateChain);
        } else if (publicKey != null) {
            // use RPK only
            builder.setPublicKey(publicKey);
            builder.setPrivateKey(privateKey);
        } else if (keyStorePath != null) {
            LOG.warn(
                    "Keystore way [-ks, -ksp, -kst, -ksa, -ksap] is DEPRECATED for leshan demo and will probably be removed soon, please use [-cert, -prik, -truststore] options");

            // Deprecated way : Set up X.509 mode (+ RPK)
            try {
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                try (FileInputStream fis = new FileInputStream(keyStorePath)) {
                    keyStore.load(fis, keyStorePass == null ? null : keyStorePass.toCharArray());
                    List<Certificate> trustedCertificates = new ArrayList<>();
                    for (Enumeration<String> aliases = keyStore.aliases(); aliases.hasMoreElements();) {
                        String alias = aliases.nextElement();
                        if (keyStore.isCertificateEntry(alias)) {
                            trustedCertificates.add(keyStore.getCertificate(alias));
                        } else if (keyStore.isKeyEntry(alias) && alias.equals(keyStoreAlias)) {
                            List<X509Certificate> x509CertificateChain = new ArrayList<>();
                            Certificate[] certificateChain = keyStore.getCertificateChain(alias);
                            if (certificateChain == null || certificateChain.length == 0) {
                                LOG.error("Keystore alias must have a non-empty chain of X509Certificates.");
                                System.exit(-1);
                            }

                            for (Certificate cert : certificateChain) {
                                if (!(cert instanceof X509Certificate)) {
                                    LOG.error("Non-X.509 certificate in alias chain is not supported: {}", cert);
                                    System.exit(-1);
                                }
                                x509CertificateChain.add((X509Certificate) cert);
                            }

                            Key key = keyStore.getKey(alias,
                                    keyStoreAliasPass == null ? new char[0] : keyStoreAliasPass.toCharArray());
                            if (!(key instanceof PrivateKey)) {
                                LOG.error("Keystore alias must have a PrivateKey entry, was {}",
                                        key == null ? null : key.getClass().getName());
                                System.exit(-1);
                            }
                            builder.setPrivateKey((PrivateKey) key);
                            serverCertificateChain = x509CertificateChain
                                    .toArray(new X509Certificate[x509CertificateChain.size()]);
                            builder.setCertificateChain(serverCertificateChain);
                        }
                    }
                    builder.setTrustedCertificates(
                            trustedCertificates.toArray(new Certificate[trustedCertificates.size()]));
                }
            } catch (KeyStoreException | IOException e) {
                LOG.error("Unable to initialize X.509.", e);
                System.exit(-1);
            }
        }

        if (publicKey == null && serverCertificateChain == null) {
            // public key or server certificated is not defined
            // use default embedded credentials (X.509 + RPK mode)
            try {
                PrivateKey embeddedPrivateKey = SecurityUtil.privateKey
                        .readFromResource("credentials/server_privkey.der");
                serverCertificateChain = SecurityUtil.certificateChain.readFromResource("credentials/server_cert.der");
                builder.setPrivateKey(embeddedPrivateKey);
                builder.setCertificateChain(serverCertificateChain);
            } catch (Exception e) {
                LOG.error("Unable to load embedded X.509 certificate.", e);
                System.exit(-1);
            }
        }

        // Define trust store
        if (serverCertificateChain != null && keyStorePath == null) {
            if (trustStore != null && !trustStore.isEmpty()) {
                builder.setTrustedCertificates(trustStore.toArray(new Certificate[trustStore.size()]));
            } else {
                // by default trust all
                builder.setTrustedCertificates(new X509Certificate[0]);
            }
        }

        // Set DTLS Config
        builder.setDtlsConfig(dtlsConfig);

        // Define model provider
        List<ObjectModel> models = ObjectLoader.loadAllDefault();
        models.addAll(ObjectLoader.loadDdfResources("/models/", LwM2mDemoConstant.modelPaths));
        if (modelsFolderPath != null) {
            models.addAll(ObjectLoader.loadObjectsFromDir(new File(modelsFolderPath), true));
        }
        LwM2mModelProvider modelProvider = new VersionedModelProvider(models);
        builder.setObjectModelProvider(modelProvider);

        // Set securityStore & registrationStore
        EditableSecurityStore securityStore;
        if (jedis == null) {
            // use file persistence
            securityStore = new FileSecurityStore();
        } else {
            // use Redis Store
            securityStore = new RedisSecurityStore(jedis);
            builder.setRegistrationStore(new RedisRegistrationStore(jedis));
        }
        builder.setSecurityStore(securityStore);

        // use a magic converter to support bad type send by the UI.
        builder.setEncoder(new DefaultLwM2mNodeEncoder(new MagicLwM2mValueConverter()));

        // Create and start LWM2M server
        LeshanServer lwServer = builder.build();

        // Now prepare Jetty
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
        root.addServlet(aHolder, "/v2/*");

        // Configuration for old demo
        DefaultServlet bServlet = new DefaultServlet();
        ServletHolder bHolder = new ServletHolder(bServlet);
        bHolder.setInitParameter("resourceBase",
                LeshanServerDemo.class.getClassLoader().getResource("webapp").toExternalForm());
        bHolder.setInitParameter("pathInfoOnly", "true");
        root.addServlet(bHolder, "/*");

        server.setHandler(root);
        /* **************************************************************** */

        // Create Servlet
        EventServlet eventServlet = new EventServlet(lwServer, lwServer.getSecuredAddress().getPort());
        ServletHolder eventServletHolder = new ServletHolder(eventServlet);
        root.addServlet(eventServletHolder, "/event/*"); // Temporary code to be able to serve both UI
        root.addServlet(eventServletHolder, "/api/event/*");
        root.addServlet(eventServletHolder, "/v2/api/event/*"); // Temporary code to be able to serve both UI

        ServletHolder clientServletHolder = new ServletHolder(new ClientServlet(lwServer));
        root.addServlet(clientServletHolder, "/api/clients/*");
        root.addServlet(clientServletHolder, "/v2/api/clients/*");// Temporary code to be able to serve both UI

        ServletHolder securityServletHolder;
        if (publicKey != null) {
            securityServletHolder = new ServletHolder(new SecurityServlet(securityStore, publicKey));
        } else {
            securityServletHolder = new ServletHolder(new SecurityServlet(securityStore, serverCertificateChain[0]));
        }
        root.addServlet(securityServletHolder, "/api/security/*");
        root.addServlet(securityServletHolder, "/v2/api/security/*");// Temporary code to be able to serve both UI

        ServletHolder objectSpecServletHolder = new ServletHolder(
                new ObjectSpecServlet(lwServer.getModelProvider(), lwServer.getRegistrationService()));
        root.addServlet(objectSpecServletHolder, "/api/objectspecs/*");
        root.addServlet(objectSpecServletHolder, "/v2/api/objectspecs/*");// Temporary code to be able to serve both UI

        // Register a service to DNS-SD
        if (publishDNSSdServices) {

            // Create a JmDNS instance
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());

            // Publish Leshan HTTP Service
            ServiceInfo httpServiceInfo = ServiceInfo.create("_http._tcp.local.", "leshan", webPort, "");
            jmdns.registerService(httpServiceInfo);

            // Publish Leshan CoAP Service
            ServiceInfo coapServiceInfo = ServiceInfo.create("_coap._udp.local.", "leshan", localPort, "");
            jmdns.registerService(coapServiceInfo);

            // Publish Leshan Secure CoAP Service
            ServiceInfo coapSecureServiceInfo = ServiceInfo.create("_coaps._udp.local.", "leshan", secureLocalPort, "");
            jmdns.registerService(coapSecureServiceInfo);
        }

        // Start Jetty & Leshan
        lwServer.start();
        server.start();
        LOG.info("Web server started at {}.", server.getURI());
    }
}
