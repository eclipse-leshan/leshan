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
package org.eclipse.leshan.server.registration;

import java.net.InetSocketAddress;
import java.util.Collection;

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientUpdate;

public interface RegistrationStore {

    Client addRegistration(Client registration);

    Client updateRegistration(ClientUpdate update);

    Client getRegistration(String registrationId);

    Client getRegistrationByEndpoint(String endpoint);

    Collection<Client> getRegistrationByAdress(InetSocketAddress address);

    // TODO should be removed
    Collection<Client> getAllRegistration();

    Client removeRegistration(String registrationId);

    Observation addObservation(String registrationId, Observation observation);

    Observation getObservation(String registrationId, byte[] observationId);

    Observation removeObservation(String registrationId, byte[] observationId);

    Collection<Observation> getObservations(String registrationId);

    Collection<Observation> removeObservations(String registrationId);

    void setExpirationListener(ExpirationListener listener);
}
