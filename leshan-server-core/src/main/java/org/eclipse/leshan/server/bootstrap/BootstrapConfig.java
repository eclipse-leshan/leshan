/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Rikard Höglund (RISE) - additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.server.bootstrap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.SecurityMode;
import org.eclipse.leshan.core.request.BindingMode;

/**
 * A client configuration to be pushed by a bootstrap operation
 */
@SuppressWarnings("serial")
public class BootstrapConfig implements Serializable {

    public Map<Integer, ServerConfig> servers = new HashMap<>();

    public Map<Integer, ServerSecurity> security = new HashMap<>();

    public Map<Integer, ACLConfig> acls = new HashMap<>();

    public Map<Integer, OscoreObject> oscore = new HashMap<>();

    /** server configuration (object 1) */
    static public class ServerConfig implements Serializable {
        public int shortId;
        public int lifetime = 86400;
        public Integer defaultMinPeriod = 1;
        public Integer defaultMaxPeriod = null;
        public Integer disableTimeout = null;
        public boolean notifIfDisabled = true;
        public BindingMode binding = BindingMode.U;

        @Override
        public String toString() {
            return String.format(
                    "ServerConfig [shortId=%s, lifetime=%s, defaultMinPeriod=%s, defaultMaxPeriod=%s, disableTimeout=%s, notifIfDisabled=%s, binding=%s]",
                    shortId, lifetime, defaultMinPeriod, defaultMaxPeriod, disableTimeout, notifIfDisabled, binding);
        }
    }

    /** security configuration (object 0) */
    static public class ServerSecurity implements Serializable {
        public String uri;
        public boolean bootstrapServer = false;
        public SecurityMode securityMode;
        public byte[] publicKeyOrId = new byte[] {};
        public byte[] serverPublicKey = new byte[] {};
        public byte[] secretKey = new byte[] {};
        public SmsSecurityMode smsSecurityMode = SmsSecurityMode.NO_SEC;
        public byte[] smsBindingKeyParam = new byte[] {};
        public byte[] smsBindingKeySecret = new byte[] {};
        public String serverSmsNumber = "";
        public Integer serverId;
        public Integer clientOldOffTime = 1;
        public Integer bootstrapServerAccountTimeout = 0;
        public Integer oscoreSecurityMode;

        @Override
        public String toString() {
            // Note : secretKey and smsBindingKeySecret are explicitly excluded from the display for security purposes
            return String.format(
                    "ServerSecurity [uri=%s, bootstrapServer=%s, securityMode=%s, publicKeyOrId=%s, serverPublicKey=%s, smsSecurityMode=%s, smsBindingKeySecret=%s, serverSmsNumber=%s, serverId=%s, clientOldOffTime=%s, bootstrapServerAccountTimeout=%s]",
                    uri, bootstrapServer, securityMode, Arrays.toString(publicKeyOrId),
                    Arrays.toString(serverPublicKey), smsSecurityMode, Arrays.toString(smsBindingKeyParam),
                    serverSmsNumber, serverId, clientOldOffTime, bootstrapServerAccountTimeout);
        }
    }

    /** server configuration (object 1) */
    static public class ACLConfig implements Serializable {
        public int objectId;
        public int objectInstanceId;
        public Map<Integer, Long> acls;
        public Integer AccessControlOwner;

        @Override
        public String toString() {
            return String.format("ACLConfig [objectId=%s, objectInstanceId=%s, ACLs=%s, AccessControlOwner=%s]",
                    objectId, objectInstanceId, acls, AccessControlOwner);
        }
    }

    /** oscore configuration (object 17) */
    static public class OscoreObject implements Serializable {

        public int objectInstanceId;
        public byte[] oscoreMasterSecret = new byte[] {};
        public byte[] oscoreSenderId = new byte[] {};
        public byte[] oscoreRecipientId = new byte[] {};
        public String oscoreAeadAlgorithm = "";
        public String oscoreHmacAlgorithm = "";
        public byte[] oscoreMasterSalt = new byte[] {};
        public byte[] oscoreIdContext = new byte[] {};

        @Override
        public String toString() {
            // Note : oscoreMasterSecret and oscoreMasterSalt are explicitly excluded from the display for security purposes
            return String.format(
                    "OscoreObject [objectInstanceId=%s, oscoreSenderId=%s, oscoreRecipientId=%s, oscoreAeadAlgorithm=%s, oscoreHmacAlgorithm=%s, oscoreIdContext=%s]",
                    objectInstanceId, Arrays.toString(oscoreSenderId), Arrays.toString(oscoreRecipientId),
                    oscoreAeadAlgorithm, oscoreHmacAlgorithm, Arrays.toString(oscoreIdContext));
        }
    }

    @Override
    public String toString() {
        return String.format("BootstrapConfig [servers=%s, security=%s, acls=%s]", servers, security, acls);
    }
}
