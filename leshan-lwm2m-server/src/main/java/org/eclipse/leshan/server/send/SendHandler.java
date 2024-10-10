/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.server.send;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.SendResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.registration.UpdatedRegistration;
import org.eclipse.leshan.server.security.Authorizer;
import org.eclipse.leshan.servers.security.Authorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class responsible to handle "Send" request from LWM2M client.
 *
 * @see SendRequest
 */
public class SendHandler implements SendService {

    private final Logger LOG = LoggerFactory.getLogger(SendHandler.class);

    private final RegistrationStore registrationStore;
    private final Authorizer authorizer;
    private final boolean updateRegistrationOnSend;

    private final List<SendListener> listeners = new CopyOnWriteArrayList<>();;

    public SendHandler(RegistrationStore registrationStore, Authorizer authorizer, boolean updateRegistrationOnSend) {
        this.registrationStore = registrationStore;
        this.authorizer = authorizer;
        this.updateRegistrationOnSend = updateRegistrationOnSend;
    }

    @Override
    public void addListener(SendListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(SendListener listener) {
        listeners.remove(listener);
    }

    public SendableResponse<SendResponse> handleSend(LwM2mPeer sender, Registration registration,
            final SendRequest request, EndpointUri serverEndpointUri) {

        // Try to update registration if needed
        final Registration updatedRegistration;
        try {
            updatedRegistration = updateRegistration(sender, registration, request, serverEndpointUri);
            if (updatedRegistration == null) {
                return errorReponse(updatedRegistration, ResponseCode.BAD_REQUEST, "not authorized", null);
            }
        } catch (Exception e) {
            return errorReponse(registration, ResponseCode.INTERNAL_SERVER_ERROR, "unable to update registration", e);
        }

        // Check if send request is allowed
        Authorization authorized = authorizer.isAuthorized(request, registration, sender, serverEndpointUri);
        if (authorized.isDeclined()) {
            return errorReponse(updatedRegistration, ResponseCode.BAD_REQUEST, "not authorized", null);
        }

        // Validate and create Send Response
        final SendResponse sendResponse = validateSendRequest(updatedRegistration, request);

        SendableResponse<SendResponse> response;
        if (sendResponse.isSuccess()) {
            response = new SendableResponse<>(sendResponse, new Runnable() {
                @Override
                public void run() {
                    if (sendResponse.isSuccess()) {
                        fireDataReceived(updatedRegistration, request.getTimestampedNodes(), request);
                    }
                }
            });
        } else {
            response = errorReponse(updatedRegistration, //
                    sendResponse.getCode(), //
                    String.format("Invalid Send Request, server returns %s %s", //
                            sendResponse.getCode().getName(), //
                            sendResponse.getErrorMessage() != null ? "because" + sendResponse.getErrorMessage() : ""), //
                    new InvalidRequestException(sendResponse.getErrorMessage() != null ? sendResponse.getErrorMessage()
                            : "unknown reason"));
        }
        return response;

    }

    /**
     * Update registration if needed and allowed by authorizer.
     *
     * Note that updating registration on send is out of specification and could be a problem for interoperability.
     *
     * @return the registration or the updated registration or null if not allowed to update.
     */
    protected Registration updateRegistration(LwM2mPeer sender, final Registration registration,
            final SendRequest request, EndpointUri endpointUri) {
        if (!updateRegistrationOnSend) {
            // mode is not activate so we don't update registration
            return registration;

        }

        // check if update is allowed
        // HACK we create and Update request
        // it can be identified because we pass the SEND request as under-layer object)
        UpdateRequest updateRequest = new UpdateRequest(registration.getId(), null, null, null, null, null, request);
        Authorization authorized = authorizer.isAuthorized(updateRequest, registration, sender, endpointUri);
        if (authorized.isDeclined()) {
            return null;
        }

        // update registration
        RegistrationUpdate regUpdate = new RegistrationUpdate(registration.getId(), sender, null, null, null, null,
                null, null, null, null, null, null);
        UpdatedRegistration updatedRegistration = registrationStore.updateRegistration(regUpdate);
        if (updatedRegistration == null || updatedRegistration.getUpdatedRegistration() == null) {
            String errorMsg = String.format(
                    "Unexpected error when receiving Send Request: There is no registration with id %s",
                    registration.getId());
            LOG.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        return updatedRegistration.getUpdatedRegistration();
    }

    protected void fireDataReceived(Registration registration, TimestampedLwM2mNodes data, SendRequest request) {
        for (SendListener listener : listeners) {
            listener.dataReceived(registration, data, request);
        }
    }

    public void onError(Registration registration, String errorMessage, Exception error) {
        for (SendListener listener : listeners) {
            listener.onError(registration, errorMessage, error);
        }
    }

    protected SendResponse validateSendRequest(Registration registration, SendRequest request) {
        // check if all data of Send request are registered by LwM2M device.
        // see : https://github.com/eclipse-leshan/leshan/issues/1472
        TimestampedLwM2mNodes timestampedNodes = request.getTimestampedNodes();
        Map<LwM2mPath, LwM2mNode> nodes = timestampedNodes.getFlattenNodes();
        for (LwM2mPath path : nodes.keySet()) {
            if (path.isRoot()) {
                continue;
            } else if (path.isObject()) {
                if (registration.getSupportedVersion(path.getObjectId()) == null) {
                    return SendResponse.notFound(String.format("object %s not registered", path));
                }
            } else {
                LwM2mPath instancePath = path.toObjectInstancePath();
                if (!registration.getAvailableInstances().contains(instancePath)) {
                    return SendResponse.notFound(String.format("object instance %s not registered", instancePath));
                }
            }
        }
        return SendResponse.success();
    }

    protected SendableResponse<SendResponse> errorReponse(Registration registration, ResponseCode code,
            String errorMessage, Exception e) {

        SendableResponse<SendResponse> response = new SendableResponse<>(new SendResponse(code, errorMessage),
                new Runnable() {
                    @Override
                    public void run() {
                        onError(registration, errorMessage, e);
                    }
                });
        return response;
    }
}
