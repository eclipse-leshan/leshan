/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests.util;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLinkParser;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.server.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumServerEndpointsProvider;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumServerEndpointsProvider.Builder;
import org.eclipse.leshan.server.californium.endpoint.coap.CoapServerProtocolProvider;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpointsProvider;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.VersionedModelProvider;
import org.eclipse.leshan.server.queue.ClientAwakeTimeProvider;
import org.eclipse.leshan.server.registration.RegistrationIdProvider;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.security.Authorizer;
import org.eclipse.leshan.server.security.SecurityStore;
import org.eclipse.leshan.server.security.ServerSecurityInfo;
import org.eclipse.leshan.transport.javacoap.endpoint.JavaCoapServerEndpointsProvider;

public class LeshanTestServerBuilder extends LeshanServerBuilder {

    private final Protocol protocolToUse;

    public LeshanTestServerBuilder(Protocol protocolToUse) {
        this.protocolToUse = protocolToUse;
        this.setDecoder(new DefaultLwM2mDecoder(true));
        this.setEncoder(new DefaultLwM2mEncoder(true));
        this.setObjectModelProvider(new VersionedModelProvider(TestObjectLoader.loadDefaultObject()));
    }

    @Override
    public LeshanTestServer build() {
        return (LeshanTestServer) super.build();
    }

    @Override
    protected LeshanTestServer createServer(LwM2mServerEndpointsProvider endpointsProvider,
            RegistrationStore registrationStore, SecurityStore securityStore, Authorizer authorizer,
            LwM2mModelProvider modelProvider, LwM2mEncoder encoder, LwM2mDecoder decoder, boolean noQueueMode,
            ClientAwakeTimeProvider awakeTimeProvider, RegistrationIdProvider registrationIdProvider,
            LwM2mLinkParser linkParser, ServerSecurityInfo serverSecurityInfo,
            boolean updateRegistrationOnNotification) {

        return new LeshanTestServer(endpointsProvider, registrationStore, securityStore, authorizer, modelProvider,
                encoder, decoder, noQueueMode, awakeTimeProvider, registrationIdProvider, linkParser,
                serverSecurityInfo, updateRegistrationOnNotification);
    }

    public LeshanTestServerBuilder with(String endpointProvider) {
        switch (endpointProvider) {
        case "Californium":
            Builder builder = new CaliforniumServerEndpointsProvider.Builder(new CoapServerProtocolProvider());
            builder.addEndpoint(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), protocolToUse);
            this.setEndpointsProvider(builder.build());
            return this;
        case "java-coap":
            this.setEndpointsProvider(
                    new JavaCoapServerEndpointsProvider(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0)));
            return this;
        default:
            throw new IllegalStateException(String.format("Unknown endpoint provider : [%s]", endpointProvider));
        }
    }

    public static LeshanTestServerBuilder givenServerUsing(Protocol protocolToUse) {
        return new LeshanTestServerBuilder(protocolToUse);
    }
}
