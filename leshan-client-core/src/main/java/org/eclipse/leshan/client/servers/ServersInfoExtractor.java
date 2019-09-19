/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use ServerIdentity.SYSTEM
 *     Rikard Höglund (RISE SICS) - Additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.client.servers;

import static org.eclipse.leshan.LwM2mId.*;
import static org.eclipse.leshan.client.request.ServerIdentity.SYSTEM;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

import org.eclipse.leshan.LwM2mId;
import org.eclipse.leshan.SecurityMode;
import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extract from LwM2m tree servers information like server uri, security mode, ...
 */
public class ServersInfoExtractor {
    private static final Logger LOG = LoggerFactory.getLogger(ServersInfoExtractor.class);

    public static ServersInfo getInfo(Map<Integer, LwM2mObjectEnabler> objectEnablers) {
        LwM2mObjectEnabler securityEnabler = objectEnablers.get(SECURITY);
        LwM2mObjectEnabler serverEnabler = objectEnablers.get(SERVER);
        LwM2mObjectEnabler oscoreEnabler = objectEnablers.get(OSCORE);

        if (securityEnabler == null || serverEnabler == null)
            return null;

        ServersInfo infos = new ServersInfo();
        LwM2mObject securities = (LwM2mObject) securityEnabler.read(SYSTEM, new ReadRequest(SECURITY)).getContent();
        LwM2mObject servers = (LwM2mObject) serverEnabler.read(SYSTEM, new ReadRequest(SERVER)).getContent();

        LwM2mObject oscores = null;
        if (oscoreEnabler != null) {
            oscores = (LwM2mObject) oscoreEnabler.read(SYSTEM, new ReadRequest(OSCORE)).getContent();
        }

        for (LwM2mObjectInstance security : securities.getInstances().values()) {
            try {
                if ((boolean) security.getResource(SEC_BOOTSTRAP).getValue()) {
                    if (infos.bootstrap != null) {
                        LOG.warn("There is more than one bootstrap configuration in security object.");
                    } else {
                        // create bootstrap info
                        ServerInfo info = new ServerInfo();
                        LwM2mResource serverIdResource = security.getResource(SEC_SERVER_ID);
                        if (serverIdResource != null && serverIdResource.getValue() != null)
                            info.serverId = (long) serverIdResource.getValue();
                        else
                            info.serverId = 0;
                        info.serverUri = new URI((String) security.getResource(SEC_SERVER_URI).getValue());
                        info.secureMode = getSecurityMode(security);
                        if (info.secureMode == SecurityMode.PSK) {
                            info.pskId = getPskIdentity(security);
                            info.pskKey = getPskKey(security);
                        } else if (info.secureMode == SecurityMode.RPK) {
                            info.publicKey = getPublicKey(security);
                            info.privateKey = getPrivateKey(security);
                            info.serverPublicKey = getServerPublicKey(security);
                        } else if (info.secureMode == SecurityMode.X509) {
                            info.clientCertificate = getClientCertificate(security);
                            info.serverCertificate = getServerCertificate(security);
                            info.privateKey = getPrivateKey(security);
                        }
                        infos.bootstrap = info;
                    }
                } else {
                    // create device management info
                    DmServerInfo info = new DmServerInfo();
                    info.serverUri = new URI((String) security.getResource(SEC_SERVER_URI).getValue());
                    info.serverId = (long) security.getResource(SEC_SERVER_ID).getValue();
                    info.secureMode = getSecurityMode(security);

                    // find instance id of the associated oscore object (if any)
                    ObjectLink oscoreObjLink = (ObjectLink) security.getResource(SEC_OSCORE_SECURITY_MODE).getValue();
                    int oscoreObjectInstanceId = oscoreObjLink.getObjectInstanceId();
                    boolean useOscore = oscoreObjLink.getObjectId() == OSCORE;

                    if (useOscore) {
                        // get corresponding oscore object
                        LwM2mObjectInstance oscoreInstance = oscores.getInstance(oscoreObjectInstanceId);

                        info.useOscore = true;
                        info.masterSecret = getMasterSecret(oscoreInstance);
                        info.senderId = getSenderId(oscoreInstance);
                        info.recipientId = getRecipientId(oscoreInstance);
                        info.aeadAlgorithm = getAeadAlgorithm(oscoreInstance);
                        info.hkdfAlgorithm = getHkdfAlgorithm(oscoreInstance);
                        info.masterSalt = getMasterSalt(oscoreInstance);
                        info.idContext = getIdContext(oscoreInstance);
                    } else if (info.secureMode == SecurityMode.PSK) {
                        info.pskId = getPskIdentity(security);
                        info.pskKey = getPskKey(security);
                    } else if (info.secureMode == SecurityMode.RPK) {
                        info.publicKey = getPublicKey(security);
                        info.privateKey = getPrivateKey(security);
                        info.serverPublicKey = getServerPublicKey(security);
                    } else if (info.secureMode == SecurityMode.X509) {
                        info.clientCertificate = getClientCertificate(security);
                        info.serverCertificate = getServerCertificate(security);
                        info.privateKey = getPrivateKey(security);
                    }
                    // search corresponding device management server
                    for (LwM2mObjectInstance server : servers.getInstances().values()) {
                        if (info.serverId == (Long) server.getResource(SRV_SERVER_ID).getValue()) {
                            info.lifetime = (long) server.getResource(SRV_LIFETIME).getValue();
                            info.binding = BindingMode.valueOf((String) server.getResource(SRV_BINDING).getValue());

                            infos.deviceMangements.put(info.serverId, info);
                            break;
                        }
                    }
                }
            } catch (URISyntaxException e) {
                LOG.error(String.format("Invalid URI %s", (String) security.getResource(SEC_SERVER_URI).getValue()), e);
            }
        }
        return infos;
    }

