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

import org.eclipse.californium.core.network.CoapEndpoint;

/**
 * A static store which old only one {@link OscoreParameters}.
 * <p>
 * Can be used when {@link CoapEndpoint} is used with only 1 foreign peer.
 *
 */
public class StaticOscoreStore implements OscoreStore {

    private OscoreParameters parameters;

    public StaticOscoreStore(OscoreParameters parameters) {
        this.parameters = parameters;
    }

    @Override
    public OscoreParameters getOscoreParameters(byte[] recipientID) {
        return parameters;
    }

    @Override
    public byte[] getRecipientId(String uri) {
        return parameters.getRecipientId();
    }
}
