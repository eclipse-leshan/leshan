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
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.leshan.client.resource.LwM2mClientObject;
import org.eclipse.leshan.client.resource.LwM2mClientObjectDefinition;
import org.eclipse.leshan.client.resource.LwM2mClientObjectInstance;

public class CaliforniumBasedObject extends CaliforniumBasedLwM2mNode<LwM2mClientObject> {

    public CaliforniumBasedObject(final LwM2mClientObjectDefinition def) {
        super(def.getId(), new LwM2mClientObject(def));

        if (def.isMandatory()) {
            createMandatoryObjectInstance(def);
        }
    }
    
    private void createMandatoryObjectInstance(final LwM2mClientObjectDefinition def) {
        LwM2mClientObjectInstance instance = node.createMandatoryInstance();
        onSuccessfulCreate(instance);
    }

    @Override
    public void handlePOST(final CoapExchange exchange) {
        node.createInstance(new CaliforniumBasedLwM2mCallbackExchange<LwM2mClientObjectInstance>(exchange,
                getCreateCallback()));
    }

    private Callback<LwM2mClientObjectInstance> getCreateCallback() {
        return new Callback<LwM2mClientObjectInstance>() {

            @Override
            public void onSuccess(final LwM2mClientObjectInstance newInstance) {
                onSuccessfulCreate(newInstance);
            }

            @Override
            public void onFailure() {
            }

        };
    }

    public void onSuccessfulCreate(final LwM2mClientObjectInstance instance) {
        add(new CaliforniumBasedObjectInstance(instance.getId(), instance));
        node.onSuccessfulCreate(instance);
    }

    @Override
    public String asLinkFormat() {
        final StringBuilder linkFormat = LinkFormat.serializeResource(this).append(
                LinkFormat.serializeAttributes(getAttributes()));
        for (final Resource child : getChildren()) {
            for (final Resource grandchild : child.getChildren()) {
                linkFormat.append(LinkFormat.serializeResource(grandchild));
            }
        }
        linkFormat.deleteCharAt(linkFormat.length() - 1);
        return linkFormat.toString();
    }

}
