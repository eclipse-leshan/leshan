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
package org.eclipse.leshan.client.observer;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.client.servers.DmServerInfo;
import org.eclipse.leshan.client.servers.ServerInfo;

/**
 * An abstract adapter class for observing registration life cycle. The methods in this
 * class are empty. This class exists as convenience for creating client
 * observer objects.
 */
public class LwM2mClientObserverAdapter implements LwM2mClientObserver {

    @Override
    public void onBootstrapSuccess(ServerInfo bsserver) {
    }

    @Override
    public void onBootstrapFailure(ServerInfo bsserver, ResponseCode responseCode, String errorMessage) {
    }

    @Override
    public void onBootstrapTimeout(ServerInfo bsserver) {
    }

    @Override
    public void onRegistrationSuccess(DmServerInfo server, String registrationID) {
    }

    @Override
    public void onRegistrationFailure(DmServerInfo server, ResponseCode responseCode, String errorMessage) {
    }

    @Override
    public void onRegistrationTimeout(DmServerInfo server) {
    }

    @Override
    public void onUpdateSuccess(DmServerInfo server, String registrationID) {
    }

    @Override
    public void onUpdateFailure(DmServerInfo server, ResponseCode responseCode, String errorMessage) {
    }

    @Override
    public void onUpdateTimeout(DmServerInfo server) {
    }

    @Override
    public void onDeregistrationSuccess(DmServerInfo server, String registrationID) {
    }

    @Override
    public void onDeregistrationFailure(DmServerInfo server, ResponseCode responseCode, String errorMessage) {
    }

    @Override
    public void onDeregistrationTimeout(DmServerInfo server) {
    }
}
