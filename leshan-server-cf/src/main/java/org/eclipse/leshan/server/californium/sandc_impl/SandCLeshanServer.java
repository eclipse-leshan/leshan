package org.eclipse.leshan.server.californium.sandc_impl;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.ObservationListener;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.Destroyable;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.Startable;
import org.eclipse.leshan.server.Stoppable;
import org.eclipse.leshan.server.californium.impl.CaliforniumLwM2mRequestSender;
import org.eclipse.leshan.server.californium.impl.LwM2mPskStore;
import org.eclipse.leshan.server.californium.impl.RegisterResource;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.observation.ObservationRegistry;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityRegistry;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * Created by Jyotsna.Bhonde on 10/23/2015.
 */
public class SandCLeshanServer implements LwM2mServer {
    private final CoapServer coapServer;

    private static final Logger LOG = LoggerFactory.getLogger(SandCLeshanServer.class);

    private final CaliforniumLwM2mRequestSender requestSender;

    private final ClientRegistry clientRegistry;

    private final ObservationRegistry observationRegistry;

    private final SecurityRegistry securityRegistry;

    private final LwM2mModelProvider modelProvider;

    private final CoapEndpoint nonSecureEndpoint;

    private final CoapEndpoint secureEndpoint;


    /**
     * Initialize a server which will bind to the specified address and port.
     *
     * @param localAddress the address to bind the CoAP server.
     * @param localAddressSecure the address to bind the CoAP server for DTLS connection.
     * @param clientRegistry the registered {@link Client} registry.
     * @param securityRegistry the {@link SecurityInfo} registry.
     * @param observationRegistry the {@link Observation} registry.
     * @param modelProvider provides the objects description for each client.
     */
    public SandCLeshanServer(InetSocketAddress localAddress, InetSocketAddress localAddressSecure,
                             final ClientRegistry clientRegistry, final SecurityRegistry securityRegistry,
                             final ObservationRegistry observationRegistry, final LwM2mModelProvider modelProvider) {
        Validate.notNull(localAddress, "IP address cannot be null");
        Validate.notNull(localAddressSecure, "Secure IP address cannot be null");
        Validate.notNull(clientRegistry, "clientRegistry cannot be null");
        Validate.notNull(securityRegistry, "securityRegistry cannot be null");
        Validate.notNull(observationRegistry, "observationRegistry cannot be null");
        Validate.notNull(modelProvider, "modelProvider cannot be null");

        // Init registries
        this.clientRegistry = clientRegistry;
        this.securityRegistry = securityRegistry;
        this.observationRegistry = observationRegistry;

        this.modelProvider = modelProvider;

        // Cancel observations on client unregistering
        this.clientRegistry.addListener(new ClientRegistryListener() {

            @Override
            public void updated(final Client clientUpdated) {
            }

            @Override
            public void unregistered(final Client client) {
                SandCLeshanServer.this.observationRegistry.cancelObservations(client);
            }

            @Override
            public void registered(final Client client) {
                // TODO observe the client when it is registered.
                System.out.println("Client has registered. you can start observing resource here now Yipppeee");
                //sendFirmwareUpdate(client);
                observeResource(client);
            }
        });

        // default endpoint
        coapServer = new CoapServer();
        nonSecureEndpoint = new CoapEndpoint(localAddress);
        coapServer.addEndpoint(nonSecureEndpoint);

        // secure endpoint
        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(localAddressSecure);
        builder.setPskStore(new LwM2mPskStore(this.securityRegistry, this.clientRegistry));
        PrivateKey privateKey = this.securityRegistry.getServerPrivateKey();
        PublicKey publicKey = this.securityRegistry.getServerPublicKey();
        if (privateKey != null && publicKey != null) {
            builder.setIdentity(privateKey, publicKey);
        }
        X509Certificate[] X509CertChain = this.securityRegistry.getServerX509CertChain();
        if (privateKey != null && X509CertChain != null && X509CertChain.length > 0) {
            builder.setIdentity(privateKey, X509CertChain, false);
        }
        Certificate[] trustedCertificates = securityRegistry.getTrustedCertificates();
        if (trustedCertificates != null && trustedCertificates.length > 0) {
            builder.setTrustStore(trustedCertificates);
        }

        secureEndpoint = new CoapEndpoint(new DTLSConnector(builder.build()), NetworkConfig.getStandard());
        coapServer.addEndpoint(secureEndpoint);

        // define /rd resource
        final RegisterResource rdResource = new RegisterResource(new RegistrationHandler(this.clientRegistry,
                this.securityRegistry));
        coapServer.add(rdResource);

        // create sender
        final Set<Endpoint> endpoints = new HashSet<>();
        endpoints.add(nonSecureEndpoint);
        endpoints.add(secureEndpoint);
        requestSender = new CaliforniumLwM2mRequestSender(endpoints, this.observationRegistry, modelProvider);
    }

