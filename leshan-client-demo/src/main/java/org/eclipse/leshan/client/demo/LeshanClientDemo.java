/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
 *******************************************************************************/
package org.eclipse.leshan.client.demo;

import static org.eclipse.leshan.client.object.Security.*;
import static org.eclipse.leshan.core.LwM2mId.*;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.ClientHandshaker;
import org.eclipse.californium.scandium.dtls.DTLSContext;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.Handshaker;
import org.eclipse.californium.scandium.dtls.ResumingClientHandshaker;
import org.eclipse.californium.scandium.dtls.ResumingServerHandshaker;
import org.eclipse.californium.scandium.dtls.ServerHandshaker;
import org.eclipse.californium.scandium.dtls.SessionAdapter;
import org.eclipse.californium.scandium.dtls.SessionId;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.datacollector.DataSender;
import org.eclipse.leshan.client.datacollector.ManualDataSender;
import org.eclipse.leshan.client.demo.cli.LeshanClientDemoCLI;
import org.eclipse.leshan.client.demo.cli.interactive.InteractiveCommands;
import org.eclipse.leshan.client.engine.DefaultRegistrationEngineFactory;
import org.eclipse.leshan.client.object.LwM2mTestObject;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.resource.listener.ObjectsListenerAdapter;
import org.eclipse.leshan.core.demo.LwM2mDemoConstant;
import org.eclipse.leshan.core.demo.cli.ShortErrorMessageHandler;
import org.eclipse.leshan.core.demo.cli.interactive.InteractiveCLI;
import org.eclipse.leshan.core.model.LwM2mModelRepository;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;

public class LeshanClientDemo {

