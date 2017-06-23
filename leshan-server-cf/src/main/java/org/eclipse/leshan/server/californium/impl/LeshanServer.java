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
 *******************************************************************************/
package org.eclipse.leshan.server.californium.impl;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.InMemoryMessageExchangeStore;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.config.NetworkConfig.Keys;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.UDPConnector;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.Destroyable;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.Startable;
import org.eclipse.leshan.server.Stoppable;
import org.eclipse.leshan.server.californium.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.impl.RegistrationServiceImpl;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.observation.ObservationService;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationService;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.eclipse.leshan.server.security.Authorizer;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityStore;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Lightweight M2M server.
 * <p>
 * This implementation starts a Californium {@link CoapServer} with a non-secure and secure endpoint. This CoAP server
 * defines a <i>/rd</i> resource as described in the CoRE RD specification.
 * </p>
 * <p>
 * This class is the entry point to send synchronous and asynchronous requests to registered clients.
 * </p>
 * <p>
 * The {@link LeshanServerBuilder} should be the preferred way to build an instance of {@link LeshanServer}.
 * </p>
 */
public class LeshanServer implements LwM2mServer {

    private final CoapServer coapServer;

    private static final Logger LOG = LoggerFactory.getLogger(LeshanServer.class);

    private final LwM2mRequestSender requestSender;

    private final RegistrationServiceImpl registrationService;

    private final ObservationServiceImpl observationService;

    private final SecurityStore securityStore;

    private final LwM2mModelProvider modelProvider;

    private final CoapEndpoint nonSecureEndpoint;

    private final CoapEndpoint secureEndpoint;

    private final CaliforniumRegistrationStore registrationStore;

    /**
     * Initialize a server which will bind to the specified address and port.
     *
     * @param localAddress the address to bind the CoAP server.
     * @param localSecureAddress the address to bind the CoAP server for DTLS connection.
     * @param registrationStore the {@link Registration} store.
     * @param securityStore the {@link SecurityInfo} store.
     * @param authorizer define which devices is allow to register on this server.
     * @param modelProvider provides the objects description for each client.
     * @param decoder decoder used to decode response payload.
     * @param encoder encode used to encode request payload.
     * @param publicKey the server public key used for RPK DTLS authentication.
     * @param privateKey the server private key used to RPK or X509 DTLS authentication.
     * @param x509CertChain the server X509 certificate (will be used for RPK too, in this case no need to set public
     *        key).
     * @param trustedCertificates the trusted certificates used to authenticate client certificates.
     * @param networkConfig the CoAP {@link NetworkConfig}.
     */
    public LeshanServer(InetSocketAddress localAddress, InetSocketAddress localSecureAddress,
            CaliforniumRegistrationStore registrationStore, SecurityStore securityStore, Authorizer authorizer,
            LwM2mModelProvider modelProvider, LwM2mNodeEncoder encoder, LwM2mNodeDecoder decoder, PublicKey publicKey,
            PrivateKey privateKey, X509Certificate[] x509CertChain, Certificate[] trustedCertificates,
            NetworkConfig networkConfig) {
        Validate.notNull(localAddress, "IP address cannot be null");
        Validate.notNull(localSecureAddress, "Secure IP address cannot be null");
        Validate.notNull(registrationStore, "registration store cannot be null");
        Validate.notNull(authorizer, "authorizer cannot be null");
        Validate.notNull(modelProvider, "modelProvider cannot be null");
        Validate.notNull(encoder, "encoder cannot be null");
        Validate.notNull(decoder, "decoder cannot be null");
        Validate.notNull(networkConfig, "networkConfig cannot be null");

        // Init services and stores
        this.registrationStore = registrationStore;
        this.registrationService = new RegistrationServiceImpl(registrationStore);
        this.securityStore = securityStore;
        this.observationService = new ObservationServiceImpl(registrationStore, modelProvider, decoder);
        this.modelProvider = modelProvider;

        // Cancel observations on client unregistering
        this.registrationService.addListener(new RegistrationListener() {

            @Override
            public void updated(RegistrationUpdate update, Registration updatedRegistration,
                    Registration previousRegistration) {
            }

            @Override
            public void unregistered(Registration registration, Collection<Observation> observations, boolean expired) {
                requestSender.cancelPendingRequests(registration);
            }

            @Override
            public void registered(Registration registration) {
            }
        });

        // define a set of endpoints
        Set<Endpoint> endpoints = new HashSet<>();

        // default endpoint
        coapServer = new CoapServer(networkConfig) {
            @Override
            protected Resource createRoot() {
                return new RootResource();
            }
        };

        // exchange store for non-secure endpoint
        InMemoryMessageExchangeStore nsExchangeStore = new InMemoryMessageExchangeStore(networkConfig);
        nsExchangeStore.setMessageIdProvider(new SimpleMessageIdProvider(networkConfig));

        nonSecureEndpoint = new CoapEndpoint(createUDPConnector(localAddress, networkConfig), networkConfig,
                this.observationService.getObservationStore(), nsExchangeStore);
        nonSecureEndpoint.addNotificationListener(observationService);
        observationService.setNonSecureEndpoint(nonSecureEndpoint);
        coapServer.addEndpoint(nonSecureEndpoint);
        endpoints.add(nonSecureEndpoint);

        // secure endpoint
        if (securityStore != null) {
            Builder builder = new DtlsConnectorConfig.Builder().setAddress(localSecureAddress);
            builder.setPskStore(new LwM2mPskStore(this.securityStore, this.registrationService.getStore()));

            // synchronize network configuration and DTLS configuration
            builder.setMaxConnections(networkConfig.getInt(Keys.MAX_ACTIVE_PEERS));
            builder.setStaleConnectionThreshold(networkConfig.getLong(Keys.MAX_PEER_INACTIVITY_PERIOD));

            // if in raw key mode and not in X.509 set the raw keys
            if (x509CertChain == null && privateKey != null && publicKey != null) {
                builder.setIdentity(privateKey, publicKey);
            }
            // if in X.509 mode set the private key, certificate chain, public key is extracted from the certificate
            if (privateKey != null && x509CertChain != null && x509CertChain.length > 0) {
                builder.setIdentity(privateKey, x509CertChain, false);
            }

            if (trustedCertificates != null && trustedCertificates.length > 0) {
                builder.setTrustStore(trustedCertificates);
            }

            // exchange store for secure endpoint
            InMemoryMessageExchangeStore sExchangeStore = new InMemoryMessageExchangeStore(networkConfig);
            sExchangeStore.setMessageIdProvider(new SimpleMessageIdProvider(networkConfig));

            secureEndpoint = new CoapEndpoint(new DTLSConnector(builder.build()), networkConfig,
                    this.observationService.getObservationStore(), sExchangeStore);
            secureEndpoint.addNotificationListener(observationService);
            observationService.setSecureEndpoint(secureEndpoint);
            coapServer.addEndpoint(secureEndpoint);
            endpoints.add(secureEndpoint);
        } else {
            secureEndpoint = null;
        }

        // define /rd resource
        RegisterResource rdResource = new RegisterResource(
                new RegistrationHandler(this.registrationService, authorizer));
        coapServer.add(rdResource);

        // create sender
        requestSender = new CaliforniumLwM2mRequestSender(endpoints, this.observationService, modelProvider, encoder,
                decoder);
    }

