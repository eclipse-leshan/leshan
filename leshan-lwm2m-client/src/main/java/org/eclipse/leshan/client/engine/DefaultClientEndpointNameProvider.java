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
package org.eclipse.leshan.client.engine;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.security.cert.X509Certificate;

import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.security.certificate.util.X509CertUtil;
import org.eclipse.leshan.core.util.Validate;

/**
 * This is default implementation of {@link ClientEndpointNameProvider}.
 * <p>
 * This {@link ClientEndpointNameProvider} can control if Client Endpoint Name should be send
 *
 * @see <a href="https://github.com/eclipse-leshan/leshan/issues/1457"></a>
 */
public class DefaultClientEndpointNameProvider implements ClientEndpointNameProvider {

    public enum Mode {
        /**
         * Always provide/send endpoint name for all request. Most safe and interoperable mode.
         */
        ALWAYS,
        /**
         * Never provide/send endpoint name for all request. Usefull for test but must probably not be used in
         * production.
         */
        NEVER,
        /**
         * Provide/send endpoint name for all request only if necessary. It should be not necessary if it can be guessed
         * by Server. Allow to save some byte on corresponding request but could lead to some interoperability issue and
         * also maybe some privacy one as your endpoint will be visible in clear (note this is already the case for X509
         * use case).
         *
         * @see <a href="https://github.com/eclipse-leshan/leshan/issues/1457"></a>
         */
        IF_NECESSARY
    }

    private final String endpoint;
    private final Mode mode;

    public DefaultClientEndpointNameProvider(String endpointName) {
        this(endpointName, Mode.ALWAYS);
    }

    public DefaultClientEndpointNameProvider(String endpointName, Mode mode) {
        Validate.notEmpty(endpointName);
        Validate.notNull(mode);
        this.endpoint = endpointName;
        this.mode = mode;
    }

    @Override
    public String getEndpointName() {
        return endpoint;
    }

    @Override
    public String getEndpointNameFor(ServerInfo serverInfo, Class<? extends UplinkRequest<?>> requestType) {
        switch (mode) {
        case ALWAYS:
            return endpoint;
        case NEVER:
            return null;
        case IF_NECESSARY:
            String identifier = getClientSecurityProtocolIdentifier(serverInfo);
            if (identifier != null && identifier.equals(endpoint)) {
                return null;
            } else {
                return endpoint;
            }
        default:
            throw new IllegalStateException(String.format("mode %s is not supported", mode));
        }
    }

    protected String getClientSecurityProtocolIdentifier(ServerInfo serverInfo) {
        if (serverInfo.useOscore) {
            byte[] senderId = serverInfo.oscoreSetting.getSenderId();
            // Try to convert byte array in UTF8 String
            try {
                CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
                ByteBuffer buf = ByteBuffer.wrap(senderId);
                return decoder.decode(buf).toString();
            } catch (CharacterCodingException e) {
                return null;
            }
        } else {
            switch (serverInfo.secureMode) {
            case PSK:
                return serverInfo.pskId;
            case X509:
                return X509CertUtil.extractCN(((X509Certificate) serverInfo.clientCertificate).getIssuerDN().getName());
            default:
                return null;
            }
        }
    }
}
