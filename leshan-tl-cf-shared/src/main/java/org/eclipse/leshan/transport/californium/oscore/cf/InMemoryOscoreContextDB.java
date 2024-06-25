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

import org.eclipse.californium.oscore.CoapOSException;
import org.eclipse.californium.oscore.HashMapCtxDB;
import org.eclipse.californium.oscore.OSCoreCtx;
import org.eclipse.californium.oscore.OSCoreCtxDB;
import org.eclipse.californium.oscore.OSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link OSCoreCtxDB} which store context in memory and is able to derive context from {@link OscoreParameters}
 * provided in {@link OscoreStore}
 *
 */
// TODO OSCORE this should be moved in californium.
public class InMemoryOscoreContextDB extends HashMapCtxDB {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryOscoreContextDB.class);

    private final OscoreStore store;

    public InMemoryOscoreContextDB(OscoreStore oscoreStore) {
        this.store = oscoreStore;
    }

    @Override
    public synchronized OSCoreCtx getContext(byte[] rid, byte[] IDContext) throws CoapOSException {
        // search in local DB
        OSCoreCtx osCoreCtx = super.getContext(rid, IDContext);

        if (osCoreCtx == null && IDContext != null) {
            throw new IllegalArgumentException("Internal Leshan operations should always use a null ID Context");
        }

        // if nothing found
        if (osCoreCtx == null) {
            // try to derive new context from OSCORE parameter in OSCORE Store
            OscoreParameters params = store.getOscoreParameters(rid);
            if (params != null) {
                osCoreCtx = deriveContext(params);
                // add new new context in local DB
                super.addContext(osCoreCtx);
            }
        }
        return osCoreCtx;
    }

    @Override
    public synchronized OSCoreCtx getContext(byte[] rid) {
        OSCoreCtx osCoreCtx = super.getContext(rid);

        // if nothing found
        if (osCoreCtx == null) {
            // try to derive new context from OSCORE parameter in OSCORE Store
            OscoreParameters params = store.getOscoreParameters(rid);
            if (params != null) {
                osCoreCtx = deriveContext(params);
                // add new new context in local DB
                super.addContext(osCoreCtx);
            }
        }
        return osCoreCtx;
    }

    @Override
    public synchronized OSCoreCtx getContext(String uri) throws OSException {
        OSCoreCtx osCoreCtx = super.getContext(uri);

        // if nothing found
        if (osCoreCtx == null) {
            // try to derive new context from OSCORE parameter in OSCORE Store
            byte[] rid = store.getRecipientId(uri);
            if (rid != null) {
                osCoreCtx = getContext(rid);
                // TODO OSCORE don't know if I should add by uri here ?
                // see : https://github.com/eclipse/leshan/pull/1212#discussion_r830937966
                // For now we don't add it because of :
                // https://github.com/eclipse/leshan/pull/1232#discussion_r841851488
                // super.addContext(uri, osCoreCtx);
            }
        }
        return osCoreCtx;
    }

    private static OSCoreCtx deriveContext(OscoreParameters oscoreParameters) {
        try {
            OSCoreCtx osCoreCtx = new OSCoreCtx(oscoreParameters.getMasterSecret(), true,
                    oscoreParameters.getAeadAlgorithm(), oscoreParameters.getSenderId(),
                    oscoreParameters.getRecipientId(), oscoreParameters.getHmacAlgorithm(), 32,
                    oscoreParameters.getMasterSalt(), null, 1000);
            osCoreCtx.setContextRederivationEnabled(true);
            return osCoreCtx;
        } catch (OSException e) {
            LOG.error("Unable to derive context from {}", oscoreParameters, e);
            return null;
        }
    }
}
