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
package org.eclipse.leshan.demo.client;

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

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.leshan.client.LeshanClient;
import org.eclipse.leshan.client.LeshanClientBuilder;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpointsProvider;
import org.eclipse.leshan.client.engine.DefaultClientEndpointNameProvider;
import org.eclipse.leshan.client.engine.DefaultRegistrationEngineFactory;
import org.eclipse.leshan.client.object.LwM2mTestObject;
import org.eclipse.leshan.client.object.Oscore;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.resource.listener.ObjectsListenerAdapter;
import org.eclipse.leshan.client.send.ManualDataSender;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.model.LwM2mModelRepository;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.node.codec.text.LwM2mNodeTextDecoder;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.BootstrapWriteRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.BootstrapWriteResponse;
import org.eclipse.leshan.demo.LwM2mDemoConstant;
import org.eclipse.leshan.demo.cli.ShortErrorMessageHandler;
import org.eclipse.leshan.demo.cli.interactive.InteractiveCLI;
import org.eclipse.leshan.demo.client.cli.LeshanClientDemoCLI;
import org.eclipse.leshan.demo.client.cli.interactive.InteractiveCommands;
import org.eclipse.leshan.transport.californium.PrincipalMdcConnectionListener;
import org.eclipse.leshan.transport.californium.client.endpoint.CaliforniumClientEndpointFactory;
import org.eclipse.leshan.transport.californium.client.endpoint.CaliforniumClientEndpointsProvider;
import org.eclipse.leshan.transport.californium.client.endpoint.ClientProtocolProvider;
import org.eclipse.leshan.transport.californium.client.endpoint.coap.CoapOscoreProtocolProvider;
import org.eclipse.leshan.transport.californium.client.endpoint.coaps.CoapsClientEndpointFactory;
import org.eclipse.leshan.transport.californium.client.endpoint.coaps.CoapsClientProtocolProvider;
import org.eclipse.leshan.transport.javacoap.client.coaptcp.endpoint.JavaCoapTcpClientEndpointsProvider;
import org.eclipse.leshan.transport.javacoap.client.coaptcp.endpoint.JavaCoapsTcpClientEndpointsProvider;
import org.eclipse.leshan.transport.javacoap.client.endpoint.JavaCoapClientEndpointsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;

public class LeshanClientDemo {

