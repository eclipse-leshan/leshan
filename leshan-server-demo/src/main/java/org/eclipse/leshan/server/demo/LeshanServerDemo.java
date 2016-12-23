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
 *******************************************************************************/
package org.eclipse.leshan.server.demo;

import java.math.BigInteger;
import java.net.BindException;
import java.net.URI;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.cluster.RedisRegistrationStore;
import org.eclipse.leshan.server.cluster.RedisSecurityStore;
import org.eclipse.leshan.server.demo.servlet.ClientServlet;
import org.eclipse.leshan.server.demo.servlet.EventServlet;
import org.eclipse.leshan.server.demo.servlet.ObjectSpecServlet;
import org.eclipse.leshan.server.demo.servlet.SecurityServlet;
import org.eclipse.leshan.server.impl.FileSecurityStore;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.StandardModelProvider;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.util.Pool;

public class LeshanServerDemo {

    private static final Logger LOG = LoggerFactory.getLogger(LeshanServerDemo.class);

    private final static String USAGE = "java -jar leshan-server-demo.jar [OPTION]";
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
        options.addOption("r", "redis", true,
                "Set the location of the Redis database for running in cluster mode. The URL is in the format of: 'redis://:password@hostname:port/db_number'\nExample without DB and password: 'redis://localhost:6379'\nDefault: none, no Redis connection.");
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
            System.err.println("Unexpected option or arguments : " + cl.getArgList());
            formatter.printHelp(USAGE, null, options, FOOTER);
            return;
        }

        // get local address
        String localAddress = System.getenv("COAPHOST");
        if (cl.hasOption("lh")) {
            localAddress = cl.getOptionValue("lh");
        }
        String localPortOption = System.getenv("COAPPORT");
        if (cl.hasOption("lp")) {
            localPortOption = cl.getOptionValue("lp");
        }
        int localPort = LeshanServerBuilder.PORT;
        if (localPortOption != null) {
            localPort = Integer.parseInt(localPortOption);
        }

        // get secure local address
        String secureLocalAddress = System.getenv("COAPSHOST");
        if (cl.hasOption("slh")) {
            secureLocalAddress = cl.getOptionValue("slh");
        }
        String secureLocalPortOption = System.getenv("COAPSPORT");
        if (cl.hasOption("slp")) {
            secureLocalPortOption = cl.getOptionValue("slp");
        }
        int secureLocalPort = LeshanServerBuilder.PORT_DTLS;
        if (secureLocalPortOption != null) {
            secureLocalPort = Integer.parseInt(secureLocalPortOption);
        }

        // get http port
        String webPortOption = System.getenv("WEBPORT");
        if (cl.hasOption("wp")) {
            webPortOption = cl.getOptionValue("wp");
        }
        int webPort = 8080;
        if (webPortOption != null) {
            webPort = Integer.parseInt(webPortOption);
        }

        // get the Redis hostname:port
        String redisUrl = null;
        if (cl.hasOption("r")) {
            redisUrl = cl.getOptionValue("r");
        }

        try {
            createAndStartServer(webPort, localAddress, localPort, secureLocalAddress, secureLocalPort, redisUrl);
        } catch (BindException e) {
            System.err.println(
                    String.format("Web port %s is alreay used, you could change it using 'webport' option.", webPort));
            formatter.printHelp(USAGE, null, options, FOOTER);
        } catch (Exception e) {
            LOG.error("Jetty stopped with unexcepted error ...", e);
        }
    }

    public static void createAndStartServer(int webPort, String localAddress, int localPort, String secureLocalAddress,
            int secureLocalPort, String redisUrl) throws Exception {
        // Prepare LWM2M server
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(localAddress, localPort);
        builder.setLocalSecureAddress(secureLocalAddress, secureLocalPort);
        builder.setEncoder(new DefaultLwM2mNodeEncoder());
        LwM2mNodeDecoder decoder = new DefaultLwM2mNodeDecoder();
        builder.setDecoder(decoder);

        // connect to redis if needed
        Pool<Jedis> jedis = null;
        if (redisUrl != null) {
            // TODO: support sentinel pool and make pool configurable
            jedis = new JedisPool(new URI(redisUrl));
        }

        // Get public and private server key
        PrivateKey privateKey = null;
        PublicKey publicKey = null;
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
            KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                    parameterSpec);
            KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);

            // Get keys
            publicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            privateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);
            builder.setPublicKey(publicKey);
            builder.setPrivateKey(privateKey);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidParameterSpecException e) {
            LOG.error("Unable to initialize RPK.", e);
            System.exit(-1);
        }

        // Define model provider
        LwM2mModelProvider modelProvider = new StandardModelProvider();
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
        LeshanServer lwServer = builder.build();

        // Now prepare Jetty
        Server server = new Server(webPort);
        WebAppContext root = new WebAppContext();
        root.setContextPath("/");
        root.setResourceBase(LeshanServerDemo.class.getClassLoader().getResource("webapp").toExternalForm());
        root.setParentLoaderPriority(true);
        server.setHandler(root);

        // Create Servlet
        EventServlet eventServlet = new EventServlet(lwServer, lwServer.getSecureAddress().getPort());
        ServletHolder eventServletHolder = new ServletHolder(eventServlet);
        root.addServlet(eventServletHolder, "/event/*");

        ServletHolder clientServletHolder = new ServletHolder(
                new ClientServlet(lwServer, lwServer.getSecureAddress().getPort()));
        root.addServlet(clientServletHolder, "/api/clients/*");

        ServletHolder securityServletHolder = new ServletHolder(
                new SecurityServlet(securityStore, publicKey));
        root.addServlet(securityServletHolder, "/api/security/*");

        ServletHolder objectSpecServletHolder = new ServletHolder(new ObjectSpecServlet(lwServer.getModelProvider()));
        root.addServlet(objectSpecServletHolder, "/api/objectspecs/*");

        // Start Jetty & Leshan
        lwServer.start();
        server.start();
        LOG.info("Web server started at {}.", server.getURI());
    }
}
