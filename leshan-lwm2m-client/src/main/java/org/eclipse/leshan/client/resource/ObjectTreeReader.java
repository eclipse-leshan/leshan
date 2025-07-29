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
package org.eclipse.leshan.client.resource;

import static org.eclipse.leshan.client.servers.LwM2mServer.SYSTEM;
import static org.eclipse.leshan.core.LwM2mId.DEVICE;
import static org.eclipse.leshan.core.LwM2mId.DVC_SUPPORTED_BINDING;
import static org.eclipse.leshan.core.LwM2mId.OSCORE_AEAD_ALGORITHM;
import static org.eclipse.leshan.core.LwM2mId.OSCORE_HMAC_ALGORITHM;
import static org.eclipse.leshan.core.LwM2mId.OSCORE_MASTER_SALT;
import static org.eclipse.leshan.core.LwM2mId.OSCORE_MASTER_SECRET;
import static org.eclipse.leshan.core.LwM2mId.OSCORE_RECIPIENT_ID;
import static org.eclipse.leshan.core.LwM2mId.OSCORE_SENDER_ID;
import static org.eclipse.leshan.core.LwM2mId.SECURITY;
import static org.eclipse.leshan.core.LwM2mId.SEC_BOOTSTRAP;
import static org.eclipse.leshan.core.LwM2mId.SEC_CERTIFICATE_USAGE;
import static org.eclipse.leshan.core.LwM2mId.SEC_PUBKEY_IDENTITY;
import static org.eclipse.leshan.core.LwM2mId.SEC_SECRET_KEY;
import static org.eclipse.leshan.core.LwM2mId.SEC_SECURITY_MODE;
import static org.eclipse.leshan.core.LwM2mId.SEC_SERVER_ID;
import static org.eclipse.leshan.core.LwM2mId.SEC_SERVER_URI;
import static org.eclipse.leshan.core.LwM2mId.SEC_SNI;
import static org.eclipse.leshan.core.LwM2mId.SERVER;
import static org.eclipse.leshan.core.LwM2mId.SRV_BINDING;
import static org.eclipse.leshan.core.LwM2mId.SRV_LIFETIME;
import static org.eclipse.leshan.core.LwM2mId.SRV_SERVER_ID;

import java.util.EnumSet;

import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.CertificateUsage;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.oscore.AeadAlgorithm;
import org.eclipse.leshan.core.oscore.HkdfAlgorithm;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.util.datatype.ULong;

@SuppressWarnings({ "java:S1168", "java:S2447" })
public class ObjectTreeReader {

    private ObjectTreeReader() {
    }

    public static LwM2mObjectInstance getBootstrapSecurityInstance(LwM2mObjectEnabler securityEnabler) {
        LwM2mObject securities = (LwM2mObject) securityEnabler.read(SYSTEM, new ReadRequest(SECURITY)).getContent();
        if (securities != null) {
            for (LwM2mObjectInstance instance : securities.getInstances().values()) {
                if (isBootstrapServer(instance)) {
                    return instance;
                }
            }
        }

        return null;
    }

    public static Long getLifeTime(LwM2mObjectEnabler serverEnabler, int instanceId) {
        ReadResponse response = serverEnabler.read(LwM2mServer.SYSTEM,
                new ReadRequest(SERVER, instanceId, SRV_LIFETIME));
        if (response.isSuccess()) {
            return (Long) ((LwM2mResource) response.getContent()).getValue();
        } else {
            return null;
        }
    }

    public static EnumSet<BindingMode> getServerBindingMode(LwM2mObjectEnabler serverEnabler, int instanceId) {
        ReadResponse response = serverEnabler.read(LwM2mServer.SYSTEM,
                new ReadRequest(SERVER, instanceId, SRV_BINDING));
        if (response.isSuccess()) {
            return BindingMode.parse((String) ((LwM2mResource) response.getContent()).getValue());
        } else {
            return null;
        }
    }

    public static EnumSet<BindingMode> getDeviceSupportedBindingMode(LwM2mObjectEnabler deviceEnabler, int instanceId) {
        ReadResponse response = deviceEnabler.read(LwM2mServer.SYSTEM,
                new ReadRequest(DEVICE, instanceId, DVC_SUPPORTED_BINDING));
        if (response.isSuccess()) {
            return BindingMode.parse((String) ((LwM2mResource) response.getContent()).getValue());
        } else {
            return null;
        }
    }

    public static Boolean isBootstrapServer(LwM2mObjectEnabler objectEnabler, int instanceId) {
        ReadResponse response = objectEnabler.read(LwM2mServer.SYSTEM,
                new ReadRequest(SECURITY, instanceId, SEC_BOOTSTRAP));
        if (response != null && response.isSuccess()) {
            return (Boolean) ((LwM2mResource) response.getContent()).getValue();
        } else {
            return null;
        }
    }

