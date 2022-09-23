/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     RISE SICS AB - added Queue Mode operation
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.server;

import java.util.Collection;
import java.util.List;

import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.Startable;
import org.eclipse.leshan.core.Stoppable;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLinkParser;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.request.exception.ClientSleepingException;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpoint;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpointsProvider;
import org.eclipse.leshan.server.endpoint.ServerEndpointToolbox;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.observation.ObservationService;
import org.eclipse.leshan.server.observation.ObservationServiceImpl;
import org.eclipse.leshan.server.profile.DefaultClientProfileProvider;
import org.eclipse.leshan.server.queue.ClientAwakeTimeProvider;
import org.eclipse.leshan.server.queue.PresenceListener;
import org.eclipse.leshan.server.queue.PresenceService;
import org.eclipse.leshan.server.queue.PresenceServiceImpl;
import org.eclipse.leshan.server.queue.PresenceStateListener;
import org.eclipse.leshan.server.queue.QueueModeLwM2mRequestSender;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.eclipse.leshan.server.registration.RegistrationIdProvider;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationService;
import org.eclipse.leshan.server.registration.RegistrationServiceImpl;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.request.DefaultDownlinkRequestSender;
import org.eclipse.leshan.server.request.DefaultUplinkRequestReceiver;
import org.eclipse.leshan.server.request.DownlinkRequestSender;
import org.eclipse.leshan.server.request.LowerLayerConfig;
import org.eclipse.leshan.server.security.Authorizer;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityStore;
import org.eclipse.leshan.server.security.ServerSecurityInfo;
import org.eclipse.leshan.server.send.SendHandler;
import org.eclipse.leshan.server.send.SendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Lightweight M2M server.
 * <p>
 * This class is the entry point to send synchronous and asynchronous requests to registered clients.
 * <p>
 * The {@link LeshanServerBuilder} should be the preferred way to build an instance of {@link LeshanServer}.
 */
public class LeshanServer {

    private static final Logger LOG = LoggerFactory.getLogger(LeshanServer.class);

    // We choose a default timeout a bit higher to the MAX_TRANSMIT_WAIT(62-93s) which is the time from starting to
    // send a Confirmable message to the time when an acknowledgement is no longer expected.
    private static final long DEFAULT_TIMEOUT = 2 * 60 * 1000l; // 2min in ms

    // LWM2M attributes
    private final RegistrationServiceImpl registrationService;
    private final RegistrationStore registrationStore;
    private final SendHandler sendService;
    private final LwM2mServerEndpointsProvider endpointsProvider;

    private final ObservationServiceImpl observationService;
    private final SecurityStore securityStore;
    private final LwM2mModelProvider modelProvider;
    private PresenceServiceImpl presenceService;
    private final DownlinkRequestSender requestSender;

