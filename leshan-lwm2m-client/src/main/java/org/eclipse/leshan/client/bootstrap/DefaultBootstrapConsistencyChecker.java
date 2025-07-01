/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.client.bootstrap;

import java.security.cert.X509Certificate;
import java.util.List;

import org.eclipse.leshan.client.servers.DmServerInfo;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.client.servers.ServersInfoExtractor;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.oscore.InvalidOscoreSettingException;
import org.eclipse.leshan.core.oscore.OscoreValidator;
import org.eclipse.leshan.core.security.certificate.util.X509CertUtil;

/**
 * A default implementation of {@link BootstrapConsistencyChecker}
 */
public class DefaultBootstrapConsistencyChecker extends BaseBootstrapConsistencyChecker {

    private final OscoreValidator oscoreValidator = new OscoreValidator();

    public DefaultBootstrapConsistencyChecker(ServersInfoExtractor serversInfoExtractor) {
        super(serversInfoExtractor);
    }

    @Override
    protected void checkBootstrapServerInfo(ServerInfo bootstrapServerInfo, List<String> errors) {
        checkCertificateIsValid(bootstrapServerInfo, errors);
        checkOscoreIsValid(bootstrapServerInfo, errors);
    }

    @Override
    protected void checkDeviceMangementServerInfo(DmServerInfo serverInfo, List<String> errors) {
        checkCertificateIsValid(serverInfo, errors);
        checkOscoreIsValid(serverInfo, errors);
    }

    protected void checkCertificateIsValid(ServerInfo serverInfo, List<String> errors) {
        if (serverInfo.secureMode == SecurityMode.X509) {
            // the first certificate in a chain is a device certificate
            if (!X509CertUtil.canBeUsedForAuthentication((X509Certificate) serverInfo.clientCertificates[0], true)) {
                errors.add(String.format("Client certificate for %s server can not be used for authenticate a client",
                        serverInfo.bootstrap ? "bootstrap" : serverInfo.serverId));
            }
            // by default don't support chains!
            if (serverInfo.clientCertificates.length > 1) {
                errors.add("Client certificate chain for is not supported");
            }
        }
    }

    protected void checkOscoreIsValid(ServerInfo serverInfo, List<String> errors) {
        if (serverInfo.useOscore) {
            try {
                oscoreValidator.validateOscoreSetting(serverInfo.oscoreSetting);
            } catch (InvalidOscoreSettingException e) {
                errors.add(e.getMessage());
            }
        }
    }
}
