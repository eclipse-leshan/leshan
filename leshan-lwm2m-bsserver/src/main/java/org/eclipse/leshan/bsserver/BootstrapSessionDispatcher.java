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
package org.eclipse.leshan.bsserver;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.DownlinkBootstrapRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;

public class BootstrapSessionDispatcher implements BootstrapSessionListener {

    private final List<BootstrapSessionListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(BootstrapSessionListener listener) {
        listeners.add(listener);
    }

    public void removeListener(BootstrapSessionListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void sessionInitiated(BootstrapRequest request, LwM2mPeer client) {
        for (BootstrapSessionListener listener : listeners) {
            listener.sessionInitiated(request, client);
        }
    }

    @Override
    public void unAuthorized(BootstrapRequest request, LwM2mPeer client) {
        for (BootstrapSessionListener listener : listeners) {
            listener.unAuthorized(request, client);
        }
    }

    @Override
    public void authorized(BootstrapSession session) {
        for (BootstrapSessionListener listener : listeners) {
            listener.authorized(session);
        }
    }

    @Override
    public void noConfig(BootstrapSession session) {
        for (BootstrapSessionListener listener : listeners) {
            listener.noConfig(session);
        }
    }

    @Override
    public void sendRequest(BootstrapSession session, DownlinkBootstrapRequest<? extends LwM2mResponse> request) {
        for (BootstrapSessionListener listener : listeners) {
            listener.sendRequest(session, request);
        }
    }

    @Override
    public void onResponseSuccess(BootstrapSession session, DownlinkBootstrapRequest<? extends LwM2mResponse> request,
            LwM2mResponse response) {
        for (BootstrapSessionListener listener : listeners) {
            listener.onResponseSuccess(session, request, response);
        }
    }

    @Override
    public void onResponseError(BootstrapSession session, DownlinkBootstrapRequest<? extends LwM2mResponse> request,
            LwM2mResponse response) {
        for (BootstrapSessionListener listener : listeners) {
            listener.onResponseError(session, request, response);
        }
    }

    @Override
    public void onRequestFailure(BootstrapSession session, DownlinkBootstrapRequest<? extends LwM2mResponse> request,
            Throwable cause) {
        for (BootstrapSessionListener listener : listeners) {
            listener.onRequestFailure(session, request, cause);
        }
    }

    @Override
    public void end(BootstrapSession session) {
        for (BootstrapSessionListener listener : listeners) {
            listener.end(session);
        }
    }

    @Override
    public void failed(BootstrapSession session, BootstrapFailureCause cause) {
        for (BootstrapSessionListener listener : listeners) {
            listener.failed(session, cause);
        }
    }
}
