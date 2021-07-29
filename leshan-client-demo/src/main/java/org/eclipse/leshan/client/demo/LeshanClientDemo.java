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
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.List;

import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.scandium.DTLSConnector;
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
import org.eclipse.californium.scandium.dtls.SingleNodeConnectionIdGenerator;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.demo.cli.ExecutionExceptionHandler;
import org.eclipse.leshan.client.demo.cli.LeshanClientDemoCLI;
import org.eclipse.leshan.client.demo.cli.ShortErrorMessageHandler;
import org.eclipse.leshan.client.demo.cli.interactive.InteractiveCLI;
import org.eclipse.leshan.client.engine.DefaultRegistrationEngineFactory;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.resource.listener.ObjectsListenerAdapter;
import org.eclipse.leshan.core.californium.DefaultEndpointFactory;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.util.Hex;
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
    // /!\ This field is a COPY of org.eclipse.leshan.server.demo.LeshanServerDemo.modelPaths /!\
    // TODO create a leshan-demo project ?
    public final static String[] modelPaths = new String[] { "8.xml", "9.xml", "10.xml", "11.xml", "12.xml", "13.xml",
                            "14.xml", "15.xml", "16.xml", "19.xml", "20.xml", "22.xml", "500.xml", "501.xml", "502.xml",
                            "503.xml", "2048.xml", "2049.xml", "2050.xml", "2051.xml", "2052.xml", "2053.xml",
                            "2054.xml", "2055.xml", "2056.xml", "2057.xml", "3200.xml", "3201.xml", "3202.xml",
                            "3203.xml", "3300.xml", "3301.xml", "3302.xml", "3303.xml", "3304.xml", "3305.xml",
                            "3306.xml", "3308.xml", "3310.xml", "3311.xml", "3312.xml", "3313.xml", "3314.xml",
                            "3315.xml", "3316.xml", "3317.xml", "3318.xml", "3319.xml", "3320.xml", "3321.xml",
                            "3322.xml", "3323.xml", "3324.xml", "3325.xml", "3326.xml", "3327.xml", "3328.xml",
                            "3329.xml", "3330.xml", "3331.xml", "3332.xml", "3333.xml", "3334.xml", "3335.xml",
                            "3336.xml", "3337.xml", "3338.xml", "3339.xml", "3340.xml", "3341.xml", "3342.xml",
                            "3343.xml", "3344.xml", "3345.xml", "3346.xml", "3347.xml", "3348.xml", "3349.xml",
                            "3350.xml", "3351.xml", "3352.xml", "3353.xml", "3354.xml", "3355.xml", "3356.xml",
                            "3357.xml", "3358.xml", "3359.xml", "3360.xml", "3361.xml", "3362.xml", "3363.xml",
                            "3364.xml", "3365.xml", "3366.xml", "3367.xml", "3368.xml", "3369.xml", "3370.xml",
                            "3371.xml", "3372.xml", "3373.xml", "3374.xml", "3375.xml", "3376.xml", "3377.xml",
                            "3378.xml", "3379.xml", "3380.xml", "3381.xml", "3382.xml", "3383.xml", "3384.xml",
                            "3385.xml", "3386.xml", "3387.xml", "3388.xml", "3389.xml", "3390.xml", "3391.xml",
                            "3392.xml", "3393.xml", "3394.xml", "3395.xml", "3396.xml", "3397.xml", "3398.xml",
                            "3399.xml", "3400.xml", "3401.xml", "3402.xml", "3403.xml", "3404.xml", "3405.xml",
                            "3406.xml", "3407.xml", "3408.xml", "3410.xml", "3411.xml", "3412.xml", "3413.xml",
                            "3414.xml", "3415.xml", "3416.xml", "3417.xml", "3418.xml", "3419.xml", "3420.xml",
                            "3421.xml", "3423.xml", "3424.xml", "3425.xml", "3426.xml", "3427.xml", "3428.xml",
                            "3429.xml", "3430.xml", "3431.xml", "3432.xml", "3433.xml", "3434.xml", "3435.xml",
                            "3436.xml", "3437.xml", "3438.xml", "3439.xml", "10241.xml", "10242.xml", "10243.xml",
                            "10244.xml", "10245.xml", "10246.xml", "10247.xml", "10248.xml", "10249.xml", "10250.xml",
                            "10251.xml", "10252.xml", "10253.xml", "10254.xml", "10255.xml", "10256.xml", "10257.xml",
                            "10258.xml", "10259.xml", "10260.xml", "10262.xml", "10263.xml", "10264.xml", "10265.xml",
                            "10266.xml", "10267.xml", "10268.xml", "10269.xml", "10270.xml", "10271.xml", "10272.xml",
                            "10273.xml", "10274.xml", "10275.xml", "10276.xml", "10277.xml", "10278.xml", "10279.xml",
                            "10280.xml", "10281.xml", "10282.xml", "10283.xml", "10284.xml", "10286.xml", "10290.xml",
                            "10291.xml", "10292.xml", "10299.xml", "10300.xml", "10308.xml", "10309.xml", "10311.xml",
                            "10313.xml", "10314.xml", "10315.xml", "10316.xml", "10318.xml", "10319.xml", "10320.xml",
                            "10322.xml", "10323.xml", "10324.xml", "10326.xml", "10327.xml", "10328.xml", "10329.xml",
                            "10330.xml", "10331.xml", "10332.xml", "10333.xml", "10334.xml", "10335.xml", "10336.xml",
                            "10337.xml", "10338.xml", "10339.xml", "10340.xml", "10341.xml", "10342.xml", "10343.xml",
                            "10344.xml", "10345.xml", "10346.xml", "10347.xml", "10348.xml", "10349.xml", "10350.xml",
                            "10351.xml", "10352.xml", "10353.xml", "10354.xml", "10355.xml", "10356.xml", "10357.xml",
                            "10358.xml", "10359.xml", "10360.xml", "10361.xml", "10362.xml", "10363.xml", "10364.xml",
                            "10365.xml", "10366.xml", "10368.xml", "10369.xml", "10371.xml", "18830.xml",
                            "18831.xml", };

    private static final int OBJECT_ID_TEMPERATURE_SENSOR = 3303;

    public static void main(String[] args) {

        // Parse command line
        LeshanClientDemoCLI cli = new LeshanClientDemoCLI();
        CommandLine command = new CommandLine(cli).setParameterExceptionHandler(new ShortErrorMessageHandler())
                .setExecutionExceptionHandler(new ExecutionExceptionHandler());
        // Handle exit code error
        int exitCode = command.execute(args);
        if (exitCode != 0)
            System.exit(exitCode);
        // Handle help or version command
        if (command.isUsageHelpRequested() || command.isVersionHelpRequested())
            System.exit(0);

        try {
            // Create Client
            LwM2mModel model = createModel(cli);
            final LeshanClient client = createClient(cli, model);

            // Print commands help
            InteractiveCLI console = new InteractiveCLI(client, model);
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

    private static LwM2mModel createModel(LeshanClientDemoCLI cli) throws Exception {

        List<ObjectModel> models = ObjectLoader.loadDefault();
        models.addAll(ObjectLoader.loadDdfResources("/models", modelPaths));
        if (cli.main.modelsFolder != null) {
            models.addAll(ObjectLoader.loadObjectsFromDir(cli.main.modelsFolder, true));
        }

        return new StaticModel(models);
    }

    public static LeshanClient createClient(LeshanClientDemoCLI cli, LwM2mModel model) throws Exception {
        // create Leshan client from command line option
        final MyLocation locationInstance = new MyLocation(cli.location.position.latitude,
                cli.location.position.longitude, cli.location.scaleFactor);

        // Initialize object list
        final ObjectsInitializer initializer = new ObjectsInitializer(model);
        if (cli.main.bootstrap) {
            if (cli.identity.isPSK()) {
                initializer.setInstancesForObject(SECURITY, pskBootstrap("coaps://" + cli.main.url,
                        cli.identity.getPsk().identity.getBytes(), cli.identity.getPsk().sharekey.getBytes()));
                initializer.setClassForObject(SERVER, Server.class);
            } else if (cli.identity.isRPK()) {
                initializer.setInstancesForObject(SECURITY,
                        rpkBootstrap("coaps://" + cli.main.url, cli.identity.getRPK().cpubk.getEncoded(),
                                cli.identity.getRPK().cprik.getEncoded(), cli.identity.getRPK().spubk.getEncoded()));
                initializer.setClassForObject(SERVER, Server.class);
            } else if (cli.identity.isx509()) {
                initializer.setInstancesForObject(SECURITY,
                        x509Bootstrap("coaps://" + cli.main.url, cli.identity.getX509().ccert.getEncoded(),
                                cli.identity.getX509().cprik.getEncoded(), cli.identity.getX509().scert.getEncoded(),
                                cli.identity.getX509().certUsage.code));
                initializer.setClassForObject(SERVER, Server.class);
            } else {
                initializer.setInstancesForObject(SECURITY, noSecBootstap("coap://" + cli.main.url));
                initializer.setClassForObject(SERVER, Server.class);
            }
        } else {
            if (cli.identity.isPSK()) {
                initializer.setInstancesForObject(SECURITY, psk("coaps://" + cli.main.url, 123,
                        cli.identity.getPsk().identity.getBytes(), cli.identity.getPsk().sharekey.getBytes()));
                initializer.setInstancesForObject(SERVER, new Server(123, cli.main.lifetimeInSec));
            } else if (cli.identity.isRPK()) {
                initializer.setInstancesForObject(SECURITY,
                        rpk("coaps://" + cli.main.url, 123, cli.identity.getRPK().cpubk.getEncoded(),
                                cli.identity.getRPK().cprik.getEncoded(), cli.identity.getRPK().spubk.getEncoded()));
                initializer.setInstancesForObject(SERVER, new Server(123, cli.main.lifetimeInSec));
            } else if (cli.identity.isx509()) {
                initializer.setInstancesForObject(SECURITY,
                        x509("coaps://" + cli.main.url, 123, cli.identity.getX509().ccert.getEncoded(),
                                cli.identity.getX509().cprik.getEncoded(), cli.identity.getX509().scert.getEncoded(),
                                cli.identity.getX509().certUsage.code));
                initializer.setInstancesForObject(SERVER, new Server(123, cli.main.lifetimeInSec));
            } else {
                initializer.setInstancesForObject(SECURITY, noSec("coap://" + cli.main.url, 123));
                initializer.setInstancesForObject(SERVER, new Server(123, cli.main.lifetimeInSec));
            }
        }
        initializer.setInstancesForObject(DEVICE, new MyDevice());
        initializer.setInstancesForObject(LOCATION, locationInstance);
        initializer.setInstancesForObject(OBJECT_ID_TEMPERATURE_SENSOR, new RandomTemperatureSensor());
        List<LwM2mObjectEnabler> enablers = initializer.createAll();

        // Create CoAP Config
        NetworkConfig coapConfig;
        File configFile = new File(NetworkConfig.DEFAULT_FILE_NAME);
        if (configFile.isFile()) {
            coapConfig = new NetworkConfig();
            coapConfig.load(configFile);
        } else {
            coapConfig = LeshanClientBuilder.createDefaultNetworkConfig();
            coapConfig.store(configFile);
        }

        // Create DTLS Config
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
        dtlsConfig.setRecommendedCipherSuitesOnly(!cli.dtls.supportDeprecatedCiphers);
        if (cli.dtls.ciphers != null) {
            dtlsConfig.setSupportedCipherSuites(cli.dtls.ciphers);
        }
        if (cli.dtls.cid != null) {
            dtlsConfig.setConnectionIdGenerator(new SingleNodeConnectionIdGenerator(cli.dtls.cid));
        }

        // Configure Registration Engine
        DefaultRegistrationEngineFactory engineFactory = new DefaultRegistrationEngineFactory();
        if (cli.main.comPeriodInSec != null)
            engineFactory.setCommunicationPeriod(cli.main.comPeriodInSec * 1000);
        engineFactory.setReconnectOnUpdate(cli.dtls.reconnectOnUpdate);
        engineFactory.setResumeOnConnect(!cli.dtls.forceFullhandshake);

        // configure EndpointFactory
        DefaultEndpointFactory endpointFactory = new DefaultEndpointFactory("LWM2M CLIENT", true) {
            @Override
            protected Connector createSecuredConnector(DtlsConnectorConfig dtlsConfig) {

                return new DTLSConnector(dtlsConfig) {
                    @Override
                    protected void onInitializeHandshaker(Handshaker handshaker) {
                        handshaker.addSessionListener(new SessionAdapter() {

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
                                    if (sessionIdentifier != null && sessionIdentifier
                                            .equals(handshaker.getSession().getSessionIdentifier())) {
                                        LOG.info("DTLS abbreviated Handshake initiated by client : SUCCEED");
                                    } else {
                                        LOG.info(
                                                "DTLS abbreviated turns into Full Handshake initiated by client : SUCCEED");
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
                    }
                };
            }
        };

        // Create client
        LeshanClientBuilder builder = new LeshanClientBuilder(cli.main.endpoint);
        builder.setLocalAddress(cli.main.localAddress, cli.main.localPort);
        builder.setObjects(enablers);
        builder.setCoapConfig(coapConfig);
        if (cli.identity.isx509())
            builder.setTrustStore(cli.identity.getX509().trustStore);
        builder.setDtlsConfig(dtlsConfig);
        builder.setRegistrationEngineFactory(engineFactory);
        builder.setEndpointFactory(endpointFactory);
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
                LOG.info("Object {} disabled.", object.getId());
            }

            @Override
            public void objectAdded(LwM2mObjectEnabler object) {
                LOG.info("Object {} enabled.", object.getId());
            }
        });

        // Display client public key to easily add it in demo servers.
        if (cli.identity.isRPK()) {
            PublicKey rawPublicKey = cli.identity.getRPK().cpubk;
            if (rawPublicKey instanceof ECPublicKey) {
                ECPublicKey ecPublicKey = (ECPublicKey) rawPublicKey;
                // Get x coordinate
                byte[] x = ecPublicKey.getW().getAffineX().toByteArray();
                if (x[0] == 0)
                    x = Arrays.copyOfRange(x, 1, x.length);

                // Get Y coordinate
                byte[] y = ecPublicKey.getW().getAffineY().toByteArray();
                if (y[0] == 0)
                    y = Arrays.copyOfRange(y, 1, y.length);

                // Get Curves params
                String params = ecPublicKey.getParams().toString();

                LOG.info(
                        "Client uses RPK : \n Elliptic Curve parameters  : {} \n Public x coord : {} \n Public y coord : {} \n Public Key (Hex): {} \n Private Key (Hex): {}",
                        params, Hex.encodeHexString(x), Hex.encodeHexString(y),
                        Hex.encodeHexString(rawPublicKey.getEncoded()),
                        Hex.encodeHexString(cli.identity.getRPK().cprik.getEncoded()));

            } else {
                throw new IllegalStateException("Unsupported Public Key Format (only ECPublicKey supported).");
            }
        }
        // Display X509 credentials to easily at it in demo servers.
        if (cli.identity.isx509()) {
            LOG.info("Client uses X509 : \n X509 Certificate (Hex): {} \n Private Key (Hex): {}",
                    Hex.encodeHexString(cli.identity.getX509().ccert.getEncoded()),
                    Hex.encodeHexString(cli.identity.getX509().cprik.getEncoded()));
        }

        return client;
    }
}
