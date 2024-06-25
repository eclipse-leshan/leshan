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
package org.eclipse.leshan.core.californium;

import java.security.Principal;

import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.MdcConnectionListener;
import org.eclipse.californium.scandium.dtls.Connection;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.slf4j.MDC;

/**
 * An {@link MdcConnectionListener} which also display {@link Principal}
 */
public class PrincipalMdcConnectionListener extends MdcConnectionListener {
    @Override
    public void beforeExecution(Connection connection) {
        if (DTLSConnector.MDC_SUPPORT) {
            DTLSSession session = connection.getSession();
            if (session != null) {
                Principal pincipal = session.getPeerIdentity();
                if (pincipal != null) {
                    MDC.put("PRINCIPAL", pincipal.toString());
                }
            }
        }
        super.beforeExecution(connection);
    }
}
