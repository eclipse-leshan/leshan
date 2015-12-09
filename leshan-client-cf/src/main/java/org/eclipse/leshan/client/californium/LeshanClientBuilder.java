/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.californium;

import java.net.InetSocketAddress;
import java.util.List;

import org.eclipse.leshan.LwM2mId;
import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.util.Validate;

/**
 * Helper class to build and configure a Californium based Leshan Lightweight M2M client.
 */
public class LeshanClientBuilder {

    private final String endpoint;

    private InetSocketAddress localAddress;
    private InetSocketAddress localSecureAddress;
    private List<? extends LwM2mObjectEnabler> objectEnablers;

    /**
     * Creates a new instance for setting the configuration options for a {@link LeshanClient} instance.
     * 
     * The builder is initialized with the following default values:
     * <ul>
     * <li><em>local address</em>: a local address and an ephemeral port (picked up during binding)</li>
     * <li><em>local secure address</em>: a local address and an ephemeral port (picked up during binding)</li>
     * <li><em>object enablers</em>:
     * <ul>
     * <li>Security(0) with one instance (DM server security): uri=<em>coap://leshan.eclipse.org:5683</em>, mode=NoSec</li>
     * <li>Server(1) with one instance (DM server): id=12345, lifetime=5minutes</li>
     * <li>Device(3): manufacturer=Eclipse Leshan, modelNumber=model12345, serialNumber=12345</li>
     * </ul>
     * </li>
     * </ul>
     * 
     * @param endpoint the end-point to identify the client on the server
     */
    public LeshanClientBuilder(String endpoint) {
        Validate.notEmpty(endpoint);
        this.endpoint = endpoint;
    }

    /**
     * Sets the local non-secure end-point address
     */
    public LeshanClientBuilder setLocalAddress(String hostname, int port) {
        if (hostname == null) {
            this.localAddress = new InetSocketAddress(port);
        } else {
            this.localAddress = new InetSocketAddress(hostname, port);
        }
        return this;
    }

    /**
     * Sets the local secure end-point address
     */
    public LeshanClientBuilder setLocalSecureAddress(String hostname, int port) {
        if (hostname == null) {
            this.localSecureAddress = new InetSocketAddress(port);
        } else {
            this.localSecureAddress = new InetSocketAddress(hostname, port);
        }
        return this;
    }

    /**
     * Sets the list of objects enablers
     */
    public LeshanClientBuilder setObjects(List<? extends LwM2mObjectEnabler> objectEnablers) {
        this.objectEnablers = objectEnablers;
        return this;
    }

    /**
     * Creates an instance of {@link LeshanClient} based on the properties set on this builder.
     */
    public LeshanClient build() {
        if (localAddress == null) {
            localAddress = new InetSocketAddress(0);
        }
        if (localSecureAddress == null) {
            localSecureAddress = new InetSocketAddress(0);
        }
        if (objectEnablers == null) {
            ObjectsInitializer initializer = new ObjectsInitializer();
            initializer
                    .setInstancesForObject(LwM2mId.SECURITY, Security.noSec("coap://leshan.eclipse.org:5683", 12345));
            initializer.setInstancesForObject(LwM2mId.SERVER, new Server(12345, 5 * 60, BindingMode.U, false));
            initializer.setInstancesForObject(LwM2mId.DEVICE, new Device("Eclipse Leshan", "model12345", "12345", "U"));
            objectEnablers = initializer.createMandatory();
        }
        return new LeshanClient(endpoint, localAddress, localSecureAddress, objectEnablers);
    }
}
