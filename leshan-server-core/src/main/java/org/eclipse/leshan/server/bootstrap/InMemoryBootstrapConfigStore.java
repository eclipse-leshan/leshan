/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Rikard Höglund (RISE) - additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.server.bootstrap;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.californium.cose.AlgorithmID;
import org.eclipse.californium.cose.CoseException;
import org.eclipse.californium.oscore.HashMapCtxDB;
import org.eclipse.californium.oscore.OSCoreCtx;
import org.eclipse.californium.oscore.OSException;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.OscoreHandler;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.upokecenter.cbor.CBORObject;

/**
 * Simple bootstrap store implementation storing bootstrap configuration information in memory.
 * 
 * * @deprecated use {@link InMemoryBootstrapConfigurationStore} instead or see *
 * {@link BootstrapConfigurationStoreAdapter} or
 * {@link BootstrapUtil#toRequests(BootstrapConfig, org.eclipse.leshan.core.request.ContentFormat)}
 */
@Deprecated
public class InMemoryBootstrapConfigStore implements EditableBootstrapConfigStore {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryBootstrapConfigStore.class);

    protected final ConfigurationChecker configChecker = new ConfigurationChecker();

    protected final Map<String /* endpoint */, BootstrapConfig> bootstrapByEndpoint = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<PskByServer, BootstrapConfig> bootstrapByPskId = new ConcurrentHashMap<>();

    @Override
    public BootstrapConfig get(String endpoint, Identity deviceIdentity, BootstrapSession session) {
        return bootstrapByEndpoint.get(endpoint);
    }

    @Override
    public synchronized void add(String endpoint, BootstrapConfig config) throws InvalidConfigurationException {
        checkConfig(endpoint, config);

        // Check PSK identity uniqueness for bootstrap server:
        PskByServer pskToAdd = getBootstrapPskIdentity(config);
        if (pskToAdd != null) {
            BootstrapConfig existingConfig = bootstrapByPskId.get(pskToAdd);
            if (existingConfig != null) {
                // check if this config will be replace by the new one.
                BootstrapConfig previousConfig = bootstrapByEndpoint.get(endpoint);
                if (previousConfig != existingConfig) {
                    throw new InvalidConfigurationException(
                            "Psk identity [%s] already used for this bootstrap server [%s]", pskToAdd.identity,
                            pskToAdd.serverUrl);
                }
            }
        }
        // TODO we should probably also check lwm2m server

        bootstrapByEndpoint.put(endpoint, config);
        if (pskToAdd != null) {
            bootstrapByPskId.put(pskToAdd, config);
        }
        addOscoreContext(config);
    }

    protected void checkConfig(String endpoint, BootstrapConfig config) throws InvalidConfigurationException {
        configChecker.verify(config);
    }

    @Override
    public synchronized BootstrapConfig remove(String enpoint) {
        BootstrapConfig bootstrapConfig = bootstrapByEndpoint.remove(enpoint);
        if (bootstrapConfig != null) {
            PskByServer pskIdentity = getBootstrapPskIdentity(bootstrapConfig);
            if (pskIdentity != null) {
                bootstrapByPskId.remove(pskIdentity, bootstrapConfig);
            }
        }
        return bootstrapConfig;
    }

    protected PskByServer getBootstrapPskIdentity(BootstrapConfig config) {
        for (ServerSecurity security : config.security.values()) {
            if (security.bootstrapServer) {
                if (security.securityMode == SecurityMode.PSK) {
                    return new PskByServer(security.uri, new String(security.publicKeyOrId));
                }
            }
        }
        return null;
    }

    @Override
    public Map<String, BootstrapConfig> getAll() {
        return Collections.unmodifiableMap(bootstrapByEndpoint);
    }

    // If an OSCORE configuration came, add it to the context db
    // TODO this should be done via a kind of OSCORE Store
    public void addOscoreContext(BootstrapConfig config) {
        HashMapCtxDB db = OscoreHandler.getContextDB();
        for (ServerSecurity security : config.security.values()) {
            // Make sure to only add OSCORE context for the BS-Client connection
            BootstrapConfig.OscoreObject osc = config.oscore.get(security.oscoreSecurityMode);
            if (!security.bootstrapServer || osc == null) {
                continue;
            }
            LOG.trace("Adding OSCORE context information to the context database");
            try {

                // Parse hexadecimal context parameters
                byte[] masterSecret = Hex.decodeHex(osc.oscoreMasterSecret.toCharArray());
                byte[] senderId = Hex.decodeHex(osc.oscoreSenderId.toCharArray());
                byte[] recipientId = Hex.decodeHex(osc.oscoreRecipientId.toCharArray());

                // Parse master salt which, should be conveyed as null if empty
                byte[] masterSalt = Hex.decodeHex(osc.oscoreMasterSalt.toCharArray());
                if (masterSalt.length == 0) {
                    masterSalt = null;
                }

                // Parse AEAD Algorithm
                AlgorithmID aeadAlg = AlgorithmID.FromCBOR(CBORObject.FromObject(osc.oscoreAeadAlgorithm));

                // Parse HKDF Algorithm
                AlgorithmID hkdfAlg = AlgorithmID.FromCBOR(CBORObject.FromObject(osc.oscoreHmacAlgorithm));

                // ID Context is not supported
                byte[] idContext = null;

                // Replay window default value
                int replayWindow = 32;

                OSCoreCtx ctx = new OSCoreCtx(masterSecret, false, aeadAlg, senderId, recipientId, hkdfAlg,
                        replayWindow, masterSalt, idContext);
                // Support Appendix B.2 functionality
                ctx.setContextRederivationEnabled(true);

                db.addContext(ctx);

            } catch (OSException | CoseException e) {
                LOG.error("Failed to add OSCORE context to context database.", e);
            }
        }
    }

    protected static class PskByServer {
        public String serverUrl;
        public String identity;

        public PskByServer(String serverUrl, String identity) {
            this.serverUrl = serverUrl;
            this.identity = identity;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((identity == null) ? 0 : identity.hashCode());
            result = prime * result + ((serverUrl == null) ? 0 : serverUrl.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            PskByServer other = (PskByServer) obj;
            if (identity == null) {
                if (other.identity != null)
                    return false;
            } else if (!identity.equals(other.identity))
                return false;
            if (serverUrl == null) {
                if (other.serverUrl != null)
                    return false;
            } else if (!serverUrl.equals(other.serverUrl))
                return false;
            return true;
        }
    }
}
