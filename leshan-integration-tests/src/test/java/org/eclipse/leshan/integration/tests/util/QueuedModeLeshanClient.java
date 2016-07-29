/*******************************************************************************
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
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
 *     Alexander Ellwein (Bosch Software Innovations GmbH)
 *                     - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.util;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.LwM2mId;
import org.eclipse.leshan.client.LwM2mClient;
import org.eclipse.leshan.client.californium.impl.BootstrapResource;
import org.eclipse.leshan.client.californium.impl.CaliforniumLwM2mClientRequestSender;
import org.eclipse.leshan.client.californium.impl.ObjectResource;
import org.eclipse.leshan.client.californium.impl.RootResource;
import org.eclipse.leshan.client.californium.impl.SecurityObjectPskStore;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.servers.BootstrapHandler;
import org.eclipse.leshan.client.servers.RegistrationEngine;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This client has a special ability to have its GET handler instrumented by a test by which different client scenarios
 * like sleep, wake up can be simulated.
 */
public class QueuedModeLeshanClient implements LwM2mClient {

    private static final Logger LOG = LoggerFactory.getLogger(QueuedModeLeshanClient.class);

    private CoapServer clientSideServer;
    private final CaliforniumLwM2mClientRequestSender requestSender;
    private final ConcurrentHashMap<Integer, LwM2mObjectEnabler> objectEnablers;
    private OnGetCallback onGetCallback = null;
    private CoapEndpoint secureEndpoint;
    private CoapEndpoint nonSecureEndpoint;
    private final RegistrationEngine engine;
    private final BootstrapHandler bootstrapHandler;

    private class CustomObjectResource extends ObjectResource {

        private LwM2mObjectEnabler nodeEnabler;

        public CustomObjectResource(LwM2mObjectEnabler nodeEnabler) {
            super(nodeEnabler, bootstrapHandler, new DefaultLwM2mNodeEncoder(), new DefaultLwM2mNodeDecoder());
            this.nodeEnabler = nodeEnabler;
            this.nodeEnabler.setNotifySender(this);
            setObservable(true);
        }

        @Override
        public void handleGET(CoapExchange exchange) {
            if (onGetCallback != null) {
                if (onGetCallback.handleGet(exchange)) {
                    super.handleGET(exchange);
                }
            } else {
                super.handleGET(exchange);
            }
        }
    }

    public interface OnGetCallback {

        /**
         * @return true, if the super implementation should be called, otherwise false.
         */
        boolean handleGet(CoapExchange coapExchange);
    }

    public QueuedModeLeshanClient(String endpoint, InetSocketAddress localAddress,
            InetSocketAddress localSecureAddress, List<? extends LwM2mObjectEnabler> objectEnablers) {
        Validate.notNull(localAddress);
        Validate.notNull(localSecureAddress);
        Validate.notNull(objectEnablers);
        Validate.notEmpty(objectEnablers);

        // Create Object enablers
        this.objectEnablers = new ConcurrentHashMap<>();
        for (LwM2mObjectEnabler enabler : objectEnablers) {
            if (this.objectEnablers.containsKey(enabler.getId())) {
                throw new IllegalArgumentException(String.format(
                        "There is several objectEnablers with the same id %d.", enabler.getId()));
            }
            this.objectEnablers.put(enabler.getId(), enabler);
        }

        // Create CoAP non secure endpoint
        nonSecureEndpoint = new CoapEndpoint(localAddress);

        // Create CoAP secure endpoint
        LwM2mObjectEnabler securityEnabler = this.objectEnablers.get(LwM2mId.SECURITY);
        if (securityEnabler == null) {
            throw new IllegalArgumentException("Security object is mandatory");
        }

        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(localSecureAddress);
        builder.setPskStore(new SecurityObjectPskStore(securityEnabler));
        secureEndpoint = new CoapEndpoint(new DTLSConnector(builder.build()), NetworkConfig.getStandard());

        // Create sender
        requestSender = new CaliforniumLwM2mClientRequestSender(secureEndpoint, nonSecureEndpoint);

        bootstrapHandler = new BootstrapHandler(this.objectEnablers);

        // Create registration engine
        engine = new RegistrationEngine(endpoint, this.objectEnablers, requestSender, bootstrapHandler, null);

        // Create CoAP Server
        clientSideServer = new CoapServer() {
            @Override
            protected Resource createRoot() {
                // Use to handle Delete on "/"
                return new RootResource(bootstrapHandler);
            }
        };
        clientSideServer.addEndpoint(secureEndpoint);
        clientSideServer.addEndpoint(nonSecureEndpoint);

        // Create custom CoAP resources for each lwm2m Objects.
        for (LwM2mObjectEnabler enabler : objectEnablers) {
            final ObjectResource clientObject = new CustomObjectResource(enabler);
            clientSideServer.add(clientObject);
        }

        // Create CoAP resources needed for the bootstrap sequence
        clientSideServer.add(new BootstrapResource(bootstrapHandler));

        // De-register on shutdown and stop client.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                QueuedModeLeshanClient.this.destroy(true);
            }
        });
    }

    @Override
    public void start() {
        LOG.info("Starting Leshan client ...");
        clientSideServer.start();
        engine.start();
        LOG.info("Leshan client started.");
    }

    @Override
    public void stop(boolean deregister) {
        LOG.info("Stopping Leshan Client ...");
        engine.stop(deregister);
        clientSideServer.stop();
        LOG.info("Leshan client stopped.");
    }

    @Override
    public void destroy(boolean deregister) {
        LOG.info("Destroying Leshan client ...");
        engine.stop(deregister);
        clientSideServer.destroy();
        LOG.info("Leshan client destroyed.");
    }

    @Override
    public Collection<LwM2mObjectEnabler> getObjectEnablers() {
        return Collections.unmodifiableCollection(objectEnablers.values());
    }

    public CoapServer getCoapServer() {
        return clientSideServer;
    }

    public InetSocketAddress getNonSecureAddress() {
        return nonSecureEndpoint.getAddress();
    }

    public InetSocketAddress getSecureAddress() {
        return secureEndpoint.getAddress();
    }

    public void setOnGetCallback(final OnGetCallback onGetCallback) {
        this.onGetCallback = onGetCallback;
    }
}
