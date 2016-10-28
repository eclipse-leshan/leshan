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
import org.eclipse.leshan.server.californium.CaliforniumObservationRegistry;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import org.eclipse.leshan.server.client.ClientUpdate;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.observation.ObservationRegistry;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.eclipse.leshan.server.request.LwM2mRequestSender;
import org.eclipse.leshan.server.security.SecurityRegistry;
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

    private final ClientRegistry clientRegistry;

    private final CaliforniumObservationRegistry observationRegistry;

    private final SecurityRegistry securityRegistry;

    private final LwM2mModelProvider modelProvider;

    private final LwM2mNodeEncoder encoder;

    private final LwM2mNodeDecoder decoder;

    private Endpoint nonSecureEndpoint;

    private Endpoint secureEndpoint;

    private LwM2mRequestSender requestSender;

    private CoapServer coapServer;

    private LeshanServer(final ClientRegistry clientRegistry, final SecurityRegistry securityRegistry,
            final CaliforniumObservationRegistry observationRegistry, final LwM2mModelProvider modelProvider,
            final LwM2mNodeEncoder encoder, final LwM2mNodeDecoder decoder) {

        Validate.notNull(clientRegistry, "clientRegistry cannot be null");
        Validate.notNull(securityRegistry, "securityRegistry cannot be null");
        Validate.notNull(observationRegistry, "observationRegistry cannot be null");
        Validate.notNull(modelProvider, "modelProvider cannot be null");
        Validate.notNull(encoder, "encoder cannot be null");
        Validate.notNull(decoder, "decoder cannot be null");

        // Init registries
        this.clientRegistry = clientRegistry;
        this.securityRegistry = securityRegistry;
        this.observationRegistry = observationRegistry;
        this.modelProvider = modelProvider;
        this.encoder = encoder;
        this.decoder = decoder;
    }

    /**
     * Creates a server for CoAP endpoints.
     * 
     * @param endpoints The endpoints to use for communicating with clients.
     * @param clientRegistry The registry for keeping track of LWM2M client lifecycle.
     * @param securityRegistry The registry to use for looking up security parameters of clients.
     * @param observationRegistry The registry for keeping track of observed client resources.
     * @param modelProvider The registry to use for looking up LWM2M object definitions.
     * @param encoder The object to use for encoding message payload.
     * @param decoder The object to use for decoding message payload.
     * @throws IllegalArgumentException if any of the parameters is {@code null}.
     */
    public LeshanServer(final Set<Endpoint> endpoints, final ClientRegistry clientRegistry,
            final SecurityRegistry securityRegistry, final CaliforniumObservationRegistry observationRegistry,
            final LwM2mModelProvider modelProvider, final LwM2mNodeEncoder encoder, final LwM2mNodeDecoder decoder) {

        this(clientRegistry, securityRegistry, observationRegistry, modelProvider, encoder, decoder);
        createServer(endpoints);
    }

    /**
     * Creates a server which will bind to the specified endpoint addresses.
     *
     * @param localAddress the address to bind the CoAP server.
     * @param localSecureAddress the address to bind the CoAP server for DTLS connection.
     * @param clientRegistry The registry for keeping track of LWM2M client lifecycle.
     * @param securityRegistry The registry to use for looking up security parameters of clients.
     * @param observationRegistry The registry for keeping track of observed client resources.
     * @param modelProvider The registry to use for looking up LWM2M object definitions.
     * @param encoder The object to use for encoding message payload.
     * @param decoder The object to use for decoding message payload.
     * @throws IllegalArgumentException if any of the parameters is {@code null}.
     */
    public LeshanServer(InetSocketAddress localAddress, InetSocketAddress localSecureAddress,
            final ClientRegistry clientRegistry, final SecurityRegistry securityRegistry,
            final CaliforniumObservationRegistry observationRegistry, final LwM2mModelProvider modelProvider,
            LwM2mNodeEncoder encoder, LwM2mNodeDecoder decoder) {

        this(clientRegistry, securityRegistry, observationRegistry, modelProvider, encoder, decoder);
        Validate.notNull(localAddress, "IP address cannot be null");
        Validate.notNull(localSecureAddress, "Secure IP address cannot be null");

        Set<Endpoint> endpointsToUse = new HashSet<>();

        nonSecureEndpoint = new CoapEndpoint(localAddress, NetworkConfig.getStandard(),
                this.observationRegistry.getObservationStore());
        nonSecureEndpoint.addNotificationListener(observationRegistry);
        observationRegistry.setNonSecureEndpoint(nonSecureEndpoint);
        endpointsToUse.add(nonSecureEndpoint);

        secureEndpoint = createSecureEndpoint(localSecureAddress, securityRegistry, clientRegistry, observationRegistry);
        secureEndpoint.addNotificationListener(observationRegistry);
        observationRegistry.setSecureEndpoint(secureEndpoint);
        endpointsToUse.add(secureEndpoint);

        createServer(endpointsToUse);
    }

    private static Endpoint createSecureEndpoint(final InetSocketAddress localSecureAddress, final SecurityRegistry securityRegistry,
            final ClientRegistry clientRegistry, final CaliforniumObservationRegistry observationRegistry) {

        // secure endpoint
        Builder builder = new DtlsConnectorConfig.Builder(localSecureAddress);
        builder.setPskStore(new LwM2mPskStore(securityRegistry, clientRegistry));
        PrivateKey privateKey = securityRegistry.getServerPrivateKey();
        PublicKey publicKey = securityRegistry.getServerPublicKey();
        X509Certificate[] X509CertChain = securityRegistry.getServerX509CertChain();

        // if in raw key mode and not in X.509 set the raw keys
        if (X509CertChain == null && privateKey != null && publicKey != null) {
            builder.setIdentity(privateKey, publicKey);
        }
        // if in X.509 mode set the private key, certificate chain, public key is extracted from the certificate
        if (privateKey != null && X509CertChain != null && X509CertChain.length > 0) {
            builder.setIdentity(privateKey, X509CertChain, false);
        }

        Certificate[] trustedCertificates = securityRegistry.getTrustedCertificates();
        if (trustedCertificates != null && trustedCertificates.length > 0) {
            builder.setTrustStore(trustedCertificates);
        }

        return new CoapEndpoint(new DTLSConnector(builder.build()), NetworkConfig.getStandard(),
                observationRegistry.getObservationStore(), null);
    }

    private void createServer(final Set<Endpoint> endpoints) {

        // Cancel observations on client unregistering
        this.clientRegistry.addListener(new ClientRegistryListener() {

            @Override
            public void updated(final ClientUpdate update, final Client clientUpdated) {
            }

            @Override
            public void unregistered(final Client client) {
                LeshanServer.this.observationRegistry.cancelObservations(client);
                requestSender.cancelPendingRequests(client);
            }

            @Override
            public void registered(final Client client) {
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
                new RegistrationHandler(this.clientRegistry, this.securityRegistry));
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

        // create sender
        requestSender = new CaliforniumLwM2mRequestSender(Collections.unmodifiableSet(endpoints),
                this.observationRegistry, modelProvider, encoder, decoder);
    }

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

        LOG.info("LWM2M server stopped.");
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

        LOG.info("LWM2M server destroyed.");
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
    public <T extends LwM2mResponse> T send(final Client destination, final DownlinkRequest<T> request)
            throws InterruptedException {
        return requestSender.send(destination, request, null);
    }

    @Override
    public <T extends LwM2mResponse> T send(final Client destination, final DownlinkRequest<T> request, long timeout)
            throws InterruptedException {
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