    static {
        // Define a default log4j2.configurationFile
        String property = System.getProperty("log4j2.configurationFile");
        if (property == null) {
            System.setProperty("log4j2.configurationFile", "log4j2.xml");
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
        CommandLine command = new CommandLine(cli).setTrimQuotes(true)
                .setParameterExceptionHandler(new ShortErrorMessageHandler());
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
            BindingMode serverBindingMode = BindingMode.fromProtocol(Protocol.fromUri(cli.main.url));

            if (cli.identity.isPSK()) {
                // TODO OSCORE support OSCORE with DTLS/PSK
                initializer.setInstancesForObject(SECURITY, psk(cli.main.url, 123,
                        cli.identity.getPsk().identity.getBytes(), cli.identity.getPsk().sharekey.getBytes()));
                initializer.setInstancesForObject(SERVER, new Server(123, cli.main.lifetimeInSec,
                        EnumSet.of(serverBindingMode), false, serverBindingMode));
            } else if (cli.identity.isRPK()) {
                // TODO OSCORE support OSCORE with DTLS/RPK
                initializer.setInstancesForObject(SECURITY,
                        rpk(cli.main.url, 123, cli.identity.getRPK().cpubk.getEncoded(),
                                cli.identity.getRPK().cprik.getEncoded(), cli.identity.getRPK().spubk.getEncoded()));
                initializer.setInstancesForObject(SERVER, new Server(123, cli.main.lifetimeInSec,
                        EnumSet.of(serverBindingMode), false, serverBindingMode));
            } else if (cli.identity.isx509()) {
                // TODO OSCORE support OSCORE with DTLS/X509
                initializer.setInstancesForObject(SECURITY,
                        x509(cli.main.url, 123, cli.identity.getX509().ccert.getEncoded(),
                                cli.identity.getX509().cprik.getEncoded(), cli.identity.getX509().scert.getEncoded(),
                                cli.identity.getX509().certUsage.code));
                initializer.setInstancesForObject(SERVER, new Server(123, cli.main.lifetimeInSec,
                        EnumSet.of(serverBindingMode), false, serverBindingMode));
            } else {
                if (oscoreObjectInstanceId != null) {
                    initializer.setInstancesForObject(SECURITY, oscoreOnly(cli.main.url, 123, oscoreObjectInstanceId));
                } else {
                    initializer.setInstancesForObject(SECURITY, noSec(cli.main.url, 123));
                }
                initializer.setInstancesForObject(SERVER, new Server(123, cli.main.lifetimeInSec,
                        EnumSet.of(serverBindingMode), false, serverBindingMode));
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

        // Create Californium Endpoints Provider:
        // --------------------------------------
        // Define Custom CoAPS protocol provider
        CoapsClientProtocolProvider customCoapsProtocolProvider = new CoapsClientProtocolProvider() {
            @Override
            public CaliforniumClientEndpointFactory createDefaultEndpointFactory() {
                return new CoapsClientEndpointFactory() {

                    @Override
                    protected DtlsConnectorConfig.Builder createRootDtlsConnectorConfigBuilder(
                            Configuration configuration) {
                        Builder builder = super.createRootDtlsConnectorConfigBuilder(configuration);

                        // Add DTLS Session lifecycle logger
                        builder.setSessionListener(new DtlsSessionLogger());

                        // Add MDC for connection logs
                        if (cli.helpsOptions.getVerboseLevel() > 0)
                            builder.setConnectionListener(new PrincipalMdcConnectionListener());
                        return builder;
                    };
                };
            }
        };

        // Create client endpoints Provider
        List<ClientProtocolProvider> protocolProvider = new ArrayList<>();
        if (!cli.main.useJavaCoap) {
            protocolProvider.add(new CoapOscoreProtocolProvider());
        }
        protocolProvider.add(customCoapsProtocolProvider);
        CaliforniumClientEndpointsProvider.Builder endpointsBuilder = new CaliforniumClientEndpointsProvider.Builder(
                protocolProvider.toArray(new ClientProtocolProvider[protocolProvider.size()]));

        // Create Californium Configuration
        Configuration clientCoapConfig = endpointsBuilder.createDefaultConfiguration();

        // Set some DTLS stuff
        // These configuration values are always overwritten by CLI therefore set them to transient.
        clientCoapConfig.setTransient(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY);
        clientCoapConfig.setTransient(DtlsConfig.DTLS_CONNECTION_ID_LENGTH);
        clientCoapConfig.set(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, !cli.dtls.supportDeprecatedCiphers);
        clientCoapConfig.set(DtlsConfig.DTLS_CONNECTION_ID_LENGTH, cli.dtls.cid);

        // Persist configuration
        File configFile = new File(CF_CONFIGURATION_FILENAME);
        if (configFile.isFile()) {
            clientCoapConfig.load(configFile);
        } else {
            clientCoapConfig.store(configFile, CF_CONFIGURATION_HEADER);
        }
        if (cli.dtls.ciphers != null) {
            clientCoapConfig.set(DtlsConfig.DTLS_CIPHER_SUITES, cli.dtls.ciphers);
        }

        // Set Californium Configuration
        endpointsBuilder.setConfiguration(clientCoapConfig);
        endpointsBuilder.setClientAddress(cli.main.localAddress);

        // creates EndpointsProvider
        List<LwM2mClientEndpointsProvider> endpointsProvider = new ArrayList<>();
        endpointsProvider.add(endpointsBuilder.build());
        if (cli.main.useJavaCoap) {
            endpointsProvider.add(new JavaCoapClientEndpointsProvider());
        }
        endpointsProvider.add(new JavaCoapTcpClientEndpointsProvider());
        endpointsProvider.add(new JavaCoapsTcpClientEndpointsProvider());

        // Create client
        LeshanClientBuilder builder = new LeshanClientBuilder(
                new DefaultClientEndpointNameProvider(cli.main.endpoint, cli.main.endpointNameMode));
        builder.setObjects(enablers);
        builder.setEndpointsProviders(
                endpointsProvider.toArray(new LwM2mClientEndpointsProvider[endpointsProvider.size()]));
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

        // Handle Factory Bootstrap option
        if (cli.main.factoryBootstrap != null) {
            LwM2mNodeTextDecoder textDecoder = new LwM2mNodeTextDecoder();
            cli.main.factoryBootstrap.forEach((resourcePath, resourceValue) -> {
                BootstrapWriteResponse response = null;
                try {
                    // get resource from string resource value
                    LwM2mSingleResource resource = textDecoder.decode(resourceValue.getBytes(), null, resourcePath,
                            repository.getLwM2mModel(), LwM2mSingleResource.class);

                    // create dummy object enabler if object is not supported
                    int objId = resourcePath.getObjectId();
                    LwM2mObjectEnabler objectEnabler = client.getObjectTree().getObjectEnabler(objId);
                    if (objectEnabler == null) {
                        // get model
                        ObjectModel objectModel = repository.getObjectModel(objId);
                        if (objectModel == null) {
                            throw new IllegalStateException(
                                    String.format("Unable to enable Object %d : there no model for this object.%n",
                                            resourcePath.getObjectId()));
                        }
                        // create and add enabler
                        ObjectsInitializer objectsInitializer = new ObjectsInitializer(new StaticModel(objectModel));
                        objectsInitializer.setDummyInstancesForObject(objId);
                        objectEnabler = objectsInitializer.create(objId);
                        client.getObjectTree().addObjectEnabler(objectEnabler);
                    }

                    // try to write this resource
                    response = objectEnabler.write(LwM2mServer.SYSTEM,
                            new BootstrapWriteRequest(resourcePath, resource, ContentFormat.TEXT));
                } catch (RuntimeException e) {
                    // catch any error
                    throw new IllegalStateException(
                            String.format(" --factory-bootstrap failed : unable to write resource %s", resourcePath),
                            e);
                }
                // Raise error if bootstrap write request failed
                if (response == null || !response.isSuccess()) {
                    throw new IllegalStateException(
                            String.format("--factory-bootstrap failed : unable to write resource %s%s", resourcePath,
                                    response.getErrorMessage() == null ? "" : " : " + response.getErrorMessage()));
                }
            });
        }

        // Add some log about object tree life cycle.
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