    public static Long getServerId(LwM2mObjectEnabler objectEnabler, int instanceId) {
        ReadResponse response = null;
        if (objectEnabler.getId() == SERVER) {
            response = objectEnabler.read(LwM2mServer.SYSTEM, new ReadRequest(SERVER, instanceId, SRV_SERVER_ID));
        } else if (objectEnabler.getId() == SECURITY) {
            response = objectEnabler.read(LwM2mServer.SYSTEM, new ReadRequest(SECURITY, instanceId, SEC_SERVER_ID));
        }
        if (response != null && response.isSuccess()) {
            return (Long) ((LwM2mResource) response.getContent()).getValue();
        } else {
            return null;
        }
    }

    public static String getServerURI(LwM2mObjectEnabler objectEnabler, int instanceId) {
        ReadResponse response = null;
        if (objectEnabler.getId() == SECURITY) {
            response = objectEnabler.read(LwM2mServer.SYSTEM, new ReadRequest(SECURITY, instanceId, SEC_SERVER_URI));
        }
        if (response != null && response.isSuccess()) {
            return (String) ((LwM2mResource) response.getContent()).getValue();
        } else {
            return null;
        }
    }

    public static SecurityMode getSecurityMode(LwM2mObjectInstance securityInstance) {
        return SecurityMode.fromCode((long) securityInstance.getResource(SEC_SECURITY_MODE).getValue());
    }

    public static CertificateUsage getCertificateUsage(LwM2mObjectInstance securityInstance) {
        return CertificateUsage.fromCode((ULong) securityInstance.getResource(SEC_CERTIFICATE_USAGE).getValue());
    }

    public static String getPskIdentity(LwM2mObjectInstance securityInstance) {
        byte[] pubKey = (byte[]) securityInstance.getResource(SEC_PUBKEY_IDENTITY).getValue();
        return new String(pubKey);
    }

    public static byte[] getPskKey(LwM2mObjectInstance securityInstance) {
        return (byte[]) securityInstance.getResource(SEC_SECRET_KEY).getValue();
    }

    public static String getSNI(LwM2mObjectInstance securityInstance) {
        LwM2mResource resource = securityInstance.getResource(SEC_SNI);
        if (resource == null) {
            return null;
        } else {
            return (String) resource.getValue();
        }
    }

    public static boolean isBootstrapServer(LwM2mInstanceEnabler instance) {
        ReadResponse response = instance.read(LwM2mServer.SYSTEM, LwM2mId.SEC_BOOTSTRAP);
        if (response == null || response.isFailure()) {
            return false;
        }

        LwM2mResource isBootstrap = (LwM2mResource) response.getContent();
        return (Boolean) isBootstrap.getValue();
    }

    public static boolean isBootstrapServer(LwM2mObjectInstance instance) {
        LwM2mResource resource = instance.getResource(SEC_BOOTSTRAP);
        if (resource == null) {
            return false;
        }
        return (Boolean) resource.getValue();
    }

    // OSCORE related methods below

    public static Integer getOscoreSecurityMode(LwM2mObjectInstance securityInstance) {
        LwM2mResource resource = securityInstance.getResource(LwM2mId.SEC_OSCORE_SECURITY_MODE);
        if (resource != null)
            return ((ObjectLink) resource.getValue()).getObjectInstanceId();
        return null;
    }

    public static byte[] getMasterSecret(LwM2mObjectInstance oscoreInstance) {
        return (byte[]) oscoreInstance.getResource(OSCORE_MASTER_SECRET).getValue();
    }

    public static byte[] getSenderId(LwM2mObjectInstance oscoreInstance) {
        return (byte[]) oscoreInstance.getResource(OSCORE_SENDER_ID).getValue();
    }

    public static byte[] getRecipientId(LwM2mObjectInstance oscoreInstance) {
        return (byte[]) oscoreInstance.getResource(OSCORE_RECIPIENT_ID).getValue();
    }

    public static long getAeadAlgorithm(LwM2mObjectInstance oscoreInstance) {
        LwM2mResource resource = oscoreInstance.getResource(OSCORE_AEAD_ALGORITHM);
        if (resource != null)
            return (long) resource.getValue();
        // return default one from https://datatracker.ietf.org/doc/html/rfc8613#section-3.2
        return AeadAlgorithm.AES_CCM_16_64_128.getValue();
    }

    public static long getHkdfAlgorithm(LwM2mObjectInstance oscoreInstance) {
        LwM2mResource resource = oscoreInstance.getResource(OSCORE_HMAC_ALGORITHM);
        if (resource != null)
            return (long) resource.getValue();
        // return default one from https://datatracker.ietf.org/doc/html/rfc8613#section-3.2
        return HkdfAlgorithm.HKDF_HMAC_SHA_256.getValue();
    }

    public static byte[] getMasterSalt(LwM2mObjectInstance oscoreInstance) {
        LwM2mResource resource = oscoreInstance.getResource(OSCORE_MASTER_SALT);
        if (resource == null)
            return null;

        byte[] value = (byte[]) resource.getValue();
        if (value.length == 0) {
            return null;
        } else {
            return value;
        }
    }
}
