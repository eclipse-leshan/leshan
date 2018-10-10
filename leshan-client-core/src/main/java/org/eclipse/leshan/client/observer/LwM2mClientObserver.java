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
import org.eclipse.leshan.client.servers.Server;

/**
 * Allow to observer the registration life cycle of a LwM2m client.
 */
public interface LwM2mClientObserver {

    void onBootstrapSuccess(Server bsserver);

    void onBootstrapFailure(Server bsserver, ResponseCode responseCode, String errorMessage);

    void onBootstrapTimeout(Server bsserver);

    void onRegistrationSuccess(Server server, String registrationID);

    void onRegistrationFailure(Server server, ResponseCode responseCode, String errorMessage);

    void onRegistrationTimeout(Server server);

    void onUpdateSuccess(Server server, String registrationID);

    void onUpdateFailure(Server server, ResponseCode responseCode, String errorMessage);

    void onUpdateTimeout(Server server);

    void onDeregistrationSuccess(Server server, String registrationID);

    void onDeregistrationFailure(Server server, ResponseCode responseCode, String errorMessage);

    void onDeregistrationTimeout(Server server);
}
