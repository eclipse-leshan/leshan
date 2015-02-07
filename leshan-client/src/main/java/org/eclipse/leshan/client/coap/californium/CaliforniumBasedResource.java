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

import org.eclipse.californium.core.coap.LinkFormat;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.client.resource.LinkFormattable;
import org.eclipse.leshan.client.resource.LwM2mClientResource;

class CaliforniumBasedResource extends CaliforniumBasedLwM2mNode<LwM2mClientResource> implements LinkFormattable {

    public CaliforniumBasedResource(final int id, final LwM2mClientResource lwM2mResource) {
        super(id, lwM2mResource);
    }

    @Override
    public void handlePOST(final CoapExchange exchange) {
        node.execute(new CaliforniumBasedLwM2mExchange(exchange));
    }

    @Override
    public String asLinkFormat() {
        final StringBuilder linkFormat = LinkFormat.serializeResource(this).append(
                LinkFormat.serializeAttributes(getAttributes()));

        linkFormat.deleteCharAt(linkFormat.length() - 1);

        return linkFormat.toString();
    }

}