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
 *******************************************************************************/
package org.eclipse.leshan.server.bootstrap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.request.BindingMode;

/**
 * A client configuration to be pushed by a bootstrap operation
 */
@SuppressWarnings("serial")
public class BootstrapConfig implements Serializable {

    public Map<Integer, ServerConfig> servers = new HashMap<>();

    public Map<Integer, ServerSecurity> security = new HashMap<>();

    /** server configuration (object 1) */
    static public class ServerConfig implements Serializable {
        public int shortId;
        public int lifetime = 86400;
        public int defaultMinPeriod = 1;
        public Integer defaultMaxPeriod = null;
        public Integer disableTimeout = null;
        public boolean notifIfDisabled = true;
        public BindingMode binding = BindingMode.U;

        @Override
        public String toString() {
            return String
                    .format("ServerConfig [shortId=%s, lifetime=%s, defaultMinPeriod=%s, defaultMaxPeriod=%s, disableTimeout=%s, notifIfDisabled=%s, binding=%s]",
                            shortId, lifetime, defaultMinPeriod, defaultMaxPeriod, disableTimeout, notifIfDisabled,
                            binding);
        }
    }

    /** security configuration (object 0) */
    static public class ServerSecurity implements Serializable {
        public String uri;
        public boolean bootstrapServer = false;
        public SecurityMode securityMode;
        public byte[] publicKeyOrId = new byte[] {};
        public byte[] serverPublicKeyOrId = new byte[] {};
        public byte[] secretKey = new byte[] {};
        public SmsSecurityMode smsSecurityMode = SmsSecurityMode.NO_SEC;
        public byte[] smsBindingKeyParam = new byte[] {};
        public byte[] smsBindingKeySecret = new byte[] {};
        public String serverSmsNumber = ""; // spec says integer WTF?
        public Integer serverId;
        public int clientOldOffTime = 1;

        @Override
        public String toString() {
            // Note : secretKey and smsBindingKeySecret are explicitly excluded from the display for security purposes
            return String
                    .format("ServerSecurity [uri=%s, bootstrapServer=%s, securityMode=%s, publicKeyOrId=%s, serverPublicKeyOrId=%s, smsSecurityMode=%s, smsBindingKeySecret=%s, serverSmsNumber=%s, serverId=%s, clientOldOffTime=%s]",
                            uri, bootstrapServer, securityMode, Arrays.toString(publicKeyOrId),
                            Arrays.toString(serverPublicKeyOrId), smsSecurityMode,
                            Arrays.toString(smsBindingKeyParam), serverSmsNumber,
                            serverId, clientOldOffTime);
        }
    }

    @Override
    public String toString() {
        return String.format("BootstrapConfig [servers=%s, security=%s]", servers, security);
    }

}
