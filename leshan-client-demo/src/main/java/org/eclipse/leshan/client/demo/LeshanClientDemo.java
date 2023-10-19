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

import static org.eclipse.leshan.client.object.Security.noSec;
import static org.eclipse.leshan.client.object.Security.noSecBootstrap;
import static org.eclipse.leshan.client.object.Security.oscoreOnly;
import static org.eclipse.leshan.client.object.Security.oscoreOnlyBootstrap;
import static org.eclipse.leshan.client.object.Security.psk;
import static org.eclipse.leshan.client.object.Security.pskBootstrap;
import static org.eclipse.leshan.client.object.Security.rpk;
import static org.eclipse.leshan.client.object.Security.rpkBootstrap;
import static org.eclipse.leshan.client.object.Security.x509;
import static org.eclipse.leshan.client.object.Security.x509Bootstrap;
import static org.eclipse.leshan.core.LwM2mId.DEVICE;
import static org.eclipse.leshan.core.LwM2mId.LOCATION;
import static org.eclipse.leshan.core.LwM2mId.OSCORE;
import static org.eclipse.leshan.core.LwM2mId.SECURITY;
import static org.eclipse.leshan.core.LwM2mId.SERVER;

import java.io.PrintWriter;
import java.util.List;

import org.eclipse.leshan.client.LeshanClient;
import org.eclipse.leshan.client.LeshanClientBuilder;
import org.eclipse.leshan.client.demo.cli.LeshanClientDemoCLI;
import org.eclipse.leshan.client.demo.cli.interactive.InteractiveCommands;
import org.eclipse.leshan.client.demo.cli.transport.TransportCommand;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpointsProvider;
import org.eclipse.leshan.client.engine.DefaultRegistrationEngineFactory;
import org.eclipse.leshan.client.object.LwM2mTestObject;
import org.eclipse.leshan.client.object.Oscore;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.resource.listener.ObjectsListenerAdapter;
import org.eclipse.leshan.client.send.ManualDataSender;
import org.eclipse.leshan.core.demo.LwM2mDemoConstant;
import org.eclipse.leshan.core.demo.cli.PicocliUtil;
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
import picocli.CommandLine.RunAll;

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

    public static void main(String[] args) {

        // Parse command line
        LeshanClientDemoCLI cli = new LeshanClientDemoCLI();
        CommandLine command = new CommandLine(cli) //
                .setExecutionStrategy(new RunAll()).setParameterExceptionHandler(new ShortErrorMessageHandler());

        // Handle exit code error
        int exitCode = command.execute(args);
        if (exitCode != 0)
            System.exit(exitCode);
        // Handle help or version command
        if (command.isUsageHelpRequested() || command.isVersionHelpRequested() || PicocliUtil.isHelpRequested(command))
            System.exit(0);

        try {
            // Create Client
            LwM2mModelRepository repository = createModel(cli);
            List<LwM2mClientEndpointsProvider> endpointProviders = createEndpointsProviders(cli, command);
            final LeshanClient client = createClient(cli, repository, endpointProviders);

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

    private static List<LwM2mClientEndpointsProvider> createEndpointsProviders(LeshanClientDemoCLI cli,
            CommandLine commandLine) {
        // Get transport command
        CommandLine transportCommand = commandLine.getSubcommands().get("transport");
        List<LwM2mClientEndpointsProvider> providers = ((TransportCommand) transportCommand.getCommandSpec()
                .userObject()).createEndpointsProviders(cli, transportCommand);

        return providers;
    }

    public static LeshanClient createClient(LeshanClientDemoCLI cli, LwM2mModelRepository repository,
            List<LwM2mClientEndpointsProvider> endpointProviders) throws Exception {
        // create Leshan client from command line option
        final MyLocation locationInstance = new MyLocation(cli.location.position.latitude,
                cli.location.position.longitude, cli.location.scaleFactor);

        // Initialize object list
        final ObjectsInitializer initializer = new ObjectsInitializer(repository.getLwM2mModel());
        // handle OSCORE
        Integer oscoreObjectInstanceId;
        if (cli.oscore != null) {
            oscoreObjectInstanceId = 12345;
            Oscore oscoreObject = new Oscore(oscoreObjectInstanceId, cli.oscore.getOscoreSetting());
            initializer.setInstancesForObject(OSCORE, oscoreObject);
        } else {
            oscoreObjectInstanceId = null;
            initializer.setClassForObject(OSCORE, Oscore.class);
        }
        if (cli.main.bootstrap) {
            if (cli.identity.isPSK()) {
                // TODO OSCORE support OSCORE with DTLS/PSK
                initializer.setInstancesForObject(SECURITY, pskBootstrap(cli.main.url,
                        cli.identity.getPsk().identity.getBytes(), cli.identity.getPsk().sharekey.getBytes()));
                initializer.setClassForObject(SERVER, Server.class);
            } else if (cli.identity.isRPK()) {
                // TODO OSCORE support OSCORE with DTLS/RPK
                initializer.setInstancesForObject(SECURITY,
                        rpkBootstrap(cli.main.url, cli.identity.getRPK().cpubk.getEncoded(),
                                cli.identity.getRPK().cprik.getEncoded(), cli.identity.getRPK().spubk.getEncoded()));
                initializer.setClassForObject(SERVER, Server.class);
            } else if (cli.identity.isx509()) {
                // TODO OSCORE support OSCORE with DTLS/X509
                initializer.setInstancesForObject(SECURITY,
                        x509Bootstrap(cli.main.url, cli.identity.getX509().ccert.getEncoded(),
                                cli.identity.getX509().cprik.getEncoded(), cli.identity.getX509().scert.getEncoded(),
                                cli.identity.getX509().certUsage.code));
                initializer.setClassForObject(SERVER, Server.class);
            } else {
                if (oscoreObjectInstanceId != null) {
                    initializer.setInstancesForObject(SECURITY,
                            oscoreOnlyBootstrap(cli.main.url, oscoreObjectInstanceId));
                } else {
                    initializer.setInstancesForObject(SECURITY, noSecBootstrap(cli.main.url));
                }
                initializer.setClassForObject(SERVER, Server.class);
            }
        } else {
            if (cli.identity.isPSK()) {
                // TODO OSCORE support OSCORE with DTLS/PSK
                initializer.setInstancesForObject(SECURITY, psk(cli.main.url, 123,
                        cli.identity.getPsk().identity.getBytes(), cli.identity.getPsk().sharekey.getBytes()));
                initializer.setInstancesForObject(SERVER, new Server(123, cli.main.lifetimeInSec));
            } else if (cli.identity.isRPK()) {
                // TODO OSCORE support OSCORE with DTLS/RPK
                initializer.setInstancesForObject(SECURITY,
                        rpk(cli.main.url, 123, cli.identity.getRPK().cpubk.getEncoded(),
                                cli.identity.getRPK().cprik.getEncoded(), cli.identity.getRPK().spubk.getEncoded()));
                initializer.setInstancesForObject(SERVER, new Server(123, cli.main.lifetimeInSec));
            } else if (cli.identity.isx509()) {
                // TODO OSCORE support OSCORE with DTLS/X509
                initializer.setInstancesForObject(SECURITY,
                        x509(cli.main.url, 123, cli.identity.getX509().ccert.getEncoded(),
                                cli.identity.getX509().cprik.getEncoded(), cli.identity.getX509().scert.getEncoded(),
                                cli.identity.getX509().certUsage.code));
                initializer.setInstancesForObject(SERVER, new Server(123, cli.main.lifetimeInSec));
            } else {
                if (oscoreObjectInstanceId != null) {
                    initializer.setInstancesForObject(SECURITY, oscoreOnly(cli.main.url, 123, oscoreObjectInstanceId));
                } else {
                    initializer.setInstancesForObject(SECURITY, noSec(cli.main.url, 123));
                }
                initializer.setInstancesForObject(SERVER, new Server(123, cli.main.lifetimeInSec));
            }
        }
        initializer.setInstancesForObject(DEVICE, new MyDevice());
        initializer.setInstancesForObject(LOCATION, locationInstance);
        initializer.setInstancesForObject(OBJECT_ID_TEMPERATURE_SENSOR, new RandomTemperatureSensor());
        initializer.setInstancesForObject(OBJECT_ID_LWM2M_TEST_OBJECT, new LwM2mTestObject());

        List<LwM2mObjectEnabler> enablers = initializer.createAll();

        // Configure Registration Engine
        DefaultRegistrationEngineFactory engineFactory = new DefaultRegistrationEngineFactory();
        if (cli.main.comPeriodInSec != null)
            engineFactory.setCommunicationPeriod(cli.main.comPeriodInSec * 1000);
        engineFactory.setReconnectOnUpdate(cli.dtls.reconnectOnUpdate);
        engineFactory.setResumeOnConnect(!cli.dtls.forceFullhandshake);
        engineFactory.setQueueMode(cli.main.queueMode);

        // Create client
        LeshanClientBuilder builder = new LeshanClientBuilder(cli.main.endpoint);
        builder.setObjects(enablers);
        builder.setEndpointsProviders(endpointProviders);
        builder.setDataSenders(new ManualDataSender());
        if (cli.identity.isx509())
            builder.setTrustStore(cli.identity.getX509().trustStore);
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
