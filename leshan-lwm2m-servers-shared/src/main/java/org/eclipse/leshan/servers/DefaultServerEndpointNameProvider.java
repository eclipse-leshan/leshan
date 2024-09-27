/*******************************************************************************
 * Copyright (c) 2024 Semtech and others.
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
 *     Semtech - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.servers;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.eclipse.leshan.core.peer.LwM2mIdentity;
import org.eclipse.leshan.core.peer.OscoreIdentity;
import org.eclipse.leshan.core.peer.PskIdentity;
import org.eclipse.leshan.core.peer.X509Identity;

/**
 * This is default implementation of {@link ServerEndpointNameProvider}.
 * <p>
 * This {@link ServerEndpointNameProvider} can guess endpoint name for PSK, X509 and OSCORE but not for NO_SEC or RPK.
 *
 * @see <a href="https://github.com/eclipse-leshan/leshan/issues/1457"></a>
 */
public class DefaultServerEndpointNameProvider implements ServerEndpointNameProvider {

    /**
     * {@inheritDoc}
     *
     * <ul>
     * <li>If PSK is used PSK Identity will be considered as endpoint name.
     * <li>If X509 is used Certificate CN will be considered as endpoint name.
     * <li>If OSCORE is used Sender ID (at client side) will be considered as endpoint name. (only if Sender ID can be
     * converted in UTF8 String)
     * <li>else <code>null</code> will be returned
     * </ul>
     */
    @Override
    public String getEndpointName(LwM2mIdentity clientIdentity) {
        if (clientIdentity instanceof PskIdentity) {
            return ((PskIdentity) clientIdentity).getPskIdentity();
        } else if (clientIdentity instanceof X509Identity) {
            return ((X509Identity) clientIdentity).getX509CommonName();
        } else if (clientIdentity instanceof OscoreIdentity) {
            // Recipient ID at server side is Sender ID at client side
            // Try to convert byte array in UTF8 String
            try {
                CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
                ByteBuffer buf = ByteBuffer.wrap(((OscoreIdentity) clientIdentity).getRecipientId());
                return decoder.decode(buf).toString();
            } catch (CharacterCodingException e) {
                return null;
            }
        }
        return null;
    }
}