    public static DmServerInfo getDMServerInfo(Map<Integer, LwM2mObjectEnabler> objectEnablers, Long shortID) {
        ServersInfo info = getInfo(objectEnablers);
        if (info == null)
            return null;

        return info.deviceMangements.get(shortID);
    }

    public static ServerInfo getBootstrapServerInfo(Map<Integer, LwM2mObjectEnabler> objectEnablers) {
        ServersInfo info = getInfo(objectEnablers);
        if (info == null)
            return null;

        return info.bootstrap;
    }

    public static SecurityMode getSecurityMode(LwM2mObjectInstance securityInstance) {
        return SecurityMode.fromCode((long) securityInstance.getResource(SEC_SECURITY_MODE).getValue());
    }

    public static String getPskIdentity(LwM2mObjectInstance securityInstance) {
        byte[] pubKey = (byte[]) securityInstance.getResource(SEC_PUBKEY_IDENTITY).getValue();
        return new String(pubKey);
    }

    public static byte[] getPskKey(LwM2mObjectInstance securityInstance) {
        return (byte[]) securityInstance.getResource(SEC_SECRET_KEY).getValue();
    }

    private static PublicKey getPublicKey(LwM2mObjectInstance securityInstance) {
        byte[] encodedKey = (byte[]) securityInstance.getResource(SEC_PUBKEY_IDENTITY).getValue();
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
        String algorithm = "EC";
        try {
            KeyFactory kf = KeyFactory.getInstance(algorithm);
            return kf.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            LOG.debug("Failed to instantiate key factory for algorithm " + algorithm, e);
        } catch (InvalidKeySpecException e) {
            LOG.debug("Failed to decode RFC7250 public key with algorithm " + algorithm, e);
        }
        return null;
    }

