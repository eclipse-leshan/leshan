/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.javacoap.server.coaptcp.endpoint;

import javax.security.auth.x500.X500Principal;

import org.eclipse.leshan.core.security.certificate.util.X509CertUtil;
import org.eclipse.leshan.transport.javacoap.identity.TlsTransportContextKeys;
import org.eclipse.leshan.transport.javacoap.server.coaptcp.transport.DefaultTransportContextMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mbed.coap.transport.TransportContext.Key;

public class LwM2mTransportContextMatcher extends DefaultTransportContextMatcher {

    private final Logger LOG = LoggerFactory.getLogger(LwM2mTransportContextMatcher.class);

    @Override
    protected boolean matches(Key<?> key, Object packetValue, Object channelValue) {
        if (key.equals(TlsTransportContextKeys.PRINCIPAL)) {
            if (packetValue instanceof X500Principal && channelValue instanceof X500Principal) {
                try {
                    String requestedCommonName = X509CertUtil.extractCN(((X500Principal) packetValue));
                    String availableCommonName = X509CertUtil.extractCN(((X500Principal) channelValue));
                    return requestedCommonName.equals(availableCommonName);
                } catch (IllegalStateException e) {
                    LOG.debug("Unable to extract CN from certificate {} or {}", packetValue, channelValue);
                    return false;
                }
            } else {
                LOG.debug("Unsupported kind of principal {} or {}", packetValue.getClass().getSimpleName(),
                        channelValue.getClass().getSimpleName());
                return false;
            }
        } else {
            return super.matches(key, packetValue, channelValue);
        }
    }
}