    static {
        // Define a default logback.configurationFile
        String property = System.getProperty("logback.configurationFile");
        if (property == null) {
            System.setProperty("logback.configurationFile", "logback-config.xml");
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(LeshanClientDemo.class);
    private static final int OBJECT_ID_TEMPERATURE_SENSOR = 3303;
    private static final int OBJECT_ID_LWM2M_TEST_OBJECT = 3442;
    private static final String CF_CONFIGURATION_FILENAME = "Californium3.client.properties";
    private static final String CF_CONFIGURATION_HEADER = "Leshan Client Demo - " + Configuration.DEFAULT_HEADER;

    public static void main(String[] args) {

        // Parse command line
        LeshanClientDemoCLI cli = new LeshanClientDemoCLI();
        CommandLine command = new CommandLine(cli).setParameterExceptionHandler(new ShortErrorMessageHandler());
        // Handle exit code error
        int exitCode = command.execute(args);
        if (exitCode != 0)
            System.exit(exitCode);
        // Handle help or version command
        if (command.isUsageHelpRequested() || command.isVersionHelpRequested())
            System.exit(0);

        try {
            // Create Client
            LwM2mModelRepository repository = createModel(cli);
            final LeshanClient client = createClient(cli, repository);

            // Print commands help
            InteractiveCLI console = new InteractiveCLI(new InteractiveCommands(client, repository));
            console.showHelp();

            // Start the client
            client.start();

            // De-register on shutdown and stop client.
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    client.destroy(true); // send de-registration request before destroy
                }
            });

            // Start interactive console
            console.start();

        } catch (Exception e) {

            // Handler Execution Error
            PrintWriter printer = command.getErr();
            printer.print(command.getColorScheme().errorText("Unable to create and start client ..."));
            printer.printf("%n%n");
            printer.print(command.getColorScheme().stackTraceText(e));
            printer.flush();
            System.exit(1);
        }
    }

    private static LwM2mModelRepository createModel(LeshanClientDemoCLI cli) throws Exception {

        List<ObjectModel> models = ObjectLoader.loadAllDefault();
        models.addAll(ObjectLoader.loadDdfResources("/models", LwM2mDemoConstant.modelPaths));
        if (cli.main.modelsFolder != null) {
            models.addAll(ObjectLoader.loadObjectsFromDir(cli.main.modelsFolder, true));
        }

        return new LwM2mModelRepository(models);
    }

    public static LeshanClient createClient(LeshanClientDemoCLI cli, LwM2mModelRepository repository) throws Exception {
        // create Leshan client from command line option
        final MyLocation locationInstance = new MyLocation(cli.location.position.latitude,
                cli.location.position.longitude, cli.location.scaleFactor);

        // Initialize object list
        final ObjectsInitializer initializer = new ObjectsInitializer(repository.getLwM2mModel());
        if (cli.main.bootstrap) {
            if (cli.identity.isPSK()) {
                initializer.setInstancesForObject(SECURITY, pskBootstrap(cli.main.url,
                        cli.identity.getPsk().identity.getBytes(), cli.identity.getPsk().sharekey.getBytes()));
                initializer.setClassForObject(SERVER, Server.class);
            } else if (cli.identity.isRPK()) {
                initializer.setInstancesForObject(SECURITY,
                        rpkBootstrap(cli.main.url, cli.identity.getRPK().cpubk.getEncoded(),
                                cli.identity.getRPK().cprik.getEncoded(), cli.identity.getRPK().spubk.getEncoded()));
                initializer.setClassForObject(SERVER, Server.class);
            } else if (cli.identity.isx509()) {
                initializer.setInstancesForObject(SECURITY,
                        x509Bootstrap(cli.main.url, cli.identity.getX509().ccert.getEncoded(),
                                cli.identity.getX509().cprik.getEncoded(), cli.identity.getX509().scert.getEncoded(),
                                cli.identity.getX509().certUsage.code));
                initializer.setClassForObject(SERVER, Server.class);
            } else {
                initializer.setInstancesForObject(SECURITY, noSecBootstap(cli.main.url));
                initializer.setClassForObject(SERVER, Server.class);
            }
        } else {
            if (cli.identity.isPSK()) {
                initializer.setInstancesForObject(SECURITY, psk(cli.main.url, 123,
                        cli.identity.getPsk().identity.getBytes(), cli.identity.getPsk().sharekey.getBytes()));
                initializer.setInstancesForObject(SERVER, new Server(123, cli.main.lifetimeInSec));
            } else if (cli.identity.isRPK()) {
                initializer.setInstancesForObject(SECURITY,
                        rpk(cli.main.url, 123, cli.identity.getRPK().cpubk.getEncoded(),
                                cli.identity.getRPK().cprik.getEncoded(), cli.identity.getRPK().spubk.getEncoded()));
                initializer.setInstancesForObject(SERVER, new Server(123, cli.main.lifetimeInSec));
            } else if (cli.identity.isx509()) {
                initializer.setInstancesForObject(SECURITY,
                        x509(cli.main.url, 123, cli.identity.getX509().ccert.getEncoded(),
                                cli.identity.getX509().cprik.getEncoded(), cli.identity.getX509().scert.getEncoded(),
                                cli.identity.getX509().certUsage.code));
                initializer.setInstancesForObject(SERVER, new Server(123, cli.main.lifetimeInSec));
            } else {
                initializer.setInstancesForObject(SECURITY, noSec(cli.main.url, 123));
                initializer.setInstancesForObject(SERVER, new Server(123, cli.main.lifetimeInSec));
            }
        }
        initializer.setInstancesForObject(DEVICE, new MyDevice());
        initializer.setInstancesForObject(LOCATION, locationInstance);
        initializer.setInstancesForObject(OBJECT_ID_TEMPERATURE_SENSOR, new RandomTemperatureSensor());
        initializer.setInstancesForObject(OBJECT_ID_LWM2M_TEST_OBJECT, new LwM2mTestObject());

        List<LwM2mObjectEnabler> enablers = initializer.createAll();

        // Create CoAP Config
        File configFile = new File(CF_CONFIGURATION_FILENAME);
        Configuration coapConfig = LeshanClientBuilder.createDefaultCoapConfiguration();
        // these configuration values are always overwritten by CLI
        // therefore set them to transient.
        coapConfig.setTransient(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY);
        coapConfig.setTransient(DtlsConfig.DTLS_CONNECTION_ID_LENGTH);
        if (configFile.isFile()) {
            coapConfig.load(configFile);
        } else {
            coapConfig.store(configFile, CF_CONFIGURATION_HEADER);
        }

        // Create DTLS Config
        DtlsConnectorConfig.Builder dtlsConfig = DtlsConnectorConfig.builder(coapConfig);
        dtlsConfig.set(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, !cli.dtls.supportDeprecatedCiphers);
        dtlsConfig.set(DtlsConfig.DTLS_CONNECTION_ID_LENGTH, cli.dtls.cid);
        if (cli.dtls.ciphers != null) {
            dtlsConfig.set(DtlsConfig.DTLS_CIPHER_SUITES, cli.dtls.ciphers);
        }

        // Configure Registration Engine
        DefaultRegistrationEngineFactory engineFactory = new DefaultRegistrationEngineFactory();
        if (cli.main.comPeriodInSec != null)
            engineFactory.setCommunicationPeriod(cli.main.comPeriodInSec * 1000);
        engineFactory.setReconnectOnUpdate(cli.dtls.reconnectOnUpdate);
        engineFactory.setResumeOnConnect(!cli.dtls.forceFullhandshake);
        engineFactory.setQueueMode(cli.main.queueMode);

        // Log Session lifecycle
        dtlsConfig.setSessionListener(new SessionAdapter() {

            private SessionId sessionIdentifier = null;

            @Override
            public void handshakeStarted(Handshaker handshaker) throws HandshakeException {
                if (handshaker instanceof ResumingServerHandshaker) {
                    LOG.info("DTLS abbreviated Handshake initiated by server : STARTED ...");
                } else if (handshaker instanceof ServerHandshaker) {
                    LOG.info("DTLS Full Handshake initiated by server : STARTED ...");
                } else if (handshaker instanceof ResumingClientHandshaker) {
                    sessionIdentifier = handshaker.getSession().getSessionIdentifier();
                    LOG.info("DTLS abbreviated Handshake initiated by client : STARTED ...");
                } else if (handshaker instanceof ClientHandshaker) {
                    LOG.info("DTLS Full Handshake initiated by client : STARTED ...");
                }
            }

            @Override
            public void contextEstablished(Handshaker handshaker, DTLSContext establishedContext)
                    throws HandshakeException {
                if (handshaker instanceof ResumingServerHandshaker) {
                    LOG.info("DTLS abbreviated Handshake initiated by server : SUCCEED");
                } else if (handshaker instanceof ServerHandshaker) {
                    LOG.info("DTLS Full Handshake initiated by server : SUCCEED");
                } else if (handshaker instanceof ResumingClientHandshaker) {
                    if (sessionIdentifier != null
                            && sessionIdentifier.equals(handshaker.getSession().getSessionIdentifier())) {
                        LOG.info("DTLS abbreviated Handshake initiated by client : SUCCEED");
                    } else {
                        LOG.info("DTLS abbreviated turns into Full Handshake initiated by client : SUCCEED");
                    }
                } else if (handshaker instanceof ClientHandshaker) {
                    LOG.info("DTLS Full Handshake initiated by client : SUCCEED");
                }
            }

            @Override
            public void handshakeFailed(Handshaker handshaker, Throwable error) {
                // get cause
                String cause;
                if (error != null) {
                    if (error.getMessage() != null) {
                        cause = error.getMessage();
                    } else {
                        cause = error.getClass().getName();
                    }
                } else {
                    cause = "unknown cause";
                }

                if (handshaker instanceof ResumingServerHandshaker) {
                    LOG.info("DTLS abbreviated Handshake initiated by server : FAILED ({})", cause);
                } else if (handshaker instanceof ServerHandshaker) {
                    LOG.info("DTLS Full Handshake initiated by server : FAILED ({})", cause);
                } else if (handshaker instanceof ResumingClientHandshaker) {
                    LOG.info("DTLS abbreviated Handshake initiated by client : FAILED ({})", cause);
                } else if (handshaker instanceof ClientHandshaker) {
                    LOG.info("DTLS Full Handshake initiated by client : FAILED ({})", cause);
                }
            }
        });

        // Create client
        LeshanClientBuilder builder = new LeshanClientBuilder(cli.main.endpoint);
        builder.setLocalAddress(cli.main.localAddress, cli.main.localPort);
        builder.setObjects(enablers);
        builder.setDataSenders(new ManualDataSender());
        builder.setCoapConfig(coapConfig);
        if (cli.identity.isx509())
            builder.setTrustStore(cli.identity.getX509().trustStore);
        builder.setDtlsConfig(dtlsConfig);
        builder.setRegistrationEngineFactory(engineFactory);
        if (cli.main.supportOldFormat) {
            builder.setDecoder(new DefaultLwM2mDecoder(true));
            builder.setEncoder(new DefaultLwM2mEncoder(true));
        }
        builder.setAdditionalAttributes(cli.main.additionalAttributes);
        builder.setBootstrapAdditionalAttributes(cli.main.bsAdditionalAttributes);
        final LeshanClient client = builder.build();

        client.getObjectTree().addListener(new ObjectsListenerAdapter() {

            @Override
            public void objectRemoved(LwM2mObjectEnabler object) {
                LOG.info("Object {} v{} disabled.", object.getId(), object.getObjectModel().version);
            }

            @Override
            public void objectAdded(LwM2mObjectEnabler object) {
                LOG.info("Object {} v{} enabled.", object.getId(), object.getObjectModel().version);
            }
        });

        return client;
    }
}
