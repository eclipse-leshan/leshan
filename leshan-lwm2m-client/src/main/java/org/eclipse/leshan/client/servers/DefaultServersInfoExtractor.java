/*******************************************************************************
 * Copyright (c) 2025 Sierra Wireless and others.
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
package org.eclipse.leshan.client.servers;

import static org.eclipse.leshan.client.servers.LwM2mServer.SYSTEM;
import static org.eclipse.leshan.core.LwM2mId.OSCORE;
import static org.eclipse.leshan.core.LwM2mId.SECURITY;
import static org.eclipse.leshan.core.LwM2mId.SEC_BOOTSTRAP;
import static org.eclipse.leshan.core.LwM2mId.SEC_OSCORE_SECURITY_MODE;
import static org.eclipse.leshan.core.LwM2mId.SEC_PUBKEY_IDENTITY;
import static org.eclipse.leshan.core.LwM2mId.SEC_SECRET_KEY;
import static org.eclipse.leshan.core.LwM2mId.SEC_SERVER_ID;
import static org.eclipse.leshan.core.LwM2mId.SEC_SERVER_PUBKEY;
import static org.eclipse.leshan.core.LwM2mId.SEC_SERVER_URI;
import static org.eclipse.leshan.core.LwM2mId.SERVER;
import static org.eclipse.leshan.core.LwM2mId.SRV_BINDING;
import static org.eclipse.leshan.core.LwM2mId.SRV_LIFETIME;
import static org.eclipse.leshan.core.LwM2mId.SRV_SERVER_ID;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectTreeReader;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.oscore.AeadAlgorithm;
import org.eclipse.leshan.core.oscore.HkdfAlgorithm;
import org.eclipse.leshan.core.oscore.OscoreSetting;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.security.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultServersInfoExtractor implements ServersInfoExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultServersInfoExtractor.class);

    @Override
    public ServersInfo getInfo(Map<Integer, LwM2mObjectEnabler> objectEnablers) {
        return getInfo(objectEnablers, false);
    }

    @Override
    public ServersInfo getInfo(Map<Integer, LwM2mObjectEnabler> objectEnablers, boolean raiseException)
            throws IllegalStateException {

        LwM2mObjectEnabler securityEnabler = objectEnablers.get(SECURITY);
        LwM2mObjectEnabler serverEnabler = objectEnablers.get(SERVER);
        LwM2mObjectEnabler oscoreEnabler = objectEnablers.get(OSCORE);

        if (securityEnabler == null || serverEnabler == null)
            return null;

        ServersInfo infos = new ServersInfo();
        LwM2mObject securities = (LwM2mObject) securityEnabler.read(SYSTEM, new ReadRequest(SECURITY)).getContent();
        LwM2mObject servers = (LwM2mObject) serverEnabler.read(SYSTEM, new ReadRequest(SERVER)).getContent();

        LwM2mObject oscores = oscoreEnabler != null
                ? (LwM2mObject) oscoreEnabler.read(SYSTEM, new ReadRequest(OSCORE)).getContent()
                : null;

        for (LwM2mObjectInstance security : securities.getInstances().values()) {
            if ((boolean) security.getResource(SEC_BOOTSTRAP).getValue()) {
                if (infos.bootstrap != null) {
                    String message = "There is more than one bootstrap configuration in security object.";
                    LOG.debug(message);
                    if (raiseException) {
                        throw new IllegalStateException(message);
                    }
                } else {
                    // create server info for bootstrap server
                    ServerInfo serverInfo = new ServerInfo();
                    serverInfo.bootstrap = true;
                    try {
                        // fill info from current client state.
                        populateServerInfo(serverInfo, security, oscores);

                        // add server info to result to return
                        infos.bootstrap = serverInfo;
                    } catch (RuntimeException e) {
                        LOG.debug("Unable to get info for bootstrap server /O/{}", security.getId(), e);
                        if (raiseException)
                            throw e;
                    }
                }
            } else {
                try {
                    // create device management info
                    DmServerInfo info = createDMServerInfo(security, servers, oscores);
                    infos.deviceManagements.put(info.serverId, info);
                } catch (RuntimeException e) {
                    LOG.debug("Unable to get info for DM server /O/{}", security.getId(), e);
                    if (raiseException)
                        throw e;
                }
            }
        }
        return infos;
    }

    private void populateServerInfo(ServerInfo info, LwM2mObjectInstance security, LwM2mObject oscoreObject) {
        try {
            LwM2mResource serverIdResource = security.getResource(SEC_SERVER_ID);
            if (serverIdResource != null && serverIdResource.getValue() != null)
                info.serverId = (long) serverIdResource.getValue();
            else
                info.serverId = 0;
            info.serverUri = new URI((String) security.getResource(SEC_SERVER_URI).getValue());
            info.secureMode = ObjectTreeReader.getSecurityMode(security);

            // find associated oscore instance (if any)
            LwM2mObjectInstance oscoreInstance = null;
            ObjectLink oscoreObjLink = (ObjectLink) security.getResource(SEC_OSCORE_SECURITY_MODE).getValue();
            if (oscoreObjLink != null && !oscoreObjLink.isNullLink()) {
                if (oscoreObjLink.getObjectId() != OSCORE) {
                    LOG.warn(
                            "Invalid Security info for LWM2M server {} : 'OSCORE Security Mode' does not link to OSCORE Object but to {} object.",
                            info.serverUri, oscoreObjLink.getObjectId());
                } else {
                    if (oscoreObject == null) {
                        LOG.warn("Invalid Security info for LWM2M server {}: OSCORE object enabler is not available.",
                                info.serverUri);
                    } else {
                        oscoreInstance = oscoreObject.getInstance(oscoreObjLink.getObjectInstanceId());
                        if (oscoreInstance == null) {
                            LOG.warn("Invalid Security info for LWM2M server {} : OSCORE instance {} does not exist.",
                                    info.serverUri, oscoreObjLink.getObjectInstanceId());
                        }
                    }
                }
            }

            if (oscoreInstance != null) {
                info.useOscore = true;

                byte[] masterSecret = ObjectTreeReader.getMasterSecret(oscoreInstance);
                byte[] senderId = ObjectTreeReader.getSenderId(oscoreInstance);
                byte[] recipientId = ObjectTreeReader.getRecipientId(oscoreInstance);
                AeadAlgorithm aeadAlgorithm = AeadAlgorithm
                        .fromValue(ObjectTreeReader.getAeadAlgorithm(oscoreInstance));
                HkdfAlgorithm hkdfAlgorithm = HkdfAlgorithm
                        .fromValue(ObjectTreeReader.getHkdfAlgorithm(oscoreInstance));
                byte[] masterSalt = ObjectTreeReader.getMasterSalt(oscoreInstance);
                info.oscoreSetting = new OscoreSetting(senderId, recipientId, masterSecret, aeadAlgorithm,
                        hkdfAlgorithm, masterSalt);
            } else if (info.secureMode == SecurityMode.PSK) {
                info.pskId = ObjectTreeReader.getPskIdentity(security);
                info.pskKey = ObjectTreeReader.getPskKey(security);
                info.sni = ObjectTreeReader.getSNI(security);
            } else if (info.secureMode == SecurityMode.RPK) {
                info.publicKey = getPublicKey(security);
                info.privateKey = getPrivateKey(security);
                info.serverPublicKey = getServerPublicKey(security);
                info.sni = ObjectTreeReader.getSNI(security);
            } else if (info.secureMode == SecurityMode.X509) {
                info.clientCertificates = getClientCertificates(security);
                info.serverCertificate = getServerCertificate(security);
                info.privateKey = getPrivateKey(security);
                info.certificateUsage = ObjectTreeReader.getCertificateUsage(security);
                info.sni = ObjectTreeReader.getSNI(security);
            }
        } catch (RuntimeException | URISyntaxException e) {
            throw new IllegalStateException("Invalid Security Instance /0/" + security.getId(), e);
        }
    }

    private DmServerInfo createDMServerInfo(LwM2mObjectInstance security, LwM2mObject servers,
            LwM2mObject oscoreObject) {
        DmServerInfo info = new DmServerInfo();
        info.bootstrap = false;
        populateServerInfo(info, security, oscoreObject);

        // search corresponding device management server
        for (LwM2mObjectInstance server : servers.getInstances().values()) {
            try {
                if (info.serverId == (Long) server.getResource(SRV_SERVER_ID).getValue()) {
                    info.lifetime = (long) server.getResource(SRV_LIFETIME).getValue();
                    info.binding = BindingMode.parse((String) server.getResource(SRV_BINDING).getValue());
                    return info;
                }
            } catch (RuntimeException e) {
                throw new IllegalStateException("Invalid Server Instance /1/" + server.getId(), e);
            }
        }
        return null;
    }

    @Override
    public DmServerInfo getDMServerInfo(Map<Integer, LwM2mObjectEnabler> objectEnablers, Long shortID) {
        ServersInfo info = getInfo(objectEnablers);
        if (info == null)
            return null;

        return info.deviceManagements.get(shortID);
    }

    @Override
    public ServerInfo getBootstrapServerInfo(Map<Integer, LwM2mObjectEnabler> objectEnablers) {
        ServersInfo info = getInfo(objectEnablers);
        if (info == null)
            return null;

        return info.bootstrap;
    }

    protected PrivateKey getPrivateKey(LwM2mObjectInstance securityInstance) {
        byte[] encodedKey = (byte[]) securityInstance.getResource(SEC_SECRET_KEY).getValue();
        try {
            return SecurityUtil.privateKey.decode(encodedKey);
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalArgumentException("Failed to decode RFC5958 private key", e);
        }
    }

    protected PublicKey getPublicKey(LwM2mObjectInstance securityInstance) {
        byte[] encodedKey = (byte[]) securityInstance.getResource(SEC_PUBKEY_IDENTITY).getValue();
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
        String algorithm = "EC";
        try {
            KeyFactory kf = KeyFactory.getInstance(algorithm);
            return kf.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Failed to instantiate key factory for algorithm " + algorithm, e);
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException("Failed to decode RFC7250 public key with algorithm " + algorithm, e);
        }
    }

    protected PublicKey getServerPublicKey(LwM2mObjectInstance securityInstance) {
        byte[] encodedKey = (byte[]) securityInstance.getResource(SEC_SERVER_PUBKEY).getValue();
        try {
            return SecurityUtil.publicKey.decode(encodedKey);
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalArgumentException("Failed to decode RFC7250 public key", e);
        }
    }

    protected Certificate getServerCertificate(LwM2mObjectInstance securityInstance) {
        byte[] encodedCert = (byte[]) securityInstance.getResource(SEC_SERVER_PUBKEY).getValue();
        try {
            return SecurityUtil.derCertificate.decode(encodedCert);
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalArgumentException("Failed to decode X.509 certificate", e);
        }
    }

    protected Certificate[] getClientCertificates(LwM2mObjectInstance securityInstance) {
        byte[] encodedCert = (byte[]) securityInstance.getResource(SEC_PUBKEY_IDENTITY).getValue();
        try {
            return new Certificate[] { SecurityUtil.derCertificate.decode(encodedCert) };
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalArgumentException("Failed to decode X.509 certificate", e);
        }
    }
}
