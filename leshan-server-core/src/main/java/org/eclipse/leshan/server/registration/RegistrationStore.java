/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
package org.eclipse.leshan.server.registration;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.ObservationIdentifier;
import org.eclipse.leshan.core.request.Identity;

/**
 * A store for registrations and observations. This interface is also responsible to handle registration expiration.
 */
public interface RegistrationStore {

    /**
     * Store a new registration.
     *
     * If a registration already exists with the given endpoint, the store is in charge of removing this registration as
     * well as the ongoing observations.
     *
     * @param registration the new registration.
     * @return the old registration and its observations or <code>null</code> if it does not already exists.
     */
    Deregistration addRegistration(Registration registration);

    /**
     * Update an existing registration
     *
     * @param update data to update
     * @return return the previous and updated registration
     */
    UpdatedRegistration updateRegistration(RegistrationUpdate update);

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
     * Get the registration by socket address. If there are 2 Registrations linked to the same address, the most recent
     * one should be returned. Generally this happened when devices are behind NAT and so address could be reused.
     *
     * @param address of the client registered.
     * @return the registration or null if there is no client registered with this socket address.
     */
    Registration getRegistrationByAdress(InetSocketAddress address);

    /**
     * Get the registration by {@link Identity}.
     *
     * @param identity of the client registered.
     * @return the registration or null if there is no client registered with this identity.
     */
    Registration getRegistrationByIdentity(Identity identity);

    /**
     * Returns an iterator over the registration of this store. There are no guarantees concerning the order in which
     * the elements are returned (unless the implementation provides a guarantee).
     *
     * @return an <tt>Iterator</tt> over the registration in this store
     */
    Iterator<Registration> getAllRegistrations();

    /**
     * Remove the registration with the given registration Id
     *
     * @param registrationId the id of the registration to removed
     * @return the registration and observations removed or null if there is no registration for this Id.
     */
    Deregistration removeRegistration(String registrationId);

    /**
     * Add a new {@link Observation} for a given registration.
     *
     * The store is in charge of removing the observations already existing for the same path and registration id.
     *
     * @param registrationId the id of the registration
     * @param observation the observation to add
     *
     * @return the list of removed observations or an empty list if none were removed.
     */
    Collection<Observation> addObservation(String registrationId, Observation observation, boolean addIfAbsent);

    /**
     * Get the observation for the given registration with the given observationId
     */
    Observation getObservation(String registrationId, ObservationIdentifier observationId);

    /**
     * Remove the observation for the given registration with the given observationId
     */
    Observation removeObservation(String registrationId, ObservationIdentifier observationId);

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
