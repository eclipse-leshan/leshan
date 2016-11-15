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
import java.net.URISyntaxException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.CaliforniumObservationRegistryImpl;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.StandardModelProvider;

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

    private final static String USAGE = "java -jar leshan-server-cluster.jar [OPTION]";
    private final static String FOOTER = "Question? Bug report? Eclipse Leshan : https://eclipse.org/leshan";

    public static void main(String[] args) {
        // Define options for command line tools
        Options options = new Options();

        options.addOption("h", "help", false, "Display this help message.");
        options.addOption("n", "instanceID", true, "Sets the unique identifier of this instance in the cluster.");
        options.addOption("l", "coapaddressport", true,
                "Sets the local CoAP bind address and port in form [IP]:[PORT].\n  Default: 0.0.0.0:5683.");
        options.addOption("sl", "coapsaddressport", true,
                "Sets the local secure CoAP bind address and port, same format.\n  Default: 0.0.0.0:5684.");
        options.addOption("r", "redis", true,
                "Set the location of the Redis database. The URL is in the format of: 'redis://:password@hostname:port/db_number'\n\nDefault: 'redis://localhost:6379'.");
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
        String clusterInstanceId;
        if (cl.hasOption("n")) {
            clusterInstanceId = cl.getOptionValue("n");
        } else {
            System.err.println("InstanceId is mandatory !");
            formatter.printHelp(USAGE, null, options, FOOTER);
            return;
        }

        // get local address/port
        String localAddressPort = "0.0.0.0:" + LeshanServerBuilder.PORT;
        if (cl.hasOption("l")) {
            localAddressPort = cl.getOptionValue("l");
        }

        // parse
        int idx = localAddressPort.lastIndexOf(":");
        if (idx < 0) {
            System.err.println("Malformed address:port configuration: '" + localAddressPort + "'");
            formatter.printHelp(USAGE, null, options, FOOTER);
            return;
        }

        int localPort;
        try {
            localPort = Integer.valueOf(localAddressPort.substring(idx + 1));
        } catch (NumberFormatException ex) {
            System.err
                    .println("Malformed address:port configuration: '" + localAddressPort + "', port is not a number");
            formatter.printHelp(USAGE, null, options, FOOTER);
            return;
        }

        String localAddress = localAddressPort.substring(0, idx);

        // get secure local address
        String secureLocalAddressPort = "0.0.0.0:" + LeshanServerBuilder.PORT_DTLS;
        if (cl.hasOption("sl")) {
            localAddressPort = cl.getOptionValue("sl");
        }
        // parse
        idx = secureLocalAddressPort.lastIndexOf(":");
        if (idx < 0) {
            System.err.println("Malformed address:port configuration: '" + secureLocalAddressPort + "'");
            formatter.printHelp(USAGE, null, options, FOOTER);
            return;
        }

        int secureLocalPort;
        try {
            secureLocalPort = Integer.valueOf(secureLocalAddressPort.substring(idx + 1));
        } catch (NumberFormatException ex) {
            System.err.println(
                    "Malformed address:port configuration: '" + secureLocalAddressPort + "', port is not a number");
            formatter.printHelp(USAGE, null, options, FOOTER);
            return;
        }
        String secureLocalAddress = secureLocalAddressPort.substring(0, idx);

        // get the Redis hostname:port
        String redisUrl = "redis://localhost:6379";
        if (cl.hasOption("r")) {
            redisUrl = cl.getOptionValue("r");
        }

        URI redisUri;
        try {
            redisUri = new URI(redisUrl);
        } catch (URISyntaxException e) {
            System.err.println("Malformed redis URL: '" + redisUrl + "'");
            formatter.printHelp(USAGE, null, options, FOOTER);
            return;
        }
        createAndStartServer(clusterInstanceId, localAddress, localPort, secureLocalAddress, secureLocalPort, redisUri);
    }

    public static void createAndStartServer(String clusterInstanceId, String localAddress, int localPort,
            String secureLocalAddress, int secureLocalPort, URI redisUri) {
        // Create Redis connector.
        // TODO: support sentinel pool and make pool configurable
        Pool<Jedis> jedis = new JedisPool(redisUri);

        // Prepare LWM2M server.
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(localAddress, localPort);
        builder.setLocalSecureAddress(secureLocalAddress, secureLocalPort);
        DefaultLwM2mNodeDecoder decoder = new DefaultLwM2mNodeDecoder();
        builder.setDecoder(decoder);

        LwM2mModelProvider modelProvider = new StandardModelProvider();
        builder.setObjectModelProvider(modelProvider);

        ClientRegistry clientRegistry = new RedisClientRegistry(jedis);
        builder.setClientRegistry(clientRegistry);

        // TODO add support of public and private server key
        builder.setSecurityRegistry(new RedisSecurityRegistry(jedis, null, null));

        builder.setObservationRegistry(new CaliforniumObservationRegistryImpl(new RedisObservationStore(jedis),
                clientRegistry, modelProvider, decoder));

        // Create and start LWM2M server
        LeshanServer lwServer = builder.build();

        // Create Clustering support
        RedisTokenHandler tokenHandler = new RedisTokenHandler(jedis, clusterInstanceId);
        new RedisRequestResponseHandler(jedis, lwServer, lwServer.getClientRegistry(), tokenHandler,
                lwServer.getObservationRegistry());
        clientRegistry.addListener(tokenHandler);
        clientRegistry.addListener(new RedisRegistrationEventPublisher(jedis));

        // Start Leshan
        lwServer.start();
    }
}