    /**
     * Copied from {@link CoapEndpoint#createUDPConnector}
     */
    private static Connector createUDPConnector(final InetSocketAddress address, final NetworkConfig config) {
        UDPConnector c = new UDPConnector(address);

        c.setReceiverThreadCount(config.getInt(NetworkConfig.Keys.NETWORK_STAGE_RECEIVER_THREAD_COUNT));
        c.setSenderThreadCount(config.getInt(NetworkConfig.Keys.NETWORK_STAGE_SENDER_THREAD_COUNT));

        c.setReceiveBufferSize(config.getInt(NetworkConfig.Keys.UDP_CONNECTOR_RECEIVE_BUFFER));
        c.setSendBufferSize(config.getInt(NetworkConfig.Keys.UDP_CONNECTOR_SEND_BUFFER));
        c.setReceiverPacketSize(config.getInt(NetworkConfig.Keys.UDP_CONNECTOR_DATAGRAM_SIZE));

        return c;
    }

    @Override
    public void start() {

        // Start stores
        if (registrationStore instanceof Startable) {
            ((Startable) registrationStore).start();
        }
        if (securityStore instanceof Startable) {
            ((Startable) securityStore).start();
        }

        // Start server
        coapServer.start();

        LOG.info("LWM2M server started at coap://{}, coaps://{}.", getNonSecureAddress(), getSecureAddress());
    }

    @Override
    public void stop() {
        // Stop server
        coapServer.stop();

        // Stop stores
        if (registrationStore instanceof Stoppable) {
            ((Stoppable) registrationStore).stop();
        }
        if (securityStore instanceof Stoppable) {
            ((Stoppable) securityStore).stop();
        }

        LOG.info("LWM2M server stopped.");
    }

    @Override
    public void destroy() {
        // Destroy server
        coapServer.destroy();

        // Destroy stores
        if (registrationStore instanceof Destroyable) {
            ((Destroyable) registrationStore).destroy();
        } else if (registrationStore instanceof Stoppable) {
            ((Stoppable) registrationStore).stop();
        }

        if (securityStore instanceof Destroyable) {
            ((Destroyable) securityStore).destroy();
        } else if (securityStore instanceof Stoppable) {
            ((Stoppable) securityStore).stop();
        }

        LOG.info("LWM2M server destroyed.");
    }

    @Override
    public RegistrationService getRegistrationService() {
        return this.registrationService;
    }

    @Override
    public ObservationService getObservationService() {
        return this.observationService;
    }

    @Override
    public SecurityStore getSecurityStore() {
        return this.securityStore;
    }

    @Override
    public LwM2mModelProvider getModelProvider() {
        return this.modelProvider;
    }

    @Override
    public <T extends LwM2mResponse> T send(Registration destination, DownlinkRequest<T> request)
            throws InterruptedException {
        return requestSender.send(destination, request, null);
    }

    @Override
    public <T extends LwM2mResponse> T send(Registration destination, DownlinkRequest<T> request, long timeout)
            throws InterruptedException {
        return requestSender.send(destination, request, timeout);
    }

    @Override
    public <T extends LwM2mResponse> void send(Registration destination, DownlinkRequest<T> request,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback) {
        requestSender.send(destination, request, responseCallback, errorCallback);
    }

    /**
     * @return the underlying {@link CoapServer}
     */
    public CoapServer getCoapServer() {
        return coapServer;
    }

    public InetSocketAddress getNonSecureAddress() {
        return nonSecureEndpoint.getAddress();
    }

    public InetSocketAddress getSecureAddress() {
        if (secureEndpoint != null) {
            return secureEndpoint.getAddress();
        } else {
            return null;
        }
    }

    /**
     * The Leshan Root Resource.
     */
    private class RootResource extends CoapResource {

        public RootResource() {
            super("");
        }

        @Override
        public void handleGET(CoapExchange exchange) {
            exchange.respond(ResponseCode.NOT_FOUND);
        }

        @Override
        public List<Endpoint> getEndpoints() {
            return coapServer.getEndpoints();
        }
    }
}
