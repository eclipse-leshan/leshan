/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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

import static org.eclipse.leshan.server.californium.impl.CoapRequestBuilder.CTX_REGID;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.observe.NotificationListener;
import org.eclipse.californium.core.observe.ObservationStore;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.codec.InvalidValueException;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.californium.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.client.Registration;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.observation.ObservationService;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link ObservationService} accessing the persisted observation via the provided
 * {@link LwM2mObservationStore}.
 * 
 * When a new observation is added or changed or canceled, the registered listeners are notified.
 */
public class ObservationServiceImpl implements ObservationService, NotificationListener {

    private final Logger LOG = LoggerFactory.getLogger(ObservationServiceImpl.class);

    private final CaliforniumRegistrationStore registrationStore;
    private final LwM2mModelProvider modelProvider;
    private final LwM2mNodeDecoder decoder;
    private Endpoint secureEndpoint;
    private Endpoint nonSecureEndpoint;

    private final List<ObservationListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Creates an instance of {@link ObservationServiceImpl}
     * 
     * @param store instance of californium's {@link ObservationStore}
     * @param modelProvider instance of {@link LwM2mModelProvider}
     * @param decoder instance of {@link LwM2mNodeDecoder}
     */
    public ObservationServiceImpl(CaliforniumRegistrationStore store, LwM2mModelProvider modelProvider,
            LwM2mNodeDecoder decoder) {
        this.registrationStore = store;
        this.modelProvider = modelProvider;
        this.decoder = decoder;
    }

    public void addObservation(Observation observation) {
        // cancel any other observation for the same path and registration id.
        // delegate this to the observation store to avoid race conditions on add/cancel?
        for (Observation obs : getObservations(observation.getRegistrationId())) {
            if (observation.getPath().equals(obs.getPath()) && !Arrays.equals(observation.getId(), obs.getId())) {
                cancelObservation(obs);
            }
        }

        // the observation is already persisted by the CoAP layer

        for (ObservationListener listener : listeners) {
            listener.newObservation(observation);
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

        Collection<Observation> observations = registrationStore
                .removeObservations(registrationId);
        if (observations == null)
            return 0;

        for (Observation observation : observations) {
            if (secureEndpoint != null)
                secureEndpoint.cancelObservation(observation.getId());
            if (nonSecureEndpoint != null)
                nonSecureEndpoint.cancelObservation(observation.getId());

            for (ObservationListener listener : listeners) {
                listener.cancelled(observation);
            }
        }

        return observations.size();
    }

    @Override
    public int cancelObservations(Registration registration, String resourcepath) {
        if (registration == null || registration.getId() == null || resourcepath == null || resourcepath.isEmpty())
            return 0;

        Set<Observation> observations = getObservations(registration.getId(), resourcepath);
        for (Observation observation : observations) {
            cancelObservation(observation);
        }
        return observations.size();
    }

    @Override
    public void cancelObservation(Observation observation) {
        if (observation == null)
            return;

        if (secureEndpoint != null)
            secureEndpoint.cancelObservation(observation.getId());
        if (nonSecureEndpoint != null)
            nonSecureEndpoint.cancelObservation(observation.getId());
        registrationStore.removeObservation(observation.getRegistrationId(), observation.getId());

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

        return new HashSet<Observation>(registrationStore.getObservations(registrationId));
    }

    private Set<Observation> getObservations(String registrationId, String resourcePath) {
        if (registrationId == null || resourcePath == null)
            return Collections.emptySet();

        Set<Observation> result = new HashSet<Observation>();
        LwM2mPath lwPath = new LwM2mPath(resourcePath);
        for (Observation obs : getObservations(registrationId)) {
            if (lwPath.equals(obs.getPath())) {
                result.add(obs);
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

        if (coapResponse.getCode() == CoAP.ResponseCode.CHANGED
                || coapResponse.getCode() == CoAP.ResponseCode.CONTENT) {
            try {
                // get registration Id
                String regid = coapRequest.getUserContext().get(CTX_REGID);
                
                // get observation for this request
                Observation observation = registrationStore.getObservation(regid, coapResponse.getToken());
                if (observation == null)
                    return;

                // get registration
                Registration registration = registrationStore.getRegistration(observation.getRegistrationId());
                if (registration == null)
                    // TODO Should we clean registrationIDs maps ?
                    return;

                // get model for this registration
                LwM2mModel model = modelProvider.getObjectModel(registration);

                // get content format
                ContentFormat contentFormat = null;
                if (coapResponse.getOptions().hasContentFormat()) {
                    contentFormat = ContentFormat.fromCode(coapResponse.getOptions().getContentFormat());
                }

                // decode response
                List<TimestampedLwM2mNode> timestampedNodes = decoder.decodeTimestampedData(coapResponse.getPayload(),
                        contentFormat, observation.getPath(), model);

                // create lwm2m response
                ObserveResponse response;
                if (timestampedNodes.size() == 1 && !timestampedNodes.get(0).isTimestamped()) {
                    response = new ObserveResponse(ResponseCode.CONTENT, timestampedNodes.get(0).getNode(), null,
                            observation, null, coapResponse);
                } else {
                    response = new ObserveResponse(ResponseCode.CONTENT, null, timestampedNodes, observation, null,
                            coapResponse);
                }

                // notify all listeners
                for (ObservationListener listener : listeners) {
                    listener.newValue(observation, response);
                }
            } catch (InvalidValueException e) {
                LOG.debug(String.format("[%s] ([%s])", e.getMessage(), e.getPath().toString()));
            }
        }
    }
}