    private static PrivateKey getPrivateKey(LwM2mObjectInstance securityInstance) {
        byte[] encodedKey = (byte[]) securityInstance.getResource(SEC_SECRET_KEY).getValue();
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);
        String algorithm = "EC";
        try {
            KeyFactory kf = KeyFactory.getInstance(algorithm);
            return kf.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException e) {
            LOG.warn("Failed to instantiate key factory for algorithm " + algorithm, e);
        } catch (InvalidKeySpecException e) {
            LOG.warn("Failed to decode RFC5958 private key with algorithm " + algorithm, e);
        }
        return null;
    }

    private static PublicKey getServerPublicKey(LwM2mObjectInstance securityInstance) {
        byte[] encodedKey = (byte[]) securityInstance.getResource(SEC_SERVER_PUBKEY).getValue();
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
        String algorithm = "EC";
        try {
            KeyFactory kf = KeyFactory.getInstance(algorithm);
            return kf.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            LOG.debug("Failed to instantiate key factory for algorithm " + algorithm, e);
        } catch (InvalidKeySpecException e) {
            LOG.debug("Failed to decode RFC7250 public key with algorithm " + algorithm, e);
        }
        return null;
    }

    private static Certificate getServerCertificate(LwM2mObjectInstance securityInstance) {
        byte[] encodedCert = (byte[]) securityInstance.getResource(SEC_SERVER_PUBKEY).getValue();
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            try (ByteArrayInputStream in = new ByteArrayInputStream(encodedCert)) {
                return cf.generateCertificate(in);
            }
        } catch (CertificateException | IOException e) {
            LOG.debug("Failed to decode X.509 certificate", e);
            return null;
        }
    }

    private static Certificate getClientCertificate(LwM2mObjectInstance securityInstance) {
        byte[] encodedCert = (byte[]) securityInstance.getResource(SEC_PUBKEY_IDENTITY).getValue();
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            try (ByteArrayInputStream in = new ByteArrayInputStream(encodedCert)) {
                return cf.generateCertificate(in);
            }
        } catch (CertificateException | IOException e) {
            LOG.debug("Failed to decode X.509 certificate", e);
            return null;
        }
    }

    public static boolean isBootstrapServer(LwM2mInstanceEnabler instance) {
        ReadResponse response = instance.read(ServerIdentity.SYSTEM, LwM2mId.SEC_BOOTSTRAP);
        if (response == null || response.isFailure()) {
            return false;
        }

        LwM2mResource isBootstrap = (LwM2mResource) response.getContent();
        return (Boolean) isBootstrap.getValue();
    }

    // OSCORE related methods below

    public static byte[] getMasterSecret(LwM2mObjectInstance oscoreInstance) {
        String value = (String) oscoreInstance.getResource(OSCORE_Master_Secret).getValue();
        return Hex.decodeHex(value.toCharArray());
    }

    public static byte[] getSenderId(LwM2mObjectInstance oscoreInstance) {
        String value = (String) oscoreInstance.getResource(OSCORE_Sender_ID).getValue();
        return Hex.decodeHex(value.toCharArray());
    }

    public static byte[] getRecipientId(LwM2mObjectInstance oscoreInstance) {
        String value = (String) oscoreInstance.getResource(OSCORE_Recipient_ID).getValue();
        return Hex.decodeHex(value.toCharArray());
    }

    public static long getAeadAlgorithm(LwM2mObjectInstance oscoreInstance) {
        return (long) oscoreInstance.getResource(OSCORE_AEAD_Algorithm).getValue();
    }

    public static long getHkdfAlgorithm(LwM2mObjectInstance oscoreInstance) {
        return (long) oscoreInstance.getResource(OSCORE_HMAC_Algorithm).getValue();
    }

    public static byte[] getMasterSalt(LwM2mObjectInstance oscoreInstance) {
        String value = (String) oscoreInstance.getResource(OSCORE_Master_Salt).getValue();

        if (value.equals("")) {
            return null;
        } else {
            return Hex.decodeHex(value.toCharArray());
        }
    }

    public static byte[] getIdContext(LwM2mObjectInstance oscoreInstance) {
        return null;
    }
}
