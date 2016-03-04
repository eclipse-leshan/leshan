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

import static org.eclipse.leshan.LwM2mId.SEC_BOOTSTRAP;
import static org.eclipse.leshan.LwM2mId.SEC_PUBKEY_IDENTITY;
import static org.eclipse.leshan.LwM2mId.SEC_SECRET_KEY;
import static org.eclipse.leshan.LwM2mId.SEC_SECURITY_MODE;
import static org.eclipse.leshan.LwM2mId.SEC_SERVER_ID;
import static org.eclipse.leshan.LwM2mId.SEC_SERVER_PUBKEY;
import static org.eclipse.leshan.LwM2mId.SEC_SERVER_URI;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
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

    private String serverUri; /* coaps://host:port */
    private boolean bootstrapServer;
    // private SecurityMode securityMode;
    private int securityMode;
    private byte[] publicKeyOrIdentity;
    private byte[] serverPublicKey;
    private byte[] secretKey;

    private Integer shortServerId;

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
     * Returns a new security instance (NoSec) for a boostrap server.
     */
    public static Security noSecBootstap(String serverUri) {
        return new Security(serverUri, true, 3, new byte[0], new byte[0], new byte[0], 0);
    }

    /**
     * Returns a new security instance (PSK) for a boostrap server.
     */
    public static Security pskBootstrap(String serverUri, byte[] pskIdentity, byte[] privateKey) {
        return new Security(serverUri, true, 0, pskIdentity.clone(), new byte[0], privateKey.clone(), 0);
    }

    /**
     * Returns a new security instance (NoSec) for a device management server.
     */
    public static Security noSec(String serverUri, int shortServerId) {
        return new Security(serverUri, false, 3, new byte[0], new byte[0], new byte[0], shortServerId);
    }

    /**
     * Returns a new security instance (PSK) for a device management server.
     */
    public static Security psk(String serverUri, int shortServerId, byte[] pskIdentity, byte[] privateKey) {
        return new Security(serverUri, false, 0, pskIdentity.clone(), new byte[0], privateKey.clone(), shortServerId);
    }

    /**
     * Returns a new security instance (RPK) for a device management server.
     */
    public static Security rpk(String serverUri, int shortServerId, byte[] clientPublicKey, byte[] clientPrivateKey,
            byte[] serverPublicKey) {
        return new Security(serverUri, false, 1, clientPublicKey.clone(), serverPublicKey.clone(),
                clientPrivateKey.clone(), shortServerId);
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
            secretKey = (byte[]) value.getValue();
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

}
