/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.core.californium.oscore.cf;

/**
 * A store holding OSCORE pre-established inputs which will be used to derive security context.
 * <p>
 * See : https://datatracker.ietf.org/doc/html/rfc8613#section-3.2
 *
 */
// TODO OSCORE this should be moved in californium.
// TODO OSCORE don't know if we want an async API or a more simple sync one.
// Let's start with a sync API for now.
public interface OscoreStore {

    /**
     * @return {@link OscoreParameters} for the given recipientID.
     */
    OscoreParameters getOscoreParameters(byte[] recipientID);

    /**
     * @return the recipientID for the given foreign peer URI.
     */
    byte[] getRecipientId(String foreignPeerURI);
}
