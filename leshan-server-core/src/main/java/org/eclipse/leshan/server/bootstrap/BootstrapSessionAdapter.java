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
package org.eclipse.leshan.server.bootstrap;

import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.LwM2mResponse;

public class BootstrapSessionAdapter implements BootstrapSessionListener {

    @Override
    public void sessionInitiated(BootstrapRequest request, Identity clientIdentity) {
    }

    @Override
    public void unAuthorized(BootstrapRequest request, Identity clientIdentity) {
    }

    @Override
    public void authorized(BootstrapSession session) {
    }

    @Override
    public void noConfig(BootstrapSession session) {
    }

    @Override
    public void sendRequest(BootstrapSession session, BootstrapDownlinkRequest<? extends LwM2mResponse> request) {
    }

    @Override
    public void onResponseSuccess(BootstrapSession session, BootstrapDownlinkRequest<? extends LwM2mResponse> request,
            LwM2mResponse response) {
    }

    @Override
    public void onResponseError(BootstrapSession session, BootstrapDownlinkRequest<? extends LwM2mResponse> request,
            LwM2mResponse response) {
    }

    @Override
    public void onRequestFailure(BootstrapSession session, BootstrapDownlinkRequest<? extends LwM2mResponse> request,
            Throwable cause) {
    }

    @Override
    public void end(BootstrapSession session) {
    }

    @Override
    public void failed(BootstrapSession session, BootstrapFailureCause cause) {
    }
}
