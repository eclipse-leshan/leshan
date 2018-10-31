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
 *     Bosch Software Innovations - added Redis URL support with authentication
 *     Firis SA - added mDNS services registering 
 *******************************************************************************/
package org.eclipse.leshan.server.demo;

import org.apache.commons.cli.*;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.leshan.Link;
import org.eclipse.leshan.LwM2m;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.CoapRequestBuilder;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.cluster.RedisRegistrationStore;
import org.eclipse.leshan.server.cluster.RedisSecurityStore;
import org.eclipse.leshan.server.demo.servlet.ClientServlet;
import org.eclipse.leshan.server.demo.servlet.EventServlet;
import org.eclipse.leshan.server.demo.servlet.ObjectSpecServlet;
import org.eclipse.leshan.server.demo.servlet.SecurityServlet;
import org.eclipse.leshan.server.impl.FileSecurityStore;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.StaticModelProvider;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.util.Pool;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.*;
import java.util.*;

public class LeshanServerDemo2 {

    static {
        // Define a default logback.configurationFile
        String property = System.getProperty("logback.configurationFile");
        if (property == null) {
            System.setProperty("logback.configurationFile", "logback-config.xml");
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(LeshanServerDemo2.class);

    private final static String[] modelPaths = new String[]{"31024.xml",

            "10241.xml", "10242.xml", "10243.xml", "10244.xml", "10245.xml", "10246.xml", "10247.xml",
            "10248.xml", "10249.xml", "10250.xml",

            "2048.xml", "2049.xml", "2050.xml", "2051.xml", "2052.xml", "2053.xml", "2054.xml",
            "2055.xml", "2056.xml", "2057.xml",

            "3200.xml", "3201.xml", "3202.xml", "3203.xml", "3300.xml", "3301.xml", "3302.xml",
            "3303.xml", "3304.xml", "3305.xml", "3306.xml", "3308.xml", "3310.xml", "3311.xml",
            "3312.xml", "3313.xml", "3314.xml", "3315.xml", "3316.xml", "3317.xml", "3318.xml",
            "3319.xml", "3320.xml", "3321.xml", "3322.xml", "3323.xml", "3324.xml", "3325.xml",
            "3326.xml", "3327.xml", "3328.xml", "3329.xml", "3330.xml", "3331.xml", "3332.xml",
            "3333.xml", "3334.xml", "3335.xml", "3336.xml", "3337.xml", "3338.xml", "3339.xml",
            "3340.xml", "3341.xml", "3342.xml", "3343.xml", "3344.xml", "3345.xml", "3346.xml",
            "3347.xml", "3348.xml", "3349.xml", "3350.xml",

            "Communication_Characteristics-V1_0.xml",

            "LWM2M_Lock_and_Wipe-V1_0.xml", "LWM2M_Cellular_connectivity-v1_0.xml",
            "LWM2M_APN_connection_profile-v1_0.xml", "LWM2M_WLAN_connectivity4-v1_0.xml",
            "LWM2M_Bearer_selection-v1_0.xml", "LWM2M_Portfolio-v1_0.xml", "LWM2M_DevCapMgmt-v1_0.xml",
            "LWM2M_Software_Component-v1_0.xml", "LWM2M_Software_Management-v1_0.xml",

            "Non-Access_Stratum_NAS_configuration-V1_0.xml"};

    private final static String USAGE = "java -jar leshan-server-demo.jar [OPTION]";

    private final static String DEFAULT_KEYSTORE_TYPE = KeyStore.getDefaultType();

    private final static String DEFAULT_KEYSTORE_ALIAS = "leshan";

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
        options.addOption("ks", "keystore", true,
                "Set the key store file. If set, X.509 mode is enabled, otherwise built-in RPK credentials are used.");
        options.addOption("ksp", "storepass", true, "Set the key store password.");
        options.addOption("kst", "storetype", true,
                String.format("Set the key store type.\nDefault: %s.", DEFAULT_KEYSTORE_TYPE));
        options.addOption("ksa", "alias", true, String.format(
                "Set the key store alias to use for server credentials.\nDefault: %s.", DEFAULT_KEYSTORE_ALIAS));
        options.addOption("ksap", "keypass", true, "Set the key store alias password to use.");
        options.addOption("wh", "webhost", true, "Set the HTTP address for web server.\nDefault: any local address.");
        options.addOption("wp", "webport", true, "Set the HTTP port for web server.\nDefault: 8080.");
        options.addOption("m", "modelsfolder", true, "A folder which contains object models in OMA DDF(.xml) format.");
        options.addOption("r", "redis", true,
                "Set the location of the Redis database for running in cluster mode. The URL is in the format of: 'redis://:password@hostname:port/db_number'\nExample without DB and password: 'redis://localhost:6379'\nDefault: none, no Redis connection.");
        options.addOption("mdns", "publishDNSSdServices", false, "Publish leshan's services to DNS Service discovery");
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

        // get local address
        String localAddress = cl.getOptionValue("lh");
        String localPortOption = cl.getOptionValue("lp");
        int localPort = LwM2m.DEFAULT_COAP_PORT;
        if (localPortOption != null) {
            localPort = Integer.parseInt(localPortOption);
        }

        // get secure local address
        String secureLocalAddress = cl.getOptionValue("slh");
        String secureLocalPortOption = cl.getOptionValue("slp");
        int secureLocalPort = LwM2m.DEFAULT_COAP_SECURE_PORT;
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

        // get the Redis hostname:port
        String redisUrl = cl.getOptionValue("r");

        // Get keystore parameters
        String keyStorePath = cl.getOptionValue("ks");
        String keyStoreType = cl.getOptionValue("kst", KeyStore.getDefaultType());
        String keyStorePass = cl.getOptionValue("ksp");
        String keyStoreAlias = cl.getOptionValue("ksa");
        String keyStoreAliasPass = cl.getOptionValue("ksap");

        // Get mDNS publish switch
        Boolean publishDNSSdServices = cl.hasOption("mdns");

        try {
            createAndStartServer(webAddress, webPort, localAddress, localPort, secureLocalAddress, secureLocalPort,
                    modelsFolderPath, redisUrl, keyStorePath, keyStoreType, keyStorePass, keyStoreAlias,
                    keyStoreAliasPass, publishDNSSdServices);
        } catch (BindException e) {
            System.err.println(
                    String.format("Web port %s is already used, you could change it using 'webport' option.", webPort));
            formatter.printHelp(USAGE, options);
        } catch (Exception e) {
            LOG.error("Jetty stopped with unexpected error ...", e);
        }
    }

    public static void createAndStartServer(String webAddress, int webPort, String localAddress, int localPort,
                                            String secureLocalAddress, int secureLocalPort, String modelsFolderPath, String redisUrl,
                                            String keyStorePath, String keyStoreType, String keyStorePass, String keyStoreAlias,
                                            String keyStoreAliasPass, Boolean publishDNSSdServices) throws Exception {
        // Prepare LWM2M server
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(localAddress, localPort);
        builder.setLocalSecureAddress(secureLocalAddress, secureLocalPort);
        builder.setEncoder(new DefaultLwM2mNodeEncoder());
        LwM2mNodeDecoder decoder = new DefaultLwM2mNodeDecoder();
        builder.setDecoder(decoder);

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

        // connect to redis if needed
        Pool<Jedis> jedis = null;
        if (redisUrl != null) {
            // TODO: support sentinel pool and make pool configurable
            jedis = new JedisPool(new URI(redisUrl));
        }

        PublicKey publicKey = null;

        // Set up X.509 mode
        if (keyStorePath != null) {
            try {
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                try (FileInputStream fis = new FileInputStream(keyStorePath)) {
                    keyStore.load(fis, keyStorePass == null ? null : keyStorePass.toCharArray());
                    List<Certificate> trustedCertificates = new ArrayList<>();
                    for (Enumeration<String> aliases = keyStore.aliases(); aliases.hasMoreElements(); ) {
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

                            for (Certificate certificate : certificateChain) {
                                if (!(certificate instanceof X509Certificate)) {
                                    LOG.error("Non-X.509 certificate in alias chain is not supported: {}", certificate);
                                    System.exit(-1);
                                }
                                x509CertificateChain.add((X509Certificate) certificate);
                            }

                            Key key = keyStore.getKey(alias,
                                    keyStoreAliasPass == null ? new char[0] : keyStoreAliasPass.toCharArray());
                            if (!(key instanceof PrivateKey)) {
                                LOG.error("Keystore alias must have a PrivateKey entry, was {}",
                                        key == null ? null : key.getClass().getName());
                                System.exit(-1);
                            }
                            builder.setPrivateKey((PrivateKey) key);
                            publicKey = keyStore.getCertificate(alias).getPublicKey();
                            builder.setCertificateChain(
                                    x509CertificateChain.toArray(new X509Certificate[x509CertificateChain.size()]));
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
        // Otherwise, set up RPK mode
        else {
            try {
                // Get point values
                byte[] publicX = Hex
                        .decodeHex("fcc28728c123b155be410fc1c0651da374fc6ebe7f96606e90d927d188894a73".toCharArray());
                byte[] publicY = Hex
                        .decodeHex("d2ffaa73957d76984633fc1cc54d0b763ca0559a9dff9706e9f4557dacc3f52a".toCharArray());
                byte[] privateS = Hex
                        .decodeHex("1dae121ba406802ef07c193c1ee4df91115aabd79c1ed7f4c0ef7ef6a5449400".toCharArray());

                // Get Elliptic Curve Parameter spec for secp256r1
                AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
                algoParameters.init(new ECGenParameterSpec("secp256r1"));
                ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);

                // Create key specs
                KeySpec publicKeySpec = new ECPublicKeySpec(
                        new ECPoint(new BigInteger(publicX), new BigInteger(publicY)), parameterSpec);
                KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);

                // Get keys
                publicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
                Files.write(Paths.get("server_pub.der"), publicKey.getEncoded(), StandardOpenOption.CREATE);
                PrivateKey privateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);
                builder.setPublicKey(publicKey);
                builder.setPrivateKey(privateKey);
            } catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidParameterSpecException e) {
                LOG.error("Unable to initialize RPK.", e);
                System.exit(-1);
            }
        }

        // Define model provider
        List<ObjectModel> models = ObjectLoader.loadDefault();
        models.addAll(ObjectLoader.loadDdfResources("/models/", modelPaths));
        if (modelsFolderPath != null) {
            models.addAll(ObjectLoader.loadObjectsFromDir(new File(modelsFolderPath)));
        }
        LwM2mModelProvider modelProvider = new StaticModelProvider(models);
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

        // Create and start LWM2M server
        final LeshanServer lwServer = getLeshanServer(builder);


        // Now prepare Jetty
        InetSocketAddress jettyAddr;
        if (webAddress == null) {
            jettyAddr = new InetSocketAddress(webPort);
        } else {
            jettyAddr = new InetSocketAddress(webAddress, webPort);
        }
        final Server server = new Server(jettyAddr);
        WebAppContext root = new WebAppContext();
        root.setContextPath("/");
        root.setResourceBase(LeshanServerDemo2.class.getClassLoader().getResource("webapp").toExternalForm());
        root.setParentLoaderPriority(true);
        server.setHandler(root);

        // Create Servlet
        EventServlet eventServlet = new EventServlet(lwServer, lwServer.getSecuredAddress().getPort());
        ServletHolder eventServletHolder = new ServletHolder(eventServlet);
        root.addServlet(eventServletHolder, "/event/*");

        ServletHolder clientServletHolder = new ServletHolder(
                new ClientServlet(lwServer, lwServer.getSecuredAddress().getPort()));
        root.addServlet(clientServletHolder, "/api/clients/*");

        ServletHolder securityServletHolder = new ServletHolder(new SecurityServlet(securityStore, publicKey));
        root.addServlet(securityServletHolder, "/api/security/*");

        ServletHolder objectSpecServletHolder = new ServletHolder(new ObjectSpecServlet(lwServer.getModelProvider()));
        root.addServlet(objectSpecServletHolder, "/api/objectspecs/*");

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

    /**
     * create LwM2M server
     */
    private static LeshanServer getLeshanServer(LeshanServerBuilder builder) {

        final LeshanServer lwServer = builder.build();

        lwServer.getRegistrationService().addListener(new RegistrationListener() {

            LwM2mModel model = new LwM2mModel(ObjectLoader.loadDefault());
            LwM2mNodeEncoder encoder = new DefaultLwM2mNodeEncoder();

            @Override
            public void registered(Registration registration, Registration previousReg, Collection<Observation> previousObsersations) {
                LOG.error("new device: " + registration.getEndpoint() + ":" + registration.getId());

                Link[] objectLinks = registration.getObjectLinks();
                for (int i = 1; i < objectLinks.length; ) {
                    //observe all
                    try {
                        ObserveResponse send = lwServer.send(registration, new ObserveRequest(objectLinks[i++].getUrl()));
                        if (send.isSuccess()) {
                            LOG.error("Observe success:" + send.getObservation() + "\n" + send.getTimestampedLwM2mNode() + "\n");
                        } else {
                            LOG.error("Observe failed");
                        }
                    } catch (Exception e) {
                        LOG.error("observe error:\n" + ExceptionUtils.getFullStackTrace(e));
                    }
                    //read all
                    try {
                        ReadRequest readRequest = new ReadRequest(objectLinks[1].getUrl());

//                        ReadResponse send = lwServer.send(registration, readRequest);

                        CoapRequestBuilder coapRequestBuilder = new CoapRequestBuilder(registration.getIdentity(),
                                registration.getRootPath(), registration.getId(), registration.getEndpoint(), model, encoder);

                        coapRequestBuilder.visit(readRequest);
                        Request request = coapRequestBuilder.getRequest();


                        Response send = lwServer.coap().send(registration, request);

                        System.out.println(send);
                        System.out.println("-------");

//                        if (send.isSuccess()) {
//                            LOG.error("Read success:" + send.getContent());
//                        } else {
//                            LOG.error("Read failed:");
//                        }
                    } catch (Exception e) {
                        LOG.error("read error:\n" + ExceptionUtils.getFullStackTrace(e));
                    }
                    //write all
                    try {
                        String url = objectLinks[1].getUrl();
                        String[] split = url.split("/");

                        WriteResponse send = lwServer.send(registration, new WriteRequest(Integer.parseInt(split[1]), Integer.parseInt(split[2]), 0, 123));
                        if (send.isSuccess()) {
                            LOG.error("write success");
                        } else {
                            LOG.error("write failed");
                        }
                    } catch (Exception e) {
                        LOG.error("write error:\n" + ExceptionUtils.getFullStackTrace(e));
                    }
                }
            }

            @Override
            public void updated(RegistrationUpdate update, Registration updatedReg, Registration previousReg) {
                LOG.error("device is still here: " + updatedReg.getEndpoint() + ":" + updatedReg.getId());
                try {

                    ReadResponse response = lwServer.send(updatedReg, new ReadRequest(3));
                    if (response.isSuccess()) {
                        LOG.error("Device time:" + ((LwM2mResource) response.getContent()).getValue());
                    } else {
                        LOG.error("Failed to read:" + response.getCode() + " " + response.getErrorMessage());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void unregistered(Registration registration, Collection<Observation> observations, boolean expired, Registration newReg) {
                LOG.error("device left: " + registration.getEndpoint() + ":" + registration.getId());
            }
        });

        final Random random = new Random();

        lwServer.getObservationService().addListener(new ObservationListener() {
            @Override
            public void newObservation(Observation observation, Registration registration) {
                LOG.error("create a Observation:" + observation);
            }

            @Override
            public void cancelled(Observation observation) {
                LOG.error("Observation cancel:" + observation);
            }

            @Override
            public void onResponse(Observation observation, Registration registration, ObserveResponse response) {
                final LwM2mNode content = response.getContent();
                if (content instanceof LwM2mSingleResource) {
                    int id = content.getId();
                    ResourceModel.Type type = ((LwM2mSingleResource) content).getType();
                    Object value = ((LwM2mSingleResource) content).getValue();
                    LOG.error("receive notify:id" + id + ",type:" + type + ",value:" + value);
                } else {
                    LOG.error("receive notify:" + content);
                }

                try {
                    ReadResponse send = lwServer.send(registration, new ReadRequest(registration.getObjectLinks()[random.nextInt(registration.getObjectLinks().length - 2) + 1].getUrl()));
                    if (send.isSuccess()) {
                        LOG.error("read success:" + send.getContent());
                    } else {
                        LOG.error("read failed");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Observation observation, Registration registration, Exception error) {
                LOG.error("notify error:\n" + ExceptionUtils.getFullStackTrace(error));
            }
        });
        return lwServer;
    }
}
