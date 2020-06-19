/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.californium.request;

import java.util.HashMap;
import java.util.Map.Entry;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.californium.EndpointContextUtil;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.request.UplinkRequestVisitor;

/**
 * This class is able to create CoAP request from LWM2M {@link UplinkRequest}.
 * <p>
 * Call <code>CoapRequestBuilder#visit(lwm2mRequest)</code>, then get the result using {@link #getRequest()}
 */
public class CoapRequestBuilder implements UplinkRequestVisitor {

    protected Request coapRequest;
    protected final Identity server;

    public CoapRequestBuilder(Identity server) {
        this.server = server;
    }

    @Override
    public void visit(BootstrapRequest request) {
        coapRequest = Request.newPost();
        buildRequestSettings();
        coapRequest.getOptions().addUriPath("bs");

        // @since 1.1
        HashMap<String, String> attributes = new HashMap<>();
        attributes.putAll(request.getAdditionalAttributes());
        attributes.put("ep", request.getEndpointName());
        for (Entry<String, String> attr : attributes.entrySet()) {
            coapRequest.getOptions().addUriQuery(attr.getKey() + "=" + attr.getValue());
        }
    }

    @Override
    public void visit(RegisterRequest request) {
        coapRequest = Request.newPost();
        buildRequestSettings();
        coapRequest.getOptions().setContentFormat(ContentFormat.LINK.getCode());
        coapRequest.getOptions().addUriPath("rd");

        HashMap<String, String> attributes = new HashMap<>();
        attributes.putAll(request.getAdditionalAttributes());

        attributes.put("ep", request.getEndpointName());

        Long lifetime = request.getLifetime();
        if (lifetime != null)
            attributes.put("lt", lifetime.toString());

        String smsNumber = request.getSmsNumber();
        if (smsNumber != null)
            attributes.put("sms", smsNumber);

        String lwVersion = request.getLwVersion();
        if (lwVersion != null)
            attributes.put("lwm2m", lwVersion);

        BindingMode bindingMode = request.getBindingMode();
        if (bindingMode != null)
            attributes.put("b", bindingMode.toString());

        for (Entry<String, String> attr : attributes.entrySet()) {
            coapRequest.getOptions().addUriQuery(attr.getKey() + "=" + attr.getValue());
        }

        Link[] objectLinks = request.getObjectLinks();
        if (objectLinks != null)
            coapRequest.setPayload(Link.serialize(objectLinks));

    }

    @Override
    public void visit(UpdateRequest request) {
        coapRequest = Request.newPost();
        buildRequestSettings();
        coapRequest.getOptions().setUriPath(request.getRegistrationId());

        Long lifetime = request.getLifeTimeInSec();
        if (lifetime != null)
            coapRequest.getOptions().addUriQuery("lt=" + lifetime);

        String smsNumber = request.getSmsNumber();
        if (smsNumber != null)
            coapRequest.getOptions().addUriQuery("sms=" + smsNumber);

        BindingMode bindingMode = request.getBindingMode();
        if (bindingMode != null)
            coapRequest.getOptions().addUriQuery("b=" + bindingMode.toString());

        Link[] linkObjects = request.getObjectLinks();
        if (linkObjects != null) {
            coapRequest.getOptions().setContentFormat(ContentFormat.LINK.getCode());
            coapRequest.setPayload(Link.serialize(linkObjects));
        }
    }

    @Override
    public void visit(DeregisterRequest request) {
        coapRequest = Request.newDelete();
        buildRequestSettings();
        coapRequest.getOptions().setUriPath(request.getRegistrationId());
    }

    public Request getRequest() {
        return coapRequest;
    }

    protected void buildRequestSettings() {
        EndpointContext context = EndpointContextUtil.extractContext(server, true);
        coapRequest.setDestinationContext(context);
    }
}
