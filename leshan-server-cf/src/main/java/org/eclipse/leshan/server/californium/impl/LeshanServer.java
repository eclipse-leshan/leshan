/*******************************************************************************
 * Copyright (c) 2013-2016 Sierra Wireless and others.
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
 *     Bosch Software Innovations - add support for providing Endpoints
 *******************************************************************************/
package org.eclipse.leshan.server.californium.impl;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
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
import org.eclipse.leshan.server.client.Registration;
import org.eclipse.leshan.server.client.RegistrationListener;
import org.eclipse.leshan.server.client.RegistrationService;
import org.eclipse.leshan.server.client.RegistrationUpdate;
import org.eclipse.leshan.server.impl.RegistrationServiceImpl;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.observation.ObservationService;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.eclipse.leshan.server.response.ResponseListener;
import org.eclipse.leshan.server.security.Authorizer;
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

    private static final Logger LOG = LoggerFactory.getLogger(LeshanServer.class);

    private final CaliforniumRegistrationStore registrationStore;
    private final RegistrationServiceImpl registrationService;
    private final ObservationServiceImpl observationService;
    private final SecurityStore securityStore;
    private final Authorizer authorizer;
    private final LwM2mModelProvider modelProvider;
    private final LwM2mNodeEncoder encoder;
    private final LwM2mNodeDecoder decoder;

    private Endpoint nonSecureEndpoint;
    private Endpoint secureEndpoint;
    private LwM2mRequestSender requestSender;
    private CoapServer coapServer;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private X509Certificate[] x509CertChain;
    private Certificate[] trustedCertificates;

    private LeshanServer(final CaliforniumRegistrationStore registrationStore, final SecurityStore securityStore,
            final Authorizer authorizer, final LwM2mModelProvider modelProvider, final LwM2mNodeEncoder encoder,
            final LwM2mNodeDecoder decoder, final PublicKey publicKey, final PrivateKey privateKey,
            final X509Certificate[] x509CertChain, final Certificate[] trustedCertificates) {
        Validate.notNull(registrationStore, "registration store cannot be null");
        Validate.notNull(securityStore, "securityStore cannot be null");
        Validate.notNull(authorizer, "authorizer cannot be null");
        Validate.notNull(modelProvider, "modelProvider cannot be null");
        Validate.notNull(encoder, "encoder cannot be null");
        Validate.notNull(decoder, "decoder cannot be null");

        // Init services and stores
        this.registrationStore = registrationStore;
        this.registrationService = new RegistrationServiceImpl(registrationStore);
        this.securityStore = securityStore;
        this.authorizer = authorizer;
        this.observationService = new ObservationServiceImpl(registrationStore, modelProvider, decoder);
        this.modelProvider = modelProvider;
        this.encoder = encoder;
        this.decoder = decoder;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.x509CertChain = x509CertChain;
        this.trustedCertificates = trustedCertificates;
    }

    /**
     * Creates a server for CoAP endpoints.
     * 
     * @param endpoints The endpoints to use for communicating with clients.
     * @param registrationStore The registry for keeping track of observed client resources.
     * @param securityStore The registry to use for looking up security parameters of clients.
     * @param authorizer define which devices is allow to register on this server.
     * @param modelProvider The registry to use for looking up LWM2M object definitions.
     * @param encoder The object to use for encoding message payload.
     * @param decoder The object to use for decoding message payload.
     * @throws IllegalArgumentException if any of the parameters is {@code null}.
     */
    public LeshanServer(final Set<Endpoint> endpoints, final CaliforniumRegistrationStore registrationStore
            , final SecurityStore securityStore, final Authorizer authorizer, final LwM2mModelProvider modelProvider,
            final LwM2mNodeEncoder encoder, final LwM2mNodeDecoder decoder, final PublicKey publicKey,
            final PrivateKey privateKey, final X509Certificate[] x509CertChain, final Certificate[] trustedCertificates) {

        this(registrationStore, securityStore, authorizer, modelProvider, encoder, decoder, publicKey,
                privateKey, x509CertChain, trustedCertificates);
        createServer(endpoints);
    }

    /**
     * Creates a server which will bind to the specified endpoint addresses.
     *
     * @param localAddress the address to bind the CoAP server.
     * @param localSecureAddress the address to bind the CoAP server for DTLS connection.
     * @param registrationStore The registry for keeping track of observed client resources.
     * @param securityStore The registry to use for looking up security parameters of clients.
     * @param authorizer define which devices is allow to register on this server.
     * @param modelProvider The registry to use for looking up LWM2M object definitions.
     * @param encoder The object to use for encoding message payload.
     * @param decoder The object to use for decoding message payload.
     * @throws IllegalArgumentException if any of the parameters is {@code null}.
     */
    public LeshanServer(InetSocketAddress localAddress, InetSocketAddress localSecureAddress,
            final CaliforniumRegistrationStore registrationStore, final SecurityStore securityStore,
            final Authorizer authorizer, final LwM2mModelProvider modelProvider, final LwM2mNodeEncoder encoder,
            final LwM2mNodeDecoder decoder, final PublicKey publicKey, final PrivateKey privateKey,
            final X509Certificate[] x509CertChain, final Certificate[] trustedCertificates) {

        this(registrationStore, securityStore, authorizer, modelProvider, encoder, decoder, publicKey,
                privateKey, x509CertChain, trustedCertificates);
        Validate.notNull(localAddress, "IP address cannot be null");
        Validate.notNull(localSecureAddress, "Secure IP address cannot be null");

        Set<Endpoint> endpointsToUse = new HashSet<>();

        nonSecureEndpoint = new CoapEndpoint(localAddress, NetworkConfig.getStandard(),
                this.observationService.getObservationStore());
        nonSecureEndpoint.addNotificationListener(observationService);
        observationService.setNonSecureEndpoint(nonSecureEndpoint);
        endpointsToUse.add(nonSecureEndpoint);

        secureEndpoint = createSecureEndpoint(localSecureAddress, securityStore, observationService);
        secureEndpoint.addNotificationListener(observationService);
        observationService.setSecureEndpoint(secureEndpoint);
        endpointsToUse.add(secureEndpoint);

        createServer(endpointsToUse);
    }

    private Endpoint createSecureEndpoint(final InetSocketAddress localSecureAddress, final SecurityStore securityStore,
            final ObservationServiceImpl observationService) {

        // secure endpoint
        Builder builder = new DtlsConnectorConfig.Builder(localSecureAddress);
        builder.setPskStore(new LwM2mPskStore(securityStore));

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

        return new CoapEndpoint(new DTLSConnector(builder.build()), NetworkConfig.getStandard(),
                observationService.getObservationStore(), null);
    }

    private void createServer(final Set<Endpoint> endpoints) {

        // Cancel observations on client unregistering
        this.registrationService.addListener(new RegistrationListener() {

            @Override
            public void updated(final RegistrationUpdate update, final Registration updatedRegistration) {
            }

            @Override
            public void unregistered(final Registration registration) {
                LeshanServer.this.observationService.cancelObservations(registration);
                requestSender.cancelPendingRequests(registration);
            }

            @Override
            public void registered(final Registration registration) {
            }
        });

        // default endpoint
        coapServer = new CoapServer() {
            @Override
            protected Resource createRoot() {
                return new RootResource();
            }
        };

        // define /rd resource
        final RegisterResource rdResource = new RegisterResource(
                new RegistrationHandler(this.registrationService, authorizer));
        coapServer.add(rdResource);

        for (Endpoint ep : endpoints) {
            if (secureEndpoint == null && ep.getUri().getScheme().startsWith(CoAP.COAP_SECURE_URI_SCHEME)) {
                // capture first "secure" endpoint
                secureEndpoint = ep;
            } else if (nonSecureEndpoint == null && ep.getUri().getScheme().startsWith(CoAP.COAP_URI_SCHEME)) {
                // capture first "non secure" endpoint
                nonSecureEndpoint = ep;
            }
            coapServer.addEndpoint(ep);
        }

        requestSender = new CaliforniumLwM2mRequestSender(Collections.unmodifiableSet(endpoints),
                this.observationService, modelProvider, encoder, decoder);
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

        if (LOG.isInfoEnabled()) {
            StringBuilder b = new StringBuilder("LWM2M server started at endpoints");
            for (Endpoint ep : coapServer.getEndpoints()) {
                b.append(" ").append(ep.getUri().toString());
            }
            LOG.info(b.toString());
        }
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

    public void destroy() {
        // Destroy server
        coapServer.destroy();

        // Destroy stores
        if (registrationStore instanceof Destroyable) {
            ((Destroyable) registrationStore).destroy();
        }
        if (securityStore instanceof Destroyable) {
            ((Destroyable) securityStore).destroy();
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
    public <T extends LwM2mResponse> T send(final Registration destination, final DownlinkRequest<T> request)
            throws InterruptedException {
        return requestSender.send(destination, request, null);
    }

    @Override
    public <T extends LwM2mResponse> T send(final Registration destination, final DownlinkRequest<T> request, long timeout)
            throws InterruptedException {
        return requestSender.send(destination, request, timeout);
    }

    @Override
    public <T extends LwM2mResponse> void send(final Registration destination, final DownlinkRequest<T> request,
            final ResponseCallback<T> responseCallback, final ErrorCallback errorCallback) {
        requestSender.send(destination, request, responseCallback, errorCallback);
    }

    @Override
    public <T extends LwM2mResponse> void send(Registration destination, String requestTicket, DownlinkRequest<T> request) {
        requestSender.send(destination, requestTicket, request);
    }

    @Override
    public void addResponseListener(ResponseListener listener) {
        requestSender.addResponseListener(listener);
    }

    @Override
    public void removeResponseListener(ResponseListener listener) {
        requestSender.removeResponseListener(listener);
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
        return secureEndpoint.getAddress();
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
