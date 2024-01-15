/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.javacoap.server.observation;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.ObservationIdentifier;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.response.AbstractLwM2mResponse;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.observation.LwM2mNotificationReceiver;
import org.eclipse.leshan.server.profile.ClientProfile;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.transport.javacoap.request.ResponseCodeUtil;

import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Opaque;
import com.mbed.coap.packet.SeparateResponse;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.observe.NotificationsReceiver;

public class CoapNotificationReceiver implements NotificationsReceiver {

    private final CoapServer coapServer;
    private final LwM2mNotificationReceiver notificationReceiver;
    private final RegistrationStore registrationStore;
    private final LwM2mModelProvider modelProvider;
    private final LwM2mDecoder decoder;

    public CoapNotificationReceiver(CoapServer coapServer, LwM2mNotificationReceiver notificationReceiver,
            RegistrationStore registrationStore, LwM2mModelProvider modelProvider, LwM2mDecoder decoder) {
        super();
        this.coapServer = coapServer;
        this.notificationReceiver = notificationReceiver;
        this.registrationStore = registrationStore;
        this.modelProvider = modelProvider;
        this.decoder = decoder;
    }

    @Override
    public boolean onObservation(String resourceUriPath, SeparateResponse coapResponse) {
        // Get foreign peer data from separated response
        InetSocketAddress peerAddress = coapResponse.getPeerAddress();
        IpPeer sender = new IpPeer(peerAddress);

        // Search if there is an observation for this resource.
        ObservationIdentifier observationId = new ObservationIdentifier(coapResponse.getToken().getBytes());
        final Observation observation = registrationStore.getObservation(observationId);
        if (observation == null)
            return false;

        // Check if path is the right one.
        Optional<String> observationPath = ObservationUtil.getPath(observation); //
        if (!observationPath.filter(p -> p.equals(resourceUriPath)).isPresent()) {
            throw new IllegalStateException(String.format("Observation path %s does not match reponse path %s ",
                    observationPath.orElse("null"), resourceUriPath));
        }

        // In case of block transfer, call to retrieve rest of payload.
        CompletableFuture<Opaque> payload = NotificationsReceiver.retrieveRemainingBlocks(resourceUriPath, coapResponse,
                req -> coapServer.clientService().apply(req));

        // Handle CoAP Notification
        payload.whenComplete((p, e) -> {
            // Check we have a corresponding registration
            Registration registration = registrationStore.getRegistration(observation.getRegistrationId());
            if (registration == null) {
                throw new IllegalStateException(
                        String.format("No registration with Id %s", observation.getRegistrationId()));
            }

            // Create Client Profile
            ClientProfile clientProfile = new ClientProfile(registration, modelProvider.getObjectModel(registration));
            try {
                // Send events
                if (e != null) {
                    // on Error
                    // TODO should we stop observe relation ?
                    notificationReceiver.onError(observation, sender, clientProfile,
                            e instanceof Exception ? (Exception) e : new Exception(e));
                } else if (p != null) {
                    AbstractLwM2mResponse observeResponse = createLwM2mResponseForNotification(observation,
                            coapResponse.asResponse(), clientProfile);
                    if (observation instanceof SingleObservation) {

                        // Single Observe Notification
                        notificationReceiver.onNotification((SingleObservation) observation, sender, clientProfile,
                                (ObserveResponse) observeResponse);
                    } else if (observation instanceof CompositeObservation) {

                        // Composite Observe Notification
                        notificationReceiver.onNotification((CompositeObservation) observation, sender, clientProfile,
                                (ObserveCompositeResponse) observeResponse);
                    } else {
                        throw new IllegalStateException(String.format("Unexpected observation :  %s is not supported",
                                observation.getClass().getSimpleName()));
                    }
                } else {
                    throw new IllegalStateException("unexpected behavior when handling notification");
                }
            } catch (CodecException exception) {
                // TODO should we stop observe relation ?
                notificationReceiver.onError(observation, sender, clientProfile, new InvalidResponseException(exception,
                        "Unable to decode notification payload  of observation %s", observation));
            } catch (Exception exception) {
                // TODO should we stop observe relation ?
                notificationReceiver.onError(observation, sender, clientProfile, exception);
            }
        });

        return true;
    }

    public AbstractLwM2mResponse createLwM2mResponseForNotification(Observation observation, CoapResponse coapResponse,
            ClientProfile profile) {

        ResponseCode responseCode = ResponseCodeUtil.toLwM2mResponseCode(coapResponse.getCode());

        if (observation instanceof SingleObservation) {
            SingleObservation singleObservation = (SingleObservation) observation;

            ContentFormat contentFormat = ContentFormat.fromCode(coapResponse.options().getContentFormat());
            List<TimestampedLwM2mNode> timestampedNodes = decoder.decodeTimestampedData(
                    coapResponse.getPayload().getBytes(), contentFormat, singleObservation.getPath(),
                    profile.getModel());

            // create lwm2m response
            if (timestampedNodes.size() == 1 && !timestampedNodes.get(0).isTimestamped()) {
                return new ObserveResponse(responseCode, timestampedNodes.get(0).getNode(), null, singleObservation,
                        null, coapResponse);
            } else {
                return new ObserveResponse(responseCode, null, timestampedNodes, singleObservation, null, coapResponse);
            }
        } else if (observation instanceof CompositeObservation) {
            CompositeObservation compositeObservation = (CompositeObservation) observation;

            ContentFormat contentFormat = ContentFormat.fromCode(coapResponse.options().getContentFormat());
            TimestampedLwM2mNodes timestampedNodes = decoder.decodeTimestampedNodes(
                    coapResponse.getPayload().getBytes(), contentFormat, compositeObservation.getPaths(),
                    profile.getModel());
            if (timestampedNodes != null && !timestampedNodes.isEmpty()
                    && !timestampedNodes.getTimestamps().stream().noneMatch(Objects::nonNull)) {

                return new ObserveCompositeResponse(responseCode, timestampedNodes.getNodes(), timestampedNodes,
                        compositeObservation, null, coapResponse);
            } else {
                Map<LwM2mPath, LwM2mNode> nodes = decoder.decodeNodes(coapResponse.getPayload().getBytes(),
                        contentFormat, compositeObservation.getPaths(), profile.getModel());
                return new ObserveCompositeResponse(responseCode, nodes, null, compositeObservation, null,
                        coapResponse);
            }
        }
        return null;
    }

}
