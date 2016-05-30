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
 * Allow to observer the registration life cycle of a LwM2m client.
 */
public interface LwM2mClientObserver {

    void onBootstrapSuccess(ServerInfo bsserver);

    void onBootstrapFailure(ServerInfo bsserver, ResponseCode responseCode, String errorMessage);

    void onBootstrapTimeout(ServerInfo bsserver);

    void onRegistrationSuccess(DmServerInfo server, String registrationID);

    void onRegistrationFailure(DmServerInfo server, ResponseCode responseCode, String errorMessage);

    void onRegistrationTimeout(DmServerInfo server);

    void onUpdateSuccess(DmServerInfo server, String registrationID);

    void onUpdateFailure(DmServerInfo server, ResponseCode responseCode, String errorMessage);

    void onUpdateTimeout(DmServerInfo server);

    void onDeregistrationSuccess(DmServerInfo server, String registrationID);

    void onDeregistrationFailure(DmServerInfo server, ResponseCode responseCode, String errorMessage);

    void onDeregistrationTimeout(DmServerInfo server);
}
