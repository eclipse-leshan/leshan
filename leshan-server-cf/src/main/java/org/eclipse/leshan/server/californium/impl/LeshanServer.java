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
 *     RISE SICS AB - added Queue Mode operation
 *******************************************************************************/
package org.eclipse.leshan.server.californium.impl;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.leshan.core.californium.CoapResponseCallback;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.exception.ClientSleepingException;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.Destroyable;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.Startable;
import org.eclipse.leshan.server.Stoppable;
import org.eclipse.leshan.server.californium.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.californium.CoapRequestSender;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.impl.RegistrationServiceImpl;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.observation.ObservationService;
import org.eclipse.leshan.server.queue.ClientAwakeTimeProvider;
import org.eclipse.leshan.server.queue.PresenceService;
import org.eclipse.leshan.server.queue.PresenceServiceImpl;
import org.eclipse.leshan.server.queue.PresenceStateListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.eclipse.leshan.server.registration.RegistrationIdProvider;
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

    // We choose a default timeout a bit higher to the MAX_TRANSMIT_WAIT(62-93s) which is the time from starting to
    // send a Confirmable message to the time when an acknowledgement is no longer expected.
    private static final long DEFAULT_TIMEOUT = 2 * 60 * 1000l; // 2min in ms

    private final LwM2mRequestSender requestSender;

    private final RegistrationServiceImpl registrationService;

    private final ObservationServiceImpl observationService;

    private final SecurityStore securityStore;

    private final LwM2mModelProvider modelProvider;

    private final CoapEndpoint unsecuredEndpoint;

    private final CoapEndpoint securedEndpoint;

    private final PresenceServiceImpl presenceService;

    private final CaliforniumRegistrationStore registrationStore;

    private final CoapAPI coapApi;

    /**
     * Initialize a server which will bind to the specified address and port.
     *
     * @param unsecuredEndpoint the unsecure coap endpoint.
     * @param securedEndpoint the secure coap endpoint.
     * @param registrationStore the {@link Registration} store.
     * @param securityStore the {@link SecurityInfo} store.
     * @param authorizer define which devices is allow to register on this server.
     * @param modelProvider provides the objects description for each client.
     * @param decoder decoder used to decode response payload.
     * @param encoder encode used to encode request payload.
     * @param coapConfig the CoAP {@link NetworkConfig}.
     * @param noQueueMode true to disable presenceService.
     * @param awakeTimeProvider to set the client awake time if queue mode is used.
     * @param registrationIdProvider to provide registrationId using for location-path option values on response of
     *        Register operation.
     */
    public LeshanServer(CoapEndpoint unsecuredEndpoint, CoapEndpoint securedEndpoint,
            CaliforniumRegistrationStore registrationStore, SecurityStore securityStore, Authorizer authorizer,
            LwM2mModelProvider modelProvider, LwM2mNodeEncoder encoder, LwM2mNodeDecoder decoder,
            NetworkConfig coapConfig, boolean noQueueMode, ClientAwakeTimeProvider awakeTimeProvider,
            RegistrationIdProvider registrationIdProvider) {

        Validate.notNull(registrationStore, "registration store cannot be null");
        Validate.notNull(authorizer, "authorizer cannot be null");
        Validate.notNull(modelProvider, "modelProvider cannot be null");
        Validate.notNull(encoder, "encoder cannot be null");
        Validate.notNull(decoder, "decoder cannot be null");
        Validate.notNull(coapConfig, "coapConfig cannot be null");

        // Init services and stores
        this.registrationStore = registrationStore;
        registrationService = new RegistrationServiceImpl(registrationStore);
        this.securityStore = securityStore;
        observationService = new ObservationServiceImpl(registrationStore, modelProvider, decoder);
        this.modelProvider = modelProvider;

        // Cancel observations on client unregistering
        registrationService.addListener(new RegistrationListener() {

            @Override
            public void updated(RegistrationUpdate update, Registration updatedRegistration,
                    Registration previousRegistration) {
            }

            @Override
            public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
                    Registration newReg) {
                requestSender.cancelPendingRequests(registration);
            }

            @Override
            public void registered(Registration registration, Registration previousReg,
                    Collection<Observation> previousObsersations) {
            }
        });

        // define a set of endpoints
        Set<Endpoint> endpoints = new HashSet<>();
        coapServer = new CoapServer(coapConfig) {
            @Override
            protected Resource createRoot() {
                return new RootResource();
            }
        };

        // default endpoint
        this.unsecuredEndpoint = unsecuredEndpoint;
        if (unsecuredEndpoint != null) {
            unsecuredEndpoint.addNotificationListener(observationService);
            observationService.setNonSecureEndpoint(unsecuredEndpoint);
            coapServer.addEndpoint(unsecuredEndpoint);
            endpoints.add(unsecuredEndpoint);
        }

        // secure endpoint
        this.securedEndpoint = securedEndpoint;
        if (securedEndpoint != null) {
            securedEndpoint.addNotificationListener(observationService);
            observationService.setSecureEndpoint(securedEndpoint);
            coapServer.addEndpoint(securedEndpoint);
            endpoints.add(securedEndpoint);
        }

        // define /rd resource
        RegisterResource rdResource = new RegisterResource(
                new RegistrationHandler(this.registrationService, authorizer, registrationIdProvider));
        coapServer.add(rdResource);

        // create sender
        // notify applications of LWM2M client coming online/offline
        if (noQueueMode) {
            // if no queue mode, create a "simple" sender
            requestSender = new CaliforniumLwM2mRequestSender(endpoints, observationService, modelProvider, encoder,
                    decoder);
            presenceService = null;
        } else {
            presenceService = new PresenceServiceImpl(awakeTimeProvider);
            registrationService.addListener(new PresenceStateListener(presenceService));
            requestSender = new CaliforniumQueueModeRequestSender(presenceService,
                    new CaliforniumLwM2mRequestSender(endpoints, observationService, modelProvider, encoder, decoder));
        }

        coapApi = new CoapAPI();
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
            LOG.info("LWM2M server started at {} {}",
                    getUnsecuredAddress() == null ? "" : "coap://" + getUnsecuredAddress(),
                    getSecuredAddress() == null ? "" : "coaps://" + getSecuredAddress());
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
    public PresenceService getPresenceService() {
        return this.presenceService;
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
        return requestSender.send(destination, request, DEFAULT_TIMEOUT);
    }

    @Override
    public <T extends LwM2mResponse> T send(Registration destination, DownlinkRequest<T> request, long timeout)
            throws InterruptedException {
        return requestSender.send(destination, request, timeout);
    }

    @Override
    public <T extends LwM2mResponse> void send(Registration destination, DownlinkRequest<T> request,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback) {
        requestSender.send(destination, request, DEFAULT_TIMEOUT, responseCallback, errorCallback);
    }

    @Override
    public <T extends LwM2mResponse> void send(Registration destination, DownlinkRequest<T> request, long timeout,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback) {
        requestSender.send(destination, request, timeout, responseCallback, errorCallback);
    }

    public InetSocketAddress getUnsecuredAddress() {
        if (unsecuredEndpoint != null) {
            return unsecuredEndpoint.getAddress();
        } else {
            return null;
        }
    }

    public InetSocketAddress getSecuredAddress() {
        if (securedEndpoint != null) {
            return securedEndpoint.getAddress();
        } else {
            return null;
        }
    }

    /**
     * A CoAP API, generally needed when you want to mix LWM2M and CoAP protocol.
     */
    public CoapAPI coap() {
        return coapApi;
    }

    public class CoapAPI {

        /**
         * @return the underlying {@link CoapServer}
         */
        public CoapServer getServer() {
            return coapServer;
        }

        /**
         * Sends a CoAP request synchronously to a registered LWM2M device. Will block until a response is received from
         * the remote client.
         * 
         * @param destination the remote client
         * @param request the request to the client
         * @return the response or <code>null</code> if the CoAP timeout expires ( see
         *         http://tools.ietf.org/html/rfc7252#section-4.2 ).
         * 
         * @throws CodecException if request payload can not be encoded.
         * @throws InterruptedException if the thread was interrupted.
         * @throws RequestRejectedException if the request is rejected by foreign peer.
         * @throws RequestCanceledException if the request is cancelled.
         * @throws InvalidResponseException if the response received is malformed.
         * @throws ClientSleepingException if the client is sleeping and then the request cannot be sent. This exception
         *         will never be raised if Queue Mode support is deactivate.
         */
        public Response send(Registration destination, Request request) throws InterruptedException {
            // Ensure that delegated sender is able to send CoAP request
            if (!(requestSender instanceof CoapRequestSender)) {
                throw new UnsupportedOperationException("This sender does not support to send CoAP request");
            }
            CoapRequestSender sender = (CoapRequestSender) requestSender;

            return sender.sendCoapRequest(destination, request, DEFAULT_TIMEOUT);
        }

        /**
         * Sends a CoAP request synchronously to a registered LWM2M device. Will block until a response is received from
         * the remote client.
         * 
         * @param destination the remote client
         * @param request the request to send to the client
         * @param timeout the request timeout in millisecond
         * @return the response or <code>null</code> if the timeout expires (given parameter or CoAP timeout).
         * 
         * @throws CodecException if request payload can not be encoded.
         * @throws InterruptedException if the thread was interrupted.
         * @throws RequestRejectedException if the request is rejected by foreign peer.
         * @throws RequestCanceledException if the request is cancelled.
         * @throws InvalidResponseException if the response received is malformed.
         * @throws ClientSleepingException if the client is sleeping and then the request cannot be sent. This exception
         *         will never be raised if Queue Mode support is deactivate.
         */
        public Response send(Registration destination, Request request, long timeout) throws InterruptedException {
            // Ensure that delegated sender is able to send CoAP request
            if (!(requestSender instanceof CoapRequestSender)) {
                throw new UnsupportedOperationException("This sender does not support to send CoAP request");
            }
            CoapRequestSender sender = (CoapRequestSender) requestSender;

            return sender.sendCoapRequest(destination, request, timeout);
        }

        /**
         * Sends a CoAP request asynchronously to a registered LWM2M device.
         * 
         * @param destination the remote client
         * @param request the request to send to the client
         * @param responseCallback a callback called when a response is received (successful or error response)
         * @param errorCallback a callback called when an error or exception occurred when response is received. It can
         *        be :
         *        <ul>
         *        <li>{@link RequestRejectedException} if the request is rejected by foreign peer.</li>
         *        <li>{@link RequestCanceledException} if the request is cancelled.</li>
         *        <li>{@link InvalidResponseException} if the response received is malformed.</li>
         *        <li>{@link TimeoutException} if the CoAP timeout expires ( see
         *        http://tools.ietf.org/html/rfc7252#section-4.2 ).</li>
         *        <li>or any other RuntimeException for unexpected issue.
         *        </ul>
         * @throws CodecException if request payload can not be encoded.
         * @throws ClientSleepingException if the client is sleeping and then the request cannot be sent. This exception
         *         will never be raised if Queue Mode support is deactivate.
         */
        public void send(Registration destination, Request request, CoapResponseCallback responseCallback,
                ErrorCallback errorCallback) {
            // Ensure that delegated sender is able to send CoAP request
            if (!(requestSender instanceof CoapRequestSender)) {
                throw new UnsupportedOperationException("This sender does not support to send CoAP request");
            }
            CoapRequestSender sender = (CoapRequestSender) requestSender;

            sender.sendCoapRequest(destination, request, DEFAULT_TIMEOUT, responseCallback, errorCallback);
        }

        /**
         * Sends a CoAP request asynchronously to a registered LWM2M device.
         * 
         * @param destination the remote client
         * @param request the request to send to the client
         * @param timeout the request timeout in millisecond
         * @param responseCallback a callback called when a response is received (successful or error response)
         * @param errorCallback a callback called when an error or exception occurred when response is received. It can
         *        be :
         *        <ul>
         *        <li>{@link RequestRejectedException} if the request is rejected by foreign peer.</li>
         *        <li>{@link RequestCanceledException} if the request is cancelled.</li>
         *        <li>{@link InvalidResponseException} if the response received is malformed.</li>
         *        <li>{@link TimeoutException} if the CoAP timeout expires ( see
         *        http://tools.ietf.org/html/rfc7252#section-4.2 ).</li>
         *        <li>or any other RuntimeException for unexpected issue.
         *        </ul>
         * @throws CodecException if request payload can not be encoded.
         * @throws ClientSleepingException if the client is sleeping and then the request cannot be sent. This exception
         *         will never be raised if Queue Mode support is deactivate.
         */
        public void send(Registration destination, Request request, long timeout, CoapResponseCallback responseCallback,
                ErrorCallback errorCallback) {
            // Ensure that delegated sender is able to send CoAP request
            if (!(requestSender instanceof CoapRequestSender)) {
                throw new UnsupportedOperationException("This sender does not support to send CoAP request");
            }
            CoapRequestSender sender = (CoapRequestSender) requestSender;

            sender.sendCoapRequest(destination, request, timeout, responseCallback, errorCallback);
        }
    }

    /**
     * @return the underlying {@link CoapServer}
     * @Deprecated use coap().{@link CoapAPI#getServer() getServer()} instead
     */
    @Deprecated
    public CoapServer getCoapServer() {
        return coapServer;
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
