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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.coap.californium;

import java.util.Map.Entry;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.LinkFormat;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.leshan.client.resource.LinkFormattable;
import org.eclipse.leshan.client.resource.LwM2mClientObject;
import org.eclipse.leshan.client.resource.LwM2mClientObjectInstance;
import org.eclipse.leshan.client.resource.LwM2mClientResource;

public class CaliforniumBasedObjectInstance extends CaliforniumBasedLwM2mNode<LwM2mClientObjectInstance> implements
        LinkFormattable {

    public CaliforniumBasedObjectInstance(final int instanceId, final LwM2mClientObjectInstance instance) {
        super(instanceId, instance);
        for (final Entry<Integer, LwM2mClientResource> entry : instance.getAllResources().entrySet()) {
            final Integer resourceId = entry.getKey();
            final LwM2mClientResource resource = entry.getValue();
            add(new CaliforniumBasedResource(resourceId, resource));
        }
    }

    @Override
    public void handleDELETE(final CoapExchange exchange) {
        node.delete(new CaliforniumBasedLwM2mCallbackExchange<LwM2mClientObject>(exchange,
                new Callback<LwM2mClientObject>() {

                    @Override
                    public void onSuccess(LwM2mClientObject object) {
                        getParent().remove(CaliforniumBasedObjectInstance.this);
                    }

                    @Override
                    public void onFailure() {
                    }

                }));

        exchange.respond(ResponseCode.DELETED);
    }

    @Override
    public String asLinkFormat() {
        final StringBuilder linkFormat = LinkFormat.serializeResource(this).append(
                LinkFormat.serializeAttributes(getAttributes()));
        for (final Resource child : getChildren()) {
            linkFormat.append(LinkFormat.serializeResource(child));
        }
        linkFormat.deleteCharAt(linkFormat.length() - 1);

        return linkFormat.toString();
    }

}