/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
 *     Michał Wadowski (Orange) - Add Observe-Composite feature.
 *******************************************************************************/
package org.eclipse.leshan.server.californium.observation;

import static org.eclipse.leshan.core.californium.ResponseCodeUtil.toLwM2mResponseCode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.observe.NotificationListener;
import org.eclipse.californium.core.observe.ObservationStore;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.californium.EndpointContextUtil;
import org.eclipse.leshan.core.californium.ObserveUtil;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.response.AbstractLwM2mResponse;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.observation.ObservationService;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.registration.UpdatedRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link ObservationService} accessing the persisted observation via the provided
 * {@link CaliforniumRegistrationStore}.
 *
 * When a new observation is added or changed or canceled, the registered listeners are notified.
 */
public class ObservationServiceImpl implements ObservationService, NotificationListener {

    private final Logger LOG = LoggerFactory.getLogger(ObservationServiceImpl.class);

    private final CaliforniumRegistrationStore registrationStore;
    private final LwM2mModelProvider modelProvider;
    private final LwM2mDecoder decoder;
    private Endpoint secureEndpoint;
    private Endpoint nonSecureEndpoint;
    private final boolean updateRegistrationOnNotification;

    private final List<ObservationListener> listeners = new CopyOnWriteArrayList<>();;

    /**
     * Creates an instance of {@link ObservationServiceImpl}
     *
     * @param store instance of californium's {@link ObservationStore}
     * @param modelProvider instance of {@link LwM2mModelProvider}
     * @param decoder instance of {@link LwM2mDecoder}
     */
    public ObservationServiceImpl(CaliforniumRegistrationStore store, LwM2mModelProvider modelProvider,
            LwM2mDecoder decoder) {
        this(store, modelProvider, decoder, false);
    }

    /**
     * Creates an instance of {@link ObservationServiceImpl}
     *
     * @param store instance of californium's {@link ObservationStore}
     * @param modelProvider instance of {@link LwM2mModelProvider}
     * @param decoder instance of {@link LwM2mDecoder}
     * @param updateRegistrationOnNotification will activate registration update on observe notification.
     *
     * @since 1.1
     */
    public ObservationServiceImpl(CaliforniumRegistrationStore store, LwM2mModelProvider modelProvider,
            LwM2mDecoder decoder, boolean updateRegistrationOnNotification) {
        this.registrationStore = store;
        this.modelProvider = modelProvider;
        this.decoder = decoder;
        this.updateRegistrationOnNotification = updateRegistrationOnNotification;
    }

    public void addObservation(Registration registration, Observation observation) {
        for (Observation existing : registrationStore.addObservation(registration.getId(), observation)) {
            cancel(existing);
        }

        for (ObservationListener listener : listeners) {
            listener.newObservation(observation, registration);
        }
    }

    public void setNonSecureEndpoint(Endpoint endpoint) {
        nonSecureEndpoint = endpoint;
    }

    public void setSecureEndpoint(Endpoint endpoint) {
        secureEndpoint = endpoint;
    }

    @Override
    public int cancelObservations(Registration registration) {
        // check registration id
        String registrationId = registration.getId();
        if (registrationId == null)
            return 0;

        Collection<Observation> observations = registrationStore.removeObservations(registrationId);
        if (observations == null)
            return 0;

        for (Observation observation : observations) {
            cancel(observation);
        }

        return observations.size();
    }

    @Override
    public int cancelObservations(Registration registration, String nodePath) {
        if (registration == null || registration.getId() == null || nodePath == null || nodePath.isEmpty())
            return 0;

        Set<Observation> observations = getObservations(registration.getId(), nodePath);
        for (Observation observation : observations) {
            cancelObservation(observation);
        }
        return observations.size();
    }

    @Override
    public int cancelCompositeObservations(Registration registration, String[] nodePaths) {
        if (registration == null || registration.getId() == null || nodePaths == null || nodePaths.length == 0)
            return 0;

        Set<Observation> observations = getCompositeObservations(registration.getId(), nodePaths);
        for (Observation observation : observations) {
            cancelObservation(observation);
        }
        return observations.size();
    }

    @Override
    public void cancelObservation(Observation observation) {
        if (observation == null)
            return;

        registrationStore.removeObservation(observation.getRegistrationId(), observation.getId());
        cancel(observation);
    }

    private void cancel(Observation observation) {
        Token token = new Token(observation.getId());
        if (secureEndpoint != null)
            secureEndpoint.cancelObservation(token);
        if (nonSecureEndpoint != null)
            nonSecureEndpoint.cancelObservation(token);

        for (ObservationListener listener : listeners) {
            listener.cancelled(observation);
        }
    }

    @Override
    public Set<Observation> getObservations(Registration registration) {
        return getObservations(registration.getId());
    }

    private Set<Observation> getObservations(String registrationId) {
        if (registrationId == null)
            return Collections.emptySet();

        return new HashSet<>(registrationStore.getObservations(registrationId));
    }

    private Set<Observation> getCompositeObservations(String registrationId, String[] nodePaths) {
        if (registrationId == null || nodePaths == null)
            return Collections.emptySet();

        // array of String to array of LWM2M path
        List<LwM2mPath> lwPaths = new ArrayList<>(nodePaths.length);
        for (int i = 0; i < nodePaths.length; i++) {
            lwPaths.add(new LwM2mPath(nodePaths[i]));
        }

        // search composite-observation
        Set<Observation> result = new HashSet<>();
        for (Observation obs : getObservations(registrationId)) {
            if (obs instanceof CompositeObservation) {
                if (lwPaths.equals(((CompositeObservation) obs).getPaths())) {
                    result.add(obs);
                }
            }
        }
        return result;
    }

    private Set<Observation> getObservations(String registrationId, String nodePath) {
        if (registrationId == null || nodePath == null)
            return Collections.emptySet();

        Set<Observation> result = new HashSet<>();
        LwM2mPath lwPath = new LwM2mPath(nodePath);
        for (Observation obs : getObservations(registrationId)) {
            if (obs instanceof SingleObservation) {
                if (lwPath.equals(((SingleObservation) obs).getPath())) {
                    result.add(obs);
                }
            }
        }
        return result;
    }

    /**
     * @return the Californium {@link ObservationStore}
     */
    public ObservationStore getObservationStore() {
        return registrationStore;
    }

    @Override
    public void addListener(ObservationListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ObservationListener listener) {
        listeners.remove(listener);
    }

    // ********** NotificationListener interface **********//

    @Override
    public void onNotification(Request coapRequest, Response coapResponse) {
        LOG.trace("notification received for request {}: {}", coapRequest, coapResponse);

        if (listeners.isEmpty())
            return;

        // get registration Id
        String regid = coapRequest.getUserContext().get(ObserveUtil.CTX_REGID);

        // get observation for this request
        Observation observation = registrationStore.getObservation(regid, coapResponse.getToken().getBytes());
        if (observation == null) {
            LOG.error("Unexpected error: Unable to find observation with token {} for registration {}",
                    coapResponse.getToken(), regid);
            return;
        }

        // get registration
        Registration registration;
        if (updateRegistrationOnNotification) {
            Identity obsIdentity = EndpointContextUtil.extractIdentity(coapResponse.getSourceContext());
            RegistrationUpdate regUpdate = new RegistrationUpdate(observation.getRegistrationId(), obsIdentity, null,
                    null, null, null, null, null);
            UpdatedRegistration updatedRegistration = registrationStore.updateRegistration(regUpdate);
            if (updatedRegistration == null || updatedRegistration.getUpdatedRegistration() == null) {
                LOG.error("Unexpected error: There is no registration with id {} for this observation {}",
                        observation.getRegistrationId(), observation);
                return;
            }
            registration = updatedRegistration.getUpdatedRegistration();
        } else {
            registration = registrationStore.getRegistration(observation.getRegistrationId());
            if (registration == null) {
                LOG.error("Unexpected error: There is no registration with id {} for this observation {}",
                        observation.getRegistrationId(), observation);
                return;
            }
        }

        try {
            // get model for this registration
            LwM2mModel model = modelProvider.getObjectModel(registration);

            // create response
            AbstractLwM2mResponse response = createObserveResponse(observation, model, coapResponse);

            if (response != null) {
                // notify all listeners
                for (ObservationListener listener : listeners) {
                    if (observation instanceof SingleObservation && response instanceof ObserveResponse) {
                        listener.onResponse((SingleObservation) observation, registration, (ObserveResponse) response);
                    }
                    if (observation instanceof CompositeObservation && response instanceof ObserveCompositeResponse) {
                        listener.onResponse((CompositeObservation) observation, registration,
                                (ObserveCompositeResponse) response);
                    }
                }
            }
        } catch (InvalidResponseException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Invalid notification for observation [%s]", observation), e);
            }

            for (ObservationListener listener : listeners) {
                listener.onError(observation, registration, e);
            }
        } catch (RuntimeException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error(String.format("Unable to handle notification for observation [%s]", observation), e);
            }

            for (ObservationListener listener : listeners) {
                listener.onError(observation, registration, e);
            }
        }
    }

    private AbstractLwM2mResponse createObserveResponse(Observation observation, LwM2mModel model,
            Response coapResponse) {
        // CHANGED response is supported for backward compatibility with old spec.
        if (coapResponse.getCode() != CoAP.ResponseCode.CHANGED
                && coapResponse.getCode() != CoAP.ResponseCode.CONTENT) {
            throw new InvalidResponseException("Unexpected response code [%s] for %s", coapResponse.getCode(),
                    observation);
        }

        // get content format
        ContentFormat contentFormat = null;
        if (coapResponse.getOptions().hasContentFormat()) {
            contentFormat = ContentFormat.fromCode(coapResponse.getOptions().getContentFormat());
        }

        // decode response
        try {
            ResponseCode responseCode = toLwM2mResponseCode(coapResponse.getCode());

            if (observation instanceof SingleObservation) {
                SingleObservation singleObservation = (SingleObservation) observation;

                List<TimestampedLwM2mNode> timestampedNodes = decoder.decodeTimestampedData(coapResponse.getPayload(),
                        contentFormat, singleObservation.getPath(), model);

                // create lwm2m response
                if (timestampedNodes.size() == 1 && !timestampedNodes.get(0).isTimestamped()) {
                    return new ObserveResponse(responseCode, timestampedNodes.get(0).getNode(), null, singleObservation,
                            null, coapResponse);
                } else {
                    return new ObserveResponse(responseCode, null, timestampedNodes, singleObservation, null,
                            coapResponse);
                }
            } else if (observation instanceof CompositeObservation) {

                CompositeObservation compositeObservation = (CompositeObservation) observation;

                Map<LwM2mPath, LwM2mNode> nodes = decoder.decodeNodes(coapResponse.getPayload(), contentFormat,
                        compositeObservation.getPaths(), model);

                return new ObserveCompositeResponse(responseCode, nodes, null, coapResponse, compositeObservation);
            }

            throw new IllegalStateException(
                    "observation must be a CompositeObservation or a SingleObservation but was " + observation == null
                            ? null
                            : observation.getClass().getSimpleName());
        } catch (CodecException e) {
            if (LOG.isDebugEnabled()) {
                byte[] payload = coapResponse.getPayload() == null ? new byte[0] : coapResponse.getPayload();
                LOG.debug(String.format("Unable to decode notification payload [%s] of observation [%s] ",
                        Hex.encodeHexString(payload), observation), e);
            }
            throw new InvalidResponseException(e, "Unable to decode notification payload  of observation [%s] ",
                    observation);
        }
    }
}