    private void observeResource(final Client client){
        System.out.println("Starting to Observe resource.");
        LOG.debug("Starting to Observe resource.");
        // TODO JB  new ObserveRequest("/500/10/1") for Vibhors device.
        ObserveRequest request = new ObserveRequest("/500/0/1");
        //LwM2mResponse cResponse = this.send(client, request);
        ObserveResponse cResponse = this.send(client, request);
        System.out.println("cResponse : " + cResponse);
        LOG.debug("cResponse : " + cResponse);
        // There is another similar observation in eventServlet. Ideally there should only be one Observation.
        cResponse.getObservation().addListener(new ObservationListener() {
            @Override
            public void cancelled(Observation observation) {
                System.out.println("Observation Cancelled ....");
                LOG.debug("Observation Cancelled ....");
            }

            @Override
            public void newValue(Observation observation, LwM2mNode value) {
                writeToFile(observation, value);
            }
        });}


    @Override
    public void start() {

        // Start registries
        if (clientRegistry instanceof Startable) {
            ((Startable) clientRegistry).start();
        }
        if (securityRegistry instanceof Startable) {
            ((Startable) securityRegistry).start();
        }
        if (observationRegistry instanceof Startable) {
            ((Startable) observationRegistry).start();
        }

        // Start server
        coapServer.start();

        LOG.info("LW-M2M server started");
    }

    @Override
    public void stop() {
        // Stop server
        coapServer.stop();

        // Start registries
        if (clientRegistry instanceof Stoppable) {
            ((Stoppable) clientRegistry).stop();
        }
        if (securityRegistry instanceof Stoppable) {
            ((Stoppable) securityRegistry).stop();
        }
        if (observationRegistry instanceof Stoppable) {
            ((Stoppable) observationRegistry).stop();
        }

        LOG.info("LW-M2M server stopped");
    }

    public void destroy() {
        // Destroy server
        coapServer.destroy();

        // Destroy registries
        if (clientRegistry instanceof Destroyable) {
            ((Destroyable) clientRegistry).destroy();
        }
        if (securityRegistry instanceof Destroyable) {
            ((Destroyable) securityRegistry).destroy();
        }
        if (observationRegistry instanceof Destroyable) {
            ((Destroyable) observationRegistry).destroy();
        }

        LOG.info("LW-M2M server destroyed");
    }

    @Override
    public ClientRegistry getClientRegistry() {
        return this.clientRegistry;
    }

    @Override
    public ObservationRegistry getObservationRegistry() {
        return this.observationRegistry;
    }

    @Override
    public SecurityRegistry getSecurityRegistry() {
        return this.securityRegistry;
    }

    @Override
    public LwM2mModelProvider getModelProvider() {
        return this.modelProvider;
    }

    @Override
    public <T extends LwM2mResponse> T send(final Client destination, final DownlinkRequest<T> request) {
        return requestSender.send(destination, request, null);
    }

    @Override
    public <T extends LwM2mResponse> T send(final Client destination, final DownlinkRequest<T> request, long timeout) {
        return requestSender.send(destination, request, timeout);
    }

    @Override
    public <T extends LwM2mResponse> void send(final Client destination, final DownlinkRequest<T> request,
                                               final ResponseCallback<T> responseCallback, final ErrorCallback errorCallback) {
        requestSender.send(destination, request, responseCallback, errorCallback);
    }

    /**
     * @return the underlying {@link CoapServer}
     */
    public CoapServer getCoapServer() {
        return coapServer;
    }

    private void writeToFile(Observation observation, LwM2mNode value){
        ClassLoader classLoader = getClass().getClassLoader();
        Path path = Paths.get(classLoader.getResource("test/data.txt").getPath().replace("/C:", ""));
        try (BufferedWriter writer = Files.newBufferedWriter(path, CREATE, APPEND)){
            writer.write("\nObservation - RegistrationId : " + observation.getRegistrationId() + " - Path : " + observation.getPath());
            writer.write("  - Value : " + value);
        }catch(IOException ex){
            System.out.println("Exception thrown : " + ex);
        }
    }

    /**private void sendFirmwareUpdate(Client client){
     Path path = Paths.get("C:\\Leshan\\leshan\\leshan-standalone\\src\\main\\resources\\webapp\\firmwareUpdate\\Zigbee_Control_Release_01.01.12.A6.bin");
     byte[] fileInBytes = null;
     try {
     fileInBytes = Files.readAllBytes(path);
     } catch (IOException e) {
     e.printStackTrace();
     }

     System.out.println("Converted file length : " + fileInBytes.length);
     LwM2mResource resource = new LwM2mResource(0, Value.newBinaryValue(fileInBytes));
     WriteRequest writeRequest = new WriteRequest(5,0,0, resource, null, true);
     this.send(client, writeRequest);

     }**/

    public InetSocketAddress getSecureAddress() {
        return secureEndpoint.getAddress();
    }

}
