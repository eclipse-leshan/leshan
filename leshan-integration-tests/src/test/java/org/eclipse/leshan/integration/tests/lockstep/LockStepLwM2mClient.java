/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.lockstep;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Random;

import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.serialization.UdpDataSerializer;
import org.eclipse.californium.core.test.lockstep.LockstepEndpoint;
import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.SystemConfig;
import org.eclipse.californium.elements.config.UdpConfig;
import org.eclipse.leshan.client.californium.request.CoapRequestBuilder;
import org.eclipse.leshan.core.californium.identity.DefaultCoapIdentityHandler;
import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.core.link.DefaultLinkSerializer;
import org.eclipse.leshan.core.link.LinkSerializer;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;

public class LockStepLwM2mClient extends LockstepEndpoint {

    private static final Random r = new Random();
    private final InetSocketAddress destination;
    private final LwM2mEncoder encoder;
    private final LwM2mModel model;
    private final LinkSerializer linkSerializer;

    public LockStepLwM2mClient(final URI destination) {
        this(EndpointUriUtil.getSocketAddr(destination));
    }

    public LockStepLwM2mClient(final InetSocketAddress destination) {
        super(destination, new Configuration(
                new Configuration(CoapConfig.DEFINITIONS, UdpConfig.DEFINITIONS, SystemConfig.DEFINITIONS)));
        this.destination = destination;
        this.encoder = new DefaultLwM2mEncoder();
        List<ObjectModel> models = ObjectLoader.loadDefault();
        this.model = new StaticModel(models);
        this.linkSerializer = new DefaultLinkSerializer();
    }

    public Request createCoapRequest(UplinkRequest<? extends LwM2mResponse> lwm2mReq) {
        // create CoAP request
        CoapRequestBuilder coapRequestBuilder = new CoapRequestBuilder(Identity.unsecure(destination), encoder, model,
                linkSerializer, new DefaultCoapIdentityHandler());
        lwm2mReq.accept(coapRequestBuilder);
        Request coapReq = coapRequestBuilder.getRequest();
        byte[] token = new byte[8];
        r.nextBytes(token);
        coapReq.setToken(token);
        coapReq.setMID(r.nextInt(Message.MAX_MID));
        return coapReq;
    }

    public Token sendLwM2mRequest(UplinkRequest<? extends LwM2mResponse> lwm2mReq) {
        return sendCoapRequest(createCoapRequest(lwm2mReq));
    }

    public Token sendCoapRequest(Request coapReq) {
        // serialize request
        UdpDataSerializer serializer = new UdpDataSerializer();
        RawData raw = serializer.serializeRequest(coapReq);

        // send it
        super.send(raw);
        return coapReq.getToken();
    }
}