    /**
     * Initialize a server which will bind to the specified address and port.
     * <p>
     * {@link LeshanServerBuilder} is the priviledged way to create a {@link LeshanServer}.
     *
     * @param endpointsProvider which will create all available {@link LwM2mServerEndpoint}
     * @param registrationStore the {@link Registration} store.
     * @param securityStore the {@link SecurityInfo} store.
     * @param authorizer define which devices is allow to register on this server.
     * @param modelProvider provides the objects description for each client.
     * @param encoder encode used to encode request payload.
     * @param decoder decoder used to decode response payload.
     * @param noQueueMode true to disable presenceService.
     * @param awakeTimeProvider to set the client awake time if queue mode is used.
     * @param registrationIdProvider to provide registrationId using for location-path option values on response of
     *        Register operation.
     * @param updateRegistrationOnNotification will activate registration update on observe notification.
     * @param linkParser a parser {@link LwM2mLinkParser} used to parse a CoRE Link.
     * @param serverSecurityInfo credentials of the Server
     * @since 1.1
     */
    public LeshanServer(LwM2mServerEndpointsProvider endpointsProvider, RegistrationStore registrationStore,
            SecurityStore securityStore, Authorizer authorizer, LwM2mModelProvider modelProvider, LwM2mEncoder encoder,
            LwM2mDecoder decoder, boolean noQueueMode, ClientAwakeTimeProvider awakeTimeProvider,
            RegistrationIdProvider registrationIdProvider, boolean updateRegistrationOnNotification,
            LwM2mLinkParser linkParser, ServerSecurityInfo serverSecurityInfo) {

        Validate.notNull(endpointsProvider, "endpointsProvider cannot be null");
        Validate.notNull(registrationStore, "registration store cannot be null");
        Validate.notNull(authorizer, "authorizer cannot be null");
        Validate.notNull(modelProvider, "modelProvider cannot be null");
        Validate.notNull(encoder, "encoder cannot be null");
        Validate.notNull(decoder, "decoder cannot be null");
        Validate.notNull(registrationIdProvider, "registrationIdProvider cannot be null");

        // init services and stores
        this.endpointsProvider = endpointsProvider;
        this.registrationStore = registrationStore;
        registrationService = createRegistrationService(registrationStore);
        this.securityStore = securityStore;
        this.modelProvider = modelProvider;
        this.observationService = createObservationService(registrationStore, updateRegistrationOnNotification,
                endpointsProvider);
        if (noQueueMode) {
            presenceService = null;
        } else {
            presenceService = createPresenceService(registrationService, awakeTimeProvider,
                    updateRegistrationOnNotification);
        }
        this.sendService = createSendHandler();

        // create endpoints
        ServerEndpointToolbox toolbox = new ServerEndpointToolbox(decoder, encoder, linkParser,
                new DefaultClientProfileProvider(registrationStore, modelProvider));
        RegistrationHandler registrationHandler = new RegistrationHandler(registrationService, authorizer,
                registrationIdProvider);
        DefaultUplinkRequestReceiver requestReceiver = new DefaultUplinkRequestReceiver(registrationHandler,
                sendService);
        endpointsProvider.createEndpoints(requestReceiver, observationService, toolbox, serverSecurityInfo, this);

        // create request sender
        requestSender = createRequestSender(endpointsProvider, registrationService, this.modelProvider,
                presenceService);

    }

    protected RegistrationServiceImpl createRegistrationService(RegistrationStore registrationStore) {
        return new RegistrationServiceImpl(registrationStore);
    }

    protected ObservationServiceImpl createObservationService(RegistrationStore registrationStore,
            boolean updateRegistrationOnNotification, LwM2mServerEndpointsProvider endpointsProvider) {

        ObservationServiceImpl observationService = new ObservationServiceImpl(registrationStore, endpointsProvider,
                updateRegistrationOnNotification);
        return observationService;
    }

    protected PresenceServiceImpl createPresenceService(RegistrationService registrationService,
            ClientAwakeTimeProvider awakeTimeProvider, boolean updateRegistrationOnNotification) {
        PresenceServiceImpl presenceService = new PresenceServiceImpl(awakeTimeProvider);
        PresenceStateListener presenceStateListener = new PresenceStateListener(presenceService);
        registrationService.addListener(new PresenceStateListener(presenceService));
        if (updateRegistrationOnNotification) {
            observationService.addListener(presenceStateListener);
        }
        return presenceService;
    }

    protected SendHandler createSendHandler() {
        return new SendHandler();
    }

    protected DownlinkRequestSender createRequestSender(LwM2mServerEndpointsProvider endpointsProvider,
            RegistrationServiceImpl registrationService, LwM2mModelProvider modelProvider,
            PresenceServiceImpl presenceService) {

        // if no queue mode, create a "simple" sender
        final DownlinkRequestSender requestSender;
        if (presenceService == null)
            requestSender = new DefaultDownlinkRequestSender(endpointsProvider, modelProvider);
        else
            requestSender = new QueueModeLwM2mRequestSender(presenceService,
                    new DefaultDownlinkRequestSender(endpointsProvider, modelProvider));

        // Cancel observations on client unregistering
        registrationService.addListener(new RegistrationListener() {

            @Override
            public void updated(RegistrationUpdate update, Registration updatedRegistration, Registration previousReg) {
                if (!update.getAddress().equals(previousReg.getAddress())
                        || update.getPort() != previousReg.getPort()) {
                    requestSender.cancelOngoingRequests(previousReg);
                }
            }

            @Override
            public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
                    Registration newReg) {
                requestSender.cancelOngoingRequests(registration);
            }

            @Override
            public void registered(Registration registration, Registration previousReg,
                    Collection<Observation> previousObsersations) {
            }
        });

        return requestSender;
    }

    /**
     * Starts the server and binds it to the specified port.
     */
    public void start() {

        // Start stores
        if (registrationStore instanceof Startable) {
            ((Startable) registrationStore).start();
        }
        if (securityStore instanceof Startable) {
            ((Startable) securityStore).start();
        }
        if (requestSender instanceof Startable) {
            ((Startable) requestSender).start();
        }

        // Start server
        endpointsProvider.start();

        if (LOG.isInfoEnabled()) {
            LOG.info("LWM2M server started.");
            for (LwM2mServerEndpoint endpoint : endpointsProvider.getEndpoints()) {
                LOG.info("{} endpoint available at {}.", endpoint.getProtocol().getName(), endpoint.getURI());
            }
        }
    }

    /**
     * Stops the server and unbinds it from assigned ports (can be restarted).
     */
    public void stop() {
        // Stop server
        endpointsProvider.stop();

        // Stop stores
        if (registrationStore instanceof Stoppable) {
            ((Stoppable) registrationStore).stop();
        }
        if (securityStore instanceof Stoppable) {
            ((Stoppable) securityStore).stop();
        }
        if (requestSender instanceof Stoppable) {
            ((Stoppable) requestSender).stop();
        }

        LOG.info("LWM2M server stopped.");
    }

    /**
     * Destroys the server, unbinds from all ports and frees all system resources.
     * <p>
     * Server can not be restarted anymore.
     */
    public void destroy() {
        // Destroy server
        endpointsProvider.destroy();

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

        if (requestSender instanceof Destroyable) {
            ((Destroyable) requestSender).destroy();
        } else if (requestSender instanceof Stoppable) {
            ((Stoppable) requestSender).stop();
        }

        presenceService.destroy();

        LOG.info("LWM2M server destroyed.");
    }

    /**
     * Get the {@link RegistrationService} to access to registered clients.
     * <p>
     * You can use this object for listening client registration lifecycle.
     */
    public RegistrationService getRegistrationService() {
        return this.registrationService;
    }

    public RegistrationStore getRegistrationStore() {
        return registrationService.getStore();
    }

    /**
     * Get the {@link ObservationService} to access current observations.
     * <p>
     * You can use this object for listening resource observation or cancel it.
     */
    public ObservationService getObservationService() {
        return this.observationService;
    }

    /**
     * Get the {@link SendService} which can be used to listen data received from LWM2M client which are using
     * {@link SendRequest}.
     */
    public SendService getSendService() {
        return sendService;
    }

    /**
     * Get the {@link PresenceService} to get status of LWM2M clients connected with binding mode 'Q'.
     * <p>
     * You can use this object to add {@link PresenceListener} to get notified when a device comes online or offline.
     *
     */
    public PresenceService getPresenceService() {
        return this.presenceService;
    }

    /**
     * Get the SecurityStore containing of security information.
     */
    public SecurityStore getSecurityStore() {
        return this.securityStore;
    }

    /**
     * Get the provider in charge of retrieving the object definitions for each client.
     */
    public LwM2mModelProvider getModelProvider() {
        return this.modelProvider;
    }

    public List<LwM2mServerEndpoint> getEndpoints() {
        return endpointsProvider.getEndpoints();
    }

    public LwM2mServerEndpoint getEndpoint(Protocol protocol) {
        for (LwM2mServerEndpoint endpoint : endpointsProvider.getEndpoints()) {
            if (endpoint.getProtocol().equals(protocol)) {
                return endpoint;
            }
        }
        return null;
    }

    /**
     * Send a Lightweight M2M request synchronously using a default 2min timeout. Will block until a response is
     * received from the remote server.
     * <p>
     * The synchronous way could block a thread during a long time so it is more recommended to use the asynchronous
     * way.
     * <p>
     * We choose a default timeout a bit higher to the MAX_TRANSMIT_WAIT(62-93s) which is the time from starting to send
     * a Confirmable message to the time when an acknowledgement is no longer expected.
     *
     * @param destination The {@link Registration} associate to the device we want to sent the request.
     * @param request The request to send to the client.
     * @return the LWM2M response. The response can be <code>null</code> if the timeout expires (see
     *         https://github.com/eclipse/leshan/wiki/Request-Timeout).
     *
     * @throws CodecException if request payload can not be encoded.
     * @throws InterruptedException if the thread was interrupted.
     * @throws RequestRejectedException if the request is rejected by foreign peer.
     * @throws RequestCanceledException if the request is cancelled.
     * @throws SendFailedException if the request can not be sent. E.g. error at CoAP or DTLS/UDP layer.
     * @throws InvalidResponseException if the response received is malformed.
     * @throws ClientSleepingException if client is currently sleeping.
     */
    public <T extends LwM2mResponse> T send(Registration destination, DownlinkRequest<T> request)
            throws InterruptedException {
        return send(destination, request, DEFAULT_TIMEOUT);
    }

    /**
     * Send a Lightweight M2M request synchronously. Will block until a response is received from the remote server.
     * <p>
     * The synchronous way could block a thread during a long time so it is more recommended to use the asynchronous
     * way.
     *
     * @param destination The {@link Registration} associate to the device we want to sent the request.
     * @param request The request to send to the client.
     * @param timeoutInMs The global timeout to wait in milliseconds (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout)
     * @return the LWM2M response. The response can be <code>null</code> if the timeout expires (see
     *         https://github.com/eclipse/leshan/wiki/Request-Timeout).
     *
     * @throws CodecException if request payload can not be encoded.
     * @throws InterruptedException if the thread was interrupted.
     * @throws RequestRejectedException if the request is rejected by foreign peer.
     * @throws RequestCanceledException if the request is cancelled.
     * @throws SendFailedException if the request can not be sent. E.g. error at CoAP or DTLS/UDP layer.
     * @throws InvalidResponseException if the response received is malformed.
     * @throws ClientSleepingException if client is currently sleeping.
     */
    public <T extends LwM2mResponse> T send(Registration destination, DownlinkRequest<T> request, long timeoutInMs)
            throws InterruptedException {
        return send(destination, request, null, timeoutInMs);
    }

    /**
     * Send a Lightweight M2M request synchronously. Will block until a response is received from the remote server.
     * <p>
     * The synchronous way could block a thread during a long time so it is more recommended to use the asynchronous
     * way.
     *
     * @param destination The {@link Registration} associate to the device we want to sent the request.
     * @param request The request to send to the client.
     * @param lowerLayerConfig to tweak lower layer request (e.g. coap request)
     * @param timeoutInMs The global timeout to wait in milliseconds (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout)
     * @return the LWM2M response. The response can be <code>null</code> if the timeout expires (see
     *         https://github.com/eclipse/leshan/wiki/Request-Timeout).
     *
     * @throws CodecException if request payload can not be encoded.
     * @throws InterruptedException if the thread was interrupted.
     * @throws RequestRejectedException if the request is rejected by foreign peer.
     * @throws RequestCanceledException if the request is cancelled.
     * @throws SendFailedException if the request can not be sent. E.g. error at CoAP or DTLS/UDP layer.
     * @throws InvalidResponseException if the response received is malformed.
     * @throws ClientSleepingException if client is currently sleeping.
     */
    public <T extends LwM2mResponse> T send(Registration destination, DownlinkRequest<T> request,
            LowerLayerConfig lowerLayerConfig, long timeoutInMs) throws InterruptedException {
        return requestSender.send(destination, request, lowerLayerConfig, timeoutInMs);
    }

    /**
     * Send a Lightweight M2M {@link DownlinkRequest} asynchronously to a LWM2M client using a default 2min timeout.
     * <p>
     * The Californium API does not ensure that message callback are exclusive. E.g. In some race condition, you can get
     * a onReponse call and a onCancel one. This method ensures that you will receive only one event. Meaning, you get
     * either 1 response or 1 error.
     * <p>
     * We choose a default timeout a bit higher to the MAX_TRANSMIT_WAIT(62-93s) which is the time from starting to send
     * a Confirmable message to the time when an acknowledgement is no longer expected.
     *
     * @param destination The {@link Registration} associate to the device we want to sent the request.
     * @param request The request to send to the client.
     * @param responseCallback a callback called when a response is received (successful or error response). This
     *        callback MUST NOT be null.
     * @param errorCallback a callback called when an error or exception occurred when response is received. It can be :
     *        <ul>
     *        <li>{@link RequestRejectedException} if the request is rejected by foreign peer.</li>
     *        <li>{@link RequestCanceledException} if the request is cancelled.</li>
     *        <li>{@link SendFailedException} if the request can not be sent. E.g. error at CoAP or DTLS/UDP layer.</li>
     *        <li>{@link InvalidResponseException} if the response received is malformed.</li>
     *        <li>{@link ClientSleepingException} if client is currently sleeping.</li>
     *        <li>{@link TimeoutException} if the timeout expires (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout).</li>
     *        <li>or any other RuntimeException for unexpected issue.
     *        </ul>
     *        This callback MUST NOT be null.
     * @throws CodecException if request payload can not be encoded.
     */
    public <T extends LwM2mResponse> void send(Registration destination, DownlinkRequest<T> request,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback) {
        send(destination, request, DEFAULT_TIMEOUT, responseCallback, errorCallback);
    }

    /**
     * Send a Lightweight M2M {@link DownlinkRequest} asynchronously to a LWM2M client.
     * <p>
     * The Californium API does not ensure that message callback are exclusive. E.g. In some race condition, you can get
     * a onReponse call and a onCancel one. This method ensures that you will receive only one event. Meaning, you get
     * either 1 response or 1 error.
     *
     * @param destination The {@link Registration} associate to the device we want to sent the request.
     * @param request The request to send to the client.
     * @param timeoutInMs The global timeout to wait in milliseconds (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout)
     * @param responseCallback a callback called when a response is received (successful or error response). This
     *        callback MUST NOT be null.
     * @param errorCallback a callback called when an error or exception occurred when response is received. It can be :
     *        <ul>
     *        <li>{@link RequestRejectedException} if the request is rejected by foreign peer.</li>
     *        <li>{@link RequestCanceledException} if the request is cancelled.</li>
     *        <li>{@link SendFailedException} if the request can not be sent. E.g. error at CoAP or DTLS/UDP layer.</li>
     *        <li>{@link InvalidResponseException} if the response received is malformed.</li>
     *        <li>{@link ClientSleepingException} if client is currently sleeping.</li>
     *        <li>{@link TimeoutException} if the timeout expires (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout).</li>
     *        <li>or any other RuntimeException for unexpected issue.
     *        </ul>
     *        This callback MUST NOT be null.
     * @throws CodecException if request payload can not be encoded.
     */
    public <T extends LwM2mResponse> void send(Registration destination, DownlinkRequest<T> request, long timeoutInMs,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback) {
        send(destination, request, null, timeoutInMs, responseCallback, errorCallback);
    }

    /**
     * Send a Lightweight M2M {@link DownlinkRequest} asynchronously to a LWM2M client.
     * <p>
     * The Californium API does not ensure that message callback are exclusive. E.g. In some race condition, you can get
     * a onReponse call and a onCancel one. This method ensures that you will receive only one event. Meaning, you get
     * either 1 response or 1 error.
     *
     * @param destination The {@link Registration} associate to the device we want to sent the request.
     * @param request The request to send to the client.
     * @param lowerLayerConfig to tweak lower layer request (e.g. coap request)
     * @param timeoutInMs The global timeout to wait in milliseconds (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout)
     * @param responseCallback a callback called when a response is received (successful or error response). This
     *        callback MUST NOT be null.
     * @param errorCallback a callback called when an error or exception occurred when response is received. It can be :
     *        <ul>
     *        <li>{@link RequestRejectedException} if the request is rejected by foreign peer.</li>
     *        <li>{@link RequestCanceledException} if the request is cancelled.</li>
     *        <li>{@link SendFailedException} if the request can not be sent. E.g. error at CoAP or DTLS/UDP layer.</li>
     *        <li>{@link InvalidResponseException} if the response received is malformed.</li>
     *        <li>{@link ClientSleepingException} if client is currently sleeping.</li>
     *        <li>{@link TimeoutException} if the timeout expires (see
     *        https://github.com/eclipse/leshan/wiki/Request-Timeout).</li>
     *        <li>or any other RuntimeException for unexpected issue.
     *        </ul>
     *        This callback MUST NOT be null.
     * @throws CodecException if request payload can not be encoded.
     *
     * @since 1.2
     */
    public <T extends LwM2mResponse> void send(Registration destination, DownlinkRequest<T> request,
            LowerLayerConfig lowerLayerConfig, long timeoutInMs, ResponseCallback<T> responseCallback,
            ErrorCallback errorCallback) {
        requestSender.send(destination, request, lowerLayerConfig, timeoutInMs, responseCallback, errorCallback);
    }
}
