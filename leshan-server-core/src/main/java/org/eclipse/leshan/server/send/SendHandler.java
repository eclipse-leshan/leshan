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

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.SendResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.registration.UpdatedRegistration;
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
    private final boolean updateRegistrationOnSend;

    private final List<SendListener> listeners = new CopyOnWriteArrayList<>();;

    public SendHandler(RegistrationStore registrationStore, boolean updateRegistrationOnSend) {
        this.registrationStore = registrationStore;
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
            final SendRequest request) {

        // try to update registration if needed
        final Registration updatedRegistration;
        try {
            updatedRegistration = updateRegistration(sender, registration);
        } catch (Exception e) {
            String errMsg = "unable to update registration";
            SendableResponse<SendResponse> response = new SendableResponse<>(SendResponse.internalServerError(errMsg),
                    new Runnable() {
                        @Override
                        public void run() {
                            onError(registration, errMsg, e);
                        }
                    });
            return response;
        }

        // Send Response to send request on success
        final SendResponse sendResponse = validateSendRequest(updatedRegistration, request);
        SendableResponse<SendResponse> response = new SendableResponse<>(sendResponse, new Runnable() {

            @Override
            public void run() {
                if (sendResponse.isSuccess()) {
                    fireDataReceived(updatedRegistration, request.getTimestampedNodes(), request);
                } else {
                    onError(updatedRegistration, String.format("Invalid Send Request, server returns %s %s", //
                            sendResponse.getCode().getName(), //
                            sendResponse.getErrorMessage() != null ? "because" + sendResponse.getErrorMessage() : ""),
                            new InvalidRequestException(
                                    sendResponse.getErrorMessage() != null ? sendResponse.getErrorMessage()
                                            : "unknown reason"));
                }
            }

        });
        return response;

    }

    private Registration updateRegistration(LwM2mPeer sender, final Registration registration) {
        if (updateRegistrationOnSend) {
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
        return registration;
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
        Map<LwM2mPath, LwM2mNode> nodes = timestampedNodes.getNodes();
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
}
