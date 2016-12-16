/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
package org.eclipse.leshan.server.cluster;

import java.net.URI;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.StandardModelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.util.Pool;

/**
 * The main entry point for the Leshan LWM2M cluster server node.
 * 
 * Will start a LWM2M server which would be part of a large cluster group. This group of cluster node will share state
 * and communicate using Redis. So they all need too be connected to the same redis server.
 */
public class LeshanClusterServer {
    private static final Logger LOG = LoggerFactory.getLogger(LeshanClusterServer.class);

    private final static String USAGE = "java -jar leshan-server-cluster.jar [OPTION]";
    private final static String FOOTER = "All options could be passed using environment variables.(using long option name in uppercase)";

    public static void main(String[] args) {
        // Define options for command line tools
        Options options = new Options();

        options.addOption("h", "help", false, "Display help information.");
        options.addOption("n", "instanceID", true, "Sets the unique identifier of this instance in the cluster.");
        options.addOption("lh", "coaphost", true, "Sets the local CoAP address.\n  Default: any local address.");
        options.addOption("lp", "coapport", true, "Sets the local CoAP port.\n  Default: 5683.");
        options.addOption("slh", "coapshost", true, "Sets the local secure CoAP address.\nDefault: any local address.");
        options.addOption("slp", "coapsport", true, "Sets the local secure CoAP port.\nDefault: 5684.");
        options.addOption("r", "redis", true,
                "Sets the location of the Redis database. The URL is in the format of: 'redis://:password@hostname:port/db_number'\n\nDefault: 'redis://localhost:6379'.");
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

        // get cluster instance Id
        String clusterInstanceId = System.getenv("INSTANCEID");
        if (cl.hasOption("n")) {
            clusterInstanceId = cl.getOptionValue("n");
        }
        if (clusterInstanceId == null) {
            System.err.println("InstanceId is mandatory !");
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

        // get the Redis hostname:port
        String redisUrl = "redis://localhost:6379";
        if (cl.hasOption("r")) {
            redisUrl = cl.getOptionValue("r");
        }

        try {
            createAndStartServer(clusterInstanceId, localAddress, localPort, secureLocalAddress,
                    secureLocalPort, redisUrl);
        }catch (Exception e) {
            LOG.error("Jetty stopped with unexcepted error ...", e);
        }
    }

    public static void createAndStartServer(String clusterInstanceId, String localAddress, int localPort,
            String secureLocalAddress, int secureLocalPort, String redisUrl) throws Exception {
        // Create Redis connector.
        // TODO: support sentinel pool and make pool configurable
        Pool<Jedis> jedis = new JedisPool(new URI(redisUrl));

        // Prepare LWM2M server.
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(localAddress, localPort);
        builder.setLocalSecureAddress(secureLocalAddress, secureLocalPort);
        DefaultLwM2mNodeDecoder decoder = new DefaultLwM2mNodeDecoder();
        builder.setDecoder(decoder);

        LwM2mModelProvider modelProvider = new StandardModelProvider();
        builder.setObjectModelProvider(modelProvider);

        RedisRegistrationStore registrationStore = new RedisRegistrationStore(jedis);
        builder.setRegistrationStore(registrationStore);

        // TODO add support of public and private server key
        builder.setSecurityStore(new RedisSecurityStore(jedis));

        // Create and start LWM2M server
        LeshanServer lwServer = builder.build();

        // Create Clustering support
        RedisTokenHandler tokenHandler = new RedisTokenHandler(jedis, clusterInstanceId);
        new RedisRequestResponseHandler(jedis, lwServer, lwServer.getRegistrationService(), tokenHandler,
                lwServer.getObservationService());
        lwServer.getRegistrationService().addListener(tokenHandler);
        lwServer.getRegistrationService().addListener(new RedisRegistrationEventPublisher(jedis));

        // Start Jetty & Leshan
        lwServer.start();
    }
}
