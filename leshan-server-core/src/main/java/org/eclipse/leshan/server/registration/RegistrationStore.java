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
import org.eclipse.leshan.server.client.Registration;
import org.eclipse.leshan.server.client.RegistrationUpdate;

/**
 * A store for registrations and observations. This interface is also responsible to handle registration expiration.
 */
public interface RegistrationStore {

    /**
     * Add a new registration. If there is already a registration with the same endpoint removed it.
     * 
     * @param registration the new registration.
     * @return the old registration and its observations removed or null.
     */
    Deregistration addRegistration(Registration registration);

    /**
     * Update an existing registration
     * 
     * @param update data to update
     * @return the registration updated
     */
    Registration updateRegistration(RegistrationUpdate update);

    /**
     * Get the registration by registration Id.
     * 
     * @param registrationId of the registration.
     * @return the registration or null if there is no registration with this id.
     */
    Registration getRegistration(String registrationId);

    /**
     * Get the registration by endpoint.
     * 
     * @param endpoint of the registration.
     * @return the registration or null if there is no registration with this endpoint.
     */
    Registration getRegistrationByEndpoint(String endpoint);

    /**
     * Get the registration by socket address.
     * 
     * @param address of the client registered.
     * @return the registration or null if there is no client registered with this socket address.
     */
    Collection<Registration> getRegistrationByAdress(InetSocketAddress address);

    /**
     * @return all registrations in this store.
     * @Deprecated should be replaced by an iterator.
     */
    // TODO Should be replaced by an iterator.
    @Deprecated
    Collection<Registration> getAllRegistration();

    /**
     * Remove the registration with the given registration Id
     * 
     * @param registrationId the id of the registration to removed
     * @return the registration and observations removed or null if there is no registration for this Id.
     */
    Deregistration removeRegistration(String registrationId);

    /**
     * Add a new Observation for a given registration.
     * 
     * @param registrationId the id of the registration
     * @param observation the observation to add
     * 
     * @return the previous observation or null if any.
     */
    Observation addObservation(String registrationId, Observation observation);

    /**
     * Get the observation for the given registration with the given observationId
     */
    Observation getObservation(String registrationId, byte[] observationId);

    /**
     * Remove the observation for the given registration with the given observationId
     */
    Observation removeObservation(String registrationId, byte[] observationId);

    /**
     * Get all observations for the given registrationId
     */
    Collection<Observation> getObservations(String registrationId);

    /**
     * Remove all observations for the given registrationId
     */
    Collection<Observation> removeObservations(String registrationId);

    /**
     * set a listener for registration expiration.
     */
    void setExpirationListener(ExpirationListener listener);
}
