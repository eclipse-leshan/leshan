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
package org.eclipse.leshan.server.californium.bootstrap;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.californium.cose.AlgorithmID;
import org.eclipse.californium.cose.CoseException;
import org.eclipse.leshan.core.californium.oscore.cf.OscoreParameters;
import org.eclipse.leshan.core.californium.oscore.cf.OscoreStore;
import org.eclipse.leshan.core.oscore.OscoreIdentity;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.upokecenter.cbor.CBORObject;

/**
 * An {@link OscoreStore} which search {@link OscoreParameters} in LWM2M {@link SecurityStore}
 */
public class LwM2mBootstrapOscoreStore implements OscoreStore {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mBootstrapOscoreStore.class);

    private final BootstrapSecurityStore securityStore;
    private final OscoreBootstrapListener sessionHolder;

    public LwM2mBootstrapOscoreStore(BootstrapSecurityStore securityStore, OscoreBootstrapListener sessionHolder) {
        Validate.notNull(securityStore);
        Validate.notNull(sessionHolder);
        this.securityStore = securityStore;
        this.sessionHolder = sessionHolder;
    }

    @Override
    public OscoreParameters getOscoreParameters(byte[] recipientID) {
        OscoreIdentity oscoreIdentity = new OscoreIdentity(recipientID);
        SecurityInfo securityInfo = securityStore.getByOscoreIdentity(oscoreIdentity);
        if (securityInfo == null || !securityInfo.useOSCORE())
            return null;

        try {
            return new OscoreParameters(//
                    securityInfo.getOscoreSetting().getSenderId(), //
                    securityInfo.getOscoreSetting().getRecipientId(), //
                    securityInfo.getOscoreSetting().getMasterSecret(), //
                    // TODO OSCORE we maybe need an API without the need to create a CBOR Object
                    AlgorithmID.FromCBOR(
                            CBORObject.FromObject(securityInfo.getOscoreSetting().getAeadAlgorithm().getValue())), //
                    AlgorithmID.FromCBOR(
                            CBORObject.FromObject(securityInfo.getOscoreSetting().getHkdfAlgorithm().getValue())), //
                    // TODO OSCORE kind of hack because californium doesn't support an empty byte[] array for salt ?
                    securityInfo.getOscoreSetting().getMasterSalt().length == 0 ? null
                            : securityInfo.getOscoreSetting().getMasterSalt());
        } catch (CoseException e) {
            LOG.error("Unable to create OscoreParameters from OoscoreSetting %s", securityInfo.getOscoreSetting(), e);
            return null;
        }
    }

    @Override
    public byte[] getRecipientId(String uri) {
        try {
            URI foreignPeerUri = new URI(uri);
            InetSocketAddress foreignPeerAddress = new InetSocketAddress(foreignPeerUri.getHost(),
                    foreignPeerUri.getPort());
            BootstrapSession session = sessionHolder.getSessionByAddr(foreignPeerAddress);
            Identity identity = session.getIdentity();
            if (identity.isOSCORE()) {
                return identity.getOscoreIdentity().getRecipientId();
            }
        } catch (URISyntaxException | SecurityException | IllegalArgumentException e) {
            LOG.error("Unable to extract InetScocketAddress from uri %s", uri, e);
            return null;
        }
        return null;
    }

}
