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

/**
 * An {@link OSCoreCtxDB} which store context in memory and is able to derive context from data provided in
 * {@link OscoreStore}
 *
 */
// TODO OSCORE this should be moved in californium.
// TODO OSCORE I don't know if we need to extends HashMapCtxDB or implement OSCoreCtxDB
public class InMemoryOscoreContextDB extends HashMapCtxDB {

    private OscoreStore store;

    public InMemoryOscoreContextDB(OscoreStore oscoreStore) {
        this.store = oscoreStore;
    }

    @Override
    public synchronized OSCoreCtx getContext(byte[] rid, byte[] IDContext) throws CoapOSException {
        OSCoreCtx osCoreCtx = super.getContext(rid, IDContext);

        if (osCoreCtx == null && IDContext != null) {
            throw new IllegalArgumentException("Internal Leshan operations should always use a null ID Context");
        }

        if (osCoreCtx == null) {
            OscoreParameters params = store.getOscoreParameters(rid);
            osCoreCtx = deriveContext(params);
            super.addContext(osCoreCtx);
        }

        return osCoreCtx;
    }

    @Override
    public synchronized OSCoreCtx getContext(byte[] rid) {
        OSCoreCtx osCoreCtx = super.getContext(rid);

        if (osCoreCtx == null) {
            OscoreParameters params = store.getOscoreParameters(rid);
            osCoreCtx = deriveContext(params);
            super.addContext(osCoreCtx);
        }

        return osCoreCtx;

    }

    @Override
    public synchronized OSCoreCtx getContext(String uri) throws OSException {
        System.out.println("=======================" + uri);

        OSCoreCtx osCoreCtx = super.getContext(uri);

        if (osCoreCtx == null) {
            OscoreParameters params = store.getOscoreParameters(uri);
            osCoreCtx = deriveContext(params);
            super.addContext(uri, osCoreCtx);
        }

        return osCoreCtx;
    }

    private static OSCoreCtx deriveContext(OscoreParameters oscoreSetting) {
        try {
            OSCoreCtx osCoreCtx = new OSCoreCtx(oscoreSetting.getMasterSecret(), true, oscoreSetting.getAeadAlgorithm(),
                    oscoreSetting.getSenderId(), oscoreSetting.getRecipientId(), oscoreSetting.getHmacAlgorithm(), 32,
                    oscoreSetting.getMasterSalt(), null, 1000);
            osCoreCtx.setContextRederivationEnabled(true);
            return osCoreCtx;
        } catch (OSException e) {
            throw new IllegalStateException("Unable to create OSCoreContext", e);
        }
    }
}
