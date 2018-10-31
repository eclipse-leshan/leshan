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
 *******************************************************************************/
package org.eclipse.leshan.client.object;

import static org.eclipse.leshan.LwM2mId.*;

import java.util.Arrays;
import java.util.List;

import org.eclipse.leshan.SecurityMode;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple {@link LwM2mInstanceEnabler} for the Security (0) object.
 */
public class Security extends BaseInstanceEnabler {

    private static final Logger LOG = LoggerFactory.getLogger(Security.class);

    private final static List<Integer> supportedResources = Arrays.asList(SEC_SERVER_URI, SEC_BOOTSTRAP,
            SEC_SECURITY_MODE, SEC_PUBKEY_IDENTITY, SEC_SERVER_PUBKEY, SEC_SECRET_KEY, SEC_SERVER_ID);

    private String serverUri; /* coaps://host:port */
    private boolean bootstrapServer;
    // private SecurityMode securityMode;
    private int securityMode;
    private byte[] publicKeyOrIdentity;
    private byte[] serverPublicKey;
    private byte[] secretKey;

    private Integer shortServerId;

    public Security() {
        // should only be used at bootstrap time
    }

    public Security(String serverUri, boolean bootstrapServer, int securityMode, byte[] publicKeyOrIdentity,
            byte[] serverPublicKey, byte[] secretKey, Integer shortServerId) {
        this.serverUri = serverUri;
        this.bootstrapServer = bootstrapServer;
        this.securityMode = securityMode;
        this.publicKeyOrIdentity = publicKeyOrIdentity;
        this.serverPublicKey = serverPublicKey;
        this.secretKey = secretKey;
        this.shortServerId = shortServerId;
    }

    /**
     * Returns a new security instance (NoSec) for a bootstrap server.
     */
    public static Security noSecBootstap(String serverUri) {
        return new Security(serverUri, true, SecurityMode.NO_SEC.code, new byte[0], new byte[0], new byte[0], 0);
    }

    /**
     * Returns a new security instance (PSK) for a bootstrap server.
     */
    public static Security pskBootstrap(String serverUri, byte[] pskIdentity, byte[] privateKey) {
        return new Security(serverUri, true, SecurityMode.PSK.code, pskIdentity.clone(), new byte[0],
                privateKey.clone(), 0);
    }

    /**
     * Returns a new security instance (RPK) for a bootstrap server.
     */
    public static Security rpkBootstrap(String serverUri, byte[] clientPublicKey, byte[] clientPrivateKey,
            byte[] serverPublicKey) {
        return new Security(serverUri, true, SecurityMode.RPK.code, clientPublicKey.clone(), serverPublicKey.clone(),
                clientPrivateKey.clone(), 0);
    }

    /**
     * Returns a new security instance (X509) for a bootstrap server.
     */
    public static Security x509Bootstrap(String serverUri, byte[] clientCertificate, byte[] clientPrivateKey,
            byte[] serverPublicKey) {
        return new Security(serverUri, true, SecurityMode.X509.code, clientCertificate.clone(), serverPublicKey.clone(),
                clientPrivateKey.clone(), 0);
    }

    /**
     * Returns a new security instance (NoSec) for a device management server.
     */
    public static Security noSec(String serverUri, int shortServerId) {
        return new Security(serverUri, false, SecurityMode.NO_SEC.code, new byte[0], new byte[0], new byte[0],
                shortServerId);
    }

    /**
     * Returns a new security instance (PSK) for a device management server.
     */
    public static Security psk(String serverUri, int shortServerId, byte[] pskIdentity, byte[] privateKey) {
        return new Security(serverUri, false, SecurityMode.PSK.code, pskIdentity.clone(), new byte[0],
                privateKey.clone(), shortServerId);
    }

    /**
     * Returns a new security instance (RPK) for a device management server.
     */
    public static Security rpk(String serverUri, int shortServerId, byte[] clientPublicKey, byte[] clientPrivateKey,
            byte[] serverPublicKey) {
        return new Security(serverUri, false, SecurityMode.RPK.code, clientPublicKey.clone(), serverPublicKey.clone(),
                clientPrivateKey.clone(), shortServerId);
    }

    /**
     * Returns a new security instance (X509) for a device management server.
     */
    public static Security x509(String serverUri, int shortServerId, byte[] clientCertificate, byte[] clientPrivateKey,
            byte[] serverPublicKey) {
        return new Security(serverUri, false, SecurityMode.X509.code, clientCertificate.clone(),
                serverPublicKey.clone(), clientPrivateKey.clone(), shortServerId);
    }

    @Override
    public WriteResponse write(int resourceId, LwM2mResource value) {
        LOG.debug("Write on resource {}: {}", resourceId, value);

        // restricted to BS server?

        switch (resourceId) {

        case SEC_SERVER_URI: // server uri
            if (value.getType() != Type.STRING) {
                return WriteResponse.badRequest("invalid type");
            }
            serverUri = (String) value.getValue();
            return WriteResponse.success();

        case SEC_BOOTSTRAP: // is bootstrap server
            if (value.getType() != Type.BOOLEAN) {
                return WriteResponse.badRequest("invalid type");
            }
            bootstrapServer = (Boolean) value.getValue();
            return WriteResponse.success();

        case SEC_SECURITY_MODE: // security mode
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            securityMode = ((Long) value.getValue()).intValue();
            return WriteResponse.success();

        case SEC_PUBKEY_IDENTITY: // Public Key or Identity
            if (value.getType() != Type.OPAQUE) {
                return WriteResponse.badRequest("invalid type");
            }
            publicKeyOrIdentity = (byte[]) value.getValue();
            return WriteResponse.success();
        case SEC_SERVER_PUBKEY: // server public key
            if (value.getType() != Type.OPAQUE) {
                return WriteResponse.badRequest("invalid type");
            }
            serverPublicKey = (byte[]) value.getValue();
            return WriteResponse.success();
        case SEC_SECRET_KEY: // Secret Key
            if (value.getType() != Type.OPAQUE) {
                return WriteResponse.badRequest("invalid type");
            }
            secretKey = (byte[]) value.getValue();
            return WriteResponse.success();
        case SEC_SERVER_ID: // short server id
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            shortServerId = ((Long) value.getValue()).intValue();
            return WriteResponse.success();

        default:
            return super.write(resourceId, value);
        }

    }

    @Override
    public ReadResponse read(int resourceid) {
        // only accessible for internal read?

        switch (resourceid) {
        case SEC_SERVER_URI: // server uri
            return ReadResponse.success(resourceid, serverUri);

        case SEC_BOOTSTRAP: // is bootstrap server?
            return ReadResponse.success(resourceid, bootstrapServer);

        case SEC_SECURITY_MODE: // security mode
            return ReadResponse.success(resourceid, securityMode);

        case SEC_PUBKEY_IDENTITY: // public key or identity
            return ReadResponse.success(resourceid, publicKeyOrIdentity);

        case SEC_SERVER_PUBKEY: // server public key
            return ReadResponse.success(resourceid, serverPublicKey);

        case SEC_SECRET_KEY: // secret key
            return ReadResponse.success(resourceid, secretKey);

        case SEC_SERVER_ID: // short server id
            return ReadResponse.success(resourceid, shortServerId);

        default:
            return super.read(resourceid);
        }
    }

    @Override
    public ExecuteResponse execute(int resourceid, String params) {
        return super.execute(resourceid, params);
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }
}
