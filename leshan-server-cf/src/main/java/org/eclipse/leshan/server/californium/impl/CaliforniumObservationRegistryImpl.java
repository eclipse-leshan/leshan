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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.Exchange.KeyToken;
import org.eclipse.californium.core.observe.NotificationListener;
import org.eclipse.californium.core.observe.ObservationStore;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.codec.InvalidValueException;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.server.californium.CaliforniumObservationRegistry;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.observation.ObservationRegistry;
import org.eclipse.leshan.server.observation.ObservationRegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * InMemory implementation of the {@link CaliforniumObservationRegistry} and {@link ObservationRegistry}. When a new
 * observation is added or changed or canceled, the listeners registered are notified.
 */
public class CaliforniumObservationRegistryImpl
        implements CaliforniumObservationRegistry, ObservationRegistry, NotificationListener {

    private final Logger LOG = LoggerFactory.getLogger(CaliforniumObservationRegistry.class);

    private final ObservationStore observationStore;
    private final ClientRegistry clientRegistry;
    private final LwM2mModelProvider modelProvider;
    private final LwM2mNodeDecoder decoder;
    private Endpoint secureEndpoint;
    private Endpoint nonSecureEndpoint;

    private final List<ObservationRegistryListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<KeyToken, Observation> observationsByToken = new ConcurrentHashMap<>();

    /**
     * creates an instance of {@link CaliforniumObservationRegistryImpl}
     * 
     * @param store instance of californium's {@link ObservationStore}
     * @param clientRegistry instance of {@link ObservationRegistry}
     * @param modelProvider instance of {@link LwM2mModelProvider}
     * @param decoder instance of {@link LwM2mNodeDecoder}
     */
    public CaliforniumObservationRegistryImpl(ObservationStore store, ClientRegistry clientRegistry,
            LwM2mModelProvider modelProvider, LwM2mNodeDecoder decoder) {
        this.observationStore = store;
        this.modelProvider = modelProvider;
        this.clientRegistry = clientRegistry;
        this.decoder = decoder;
    }

    @Override
    public void addObservation(Observation observation) {
        // cancel any existing observations for the same path and registration id.
        Set<Observation> existingObservations = getObservations(observation.getRegistrationId(),
                observation.getPath().toString());
        for (Observation oldObservation : existingObservations) {
            cancelObservation(oldObservation);
        }

        // add the new observation
        observationsByToken.put(new KeyToken(observation.getId()), observation);

        for (ObservationRegistryListener listener : listeners) {
            listener.newObservation(observation);
        }
    }

    @Override
    public void setNonSecureEndpoint(Endpoint endpoint) {
        nonSecureEndpoint = endpoint;
    }

    @Override
    public void setSecureEndpoint(Endpoint endpoint) {
        secureEndpoint = endpoint;
    }

    @Override
    public int cancelObservations(Client client) {
        // check registration id
        String registrationId = client.getRegistrationId();
        if (registrationId == null)
            return 0;

        Set<Observation> observations = getObservations(registrationId);
        for (Observation observation : observations) {
            cancelObservation(observation);
        }
        return observations.size();
    }

    @Override
    public int cancelObservations(Client client, String resourcepath) {
        // check registration id
        String registrationId = client.getRegistrationId();
        if (registrationId == null || resourcepath == null || resourcepath.isEmpty())
            return 0;

        Set<Observation> observations = getObservations(registrationId, resourcepath);
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
        observationsByToken.remove(new KeyToken(observation.getId()));

        for (ObservationRegistryListener listener : listeners) {
            listener.cancelled(observation);
        }
    }

    @Override
    public Set<Observation> getObservations(Client client) {
        return getObservations(client.getRegistrationId());
    }

    private Set<Observation> getObservations(String registrationId) {
        if (registrationId == null)
            return Collections.emptySet();

        Set<Observation> result = new HashSet<>();
        for (Map.Entry<KeyToken, Observation> entry : observationsByToken.entrySet()) {
            Observation observation = entry.getValue();
            if (registrationId.equals(observation.getRegistrationId())) {
                result.add(observation);
            }
        }
        return result;
    }

    private Set<Observation> getObservations(String registrationId, String resourcePath) {
        if (registrationId == null || resourcePath == null)
            return Collections.emptySet();

        Set<Observation> result = new HashSet<>();
        for (Observation observation : observationsByToken.values()) {
            if (registrationId.equals(observation.getRegistrationId())
                    && new LwM2mPath(resourcePath).equals(observation.getPath())) {
                result.add(observation);
            }
        }
        return result;
    }

    @Override
    public ObservationStore getObservationStore() {
        return observationStore;
    }

    @Override
    public void addListener(ObservationRegistryListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ObservationRegistryListener listener) {
        listeners.remove(listener);
    }

    // ********** NotificationListener interface **********//

    // TODO duplicate code from org.eclipse.leshan.server.demo.cluster.RedisObservationRegistry
    @Override
    public void onNotification(Request coapRequest, Response coapResponse) {
        if (listeners.isEmpty())
            return;

        if (coapResponse.getCode() == CoAP.ResponseCode.CHANGED
                || coapResponse.getCode() == CoAP.ResponseCode.CONTENT) {
            try {
                // get observation for this request
                Observation observation = observationsByToken.get(new KeyToken(coapResponse.getToken()));
                if (observation == null)
                    return;

                // get client for this registration ID
                Client client = clientRegistry.findByRegistrationId(observation.getRegistrationId());
                if (client == null)
                    // TODO Should we clean registrationIDs maps ?
                    return;

                // get model for this client
                LwM2mModel model = modelProvider.getObjectModel(client);

                // get content format
                ContentFormat contentFormat = null;
                if (coapResponse.getOptions().hasContentFormat()) {
                    contentFormat = ContentFormat.fromCode(coapResponse.getOptions().getContentFormat());
                }

                // decode response
                List<TimestampedLwM2mNode> content = decoder.decodeTimestampedData(coapResponse.getPayload(),
                        contentFormat, observation.getPath(), model);

                // notify all listeners
                for (ObservationRegistryListener listener : listeners) {
                    if (content.isEmpty()) {
                        listener.newValue(observation, null, content);
                    } else if (content.size() == 1 && content.get(0).isTimespamped()) {
                        listener.newValue(observation, content.get(0).getNode(), null);
                    } else {
                        listener.newValue(observation, content.get(0).getNode(), content);
                    }
                }
            } catch (InvalidValueException e) {
                String msg = String.format("[%s] ([%s])", e.getMessage(), e.getPath().toString());
                LOG.debug(msg);
            }
        }
    }
}
