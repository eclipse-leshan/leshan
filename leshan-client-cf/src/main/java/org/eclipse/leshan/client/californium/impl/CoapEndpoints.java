/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
package org.eclipse.leshan.client.californium.impl;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.observer.LwM2mClientObserverAdapter;
import org.eclipse.leshan.client.observer.LwM2mClientObserverDispatcher;
import org.eclipse.leshan.client.servers.DmServerInfo;
import org.eclipse.leshan.client.servers.ServerInfo;

public class CoapEndpoints {

    private final CoapServer clientSideServer;
    private final NetworkConfig coapConfig;

    private CoapEndpoint nonSecureEndpoint;
    private CoapEndpoint secureEndpoint;

    private final LwM2mClientObserverDispatcher observers;

    public CoapEndpoints(CoapServer clientSideServer, InetSocketAddress localAddress, DtlsConnectorConfig dtlsConfig,
            NetworkConfig coapConfig, LwM2mClientObserverDispatcher observers) {
        this.clientSideServer = clientSideServer;
        this.coapConfig = coapConfig;
        this.observers = observers;

        // initialize both secure and non-secure endpoints
        nonSecureEndpoint = new CoapEndpoint(localAddress, coapConfig);
        clientSideServer.addEndpoint(nonSecureEndpoint);

        secureEndpoint = new SecureCoapEndoint(new DTLSConnector(dtlsConfig), coapConfig);
        clientSideServer.addEndpoint(secureEndpoint);
        observers.addObserver(((SecureCoapEndoint) secureEndpoint).clearConnectionObserver);
    }

    public CoapEndpoint getNonSecureEndpoint() {
        return nonSecureEndpoint;
    }

    public CoapEndpoint getSecureEndpoint() {
        return secureEndpoint;
    }

    public NetworkConfig getNetworkConfig() {
        return coapConfig;
    }

    public void recreateSecureEndpoint(DtlsConnectorConfig dtlsConfig) throws IOException {

        // remove old secure end-point
        secureEndpoint.destroy();
        observers.removeObserver(((SecureCoapEndoint) secureEndpoint).clearConnectionObserver);
        clientSideServer.getEndpoints().remove(secureEndpoint);

        // and create the new one
        secureEndpoint = new SecureCoapEndoint(new DTLSConnector(dtlsConfig), coapConfig);
        clientSideServer.addEndpoint(secureEndpoint);
        observers.addObserver(((SecureCoapEndoint) secureEndpoint).clearConnectionObserver);
        secureEndpoint.start();
    }

    private static class SecureCoapEndoint extends CoapEndpoint {

        private LwM2mClientObserver clearConnectionObserver;

        public SecureCoapEndoint(final DTLSConnector dtlsConnector, NetworkConfig config) {
            super(dtlsConnector, config);

            this.clearConnectionObserver = new LwM2mClientObserverAdapter() {
                @Override
                public void onBootstrapSuccess(ServerInfo bsserver) {
                    dtlsConnector.clearConnectionState();
                }

                @Override
                public void onBootstrapTimeout(ServerInfo bsserver) {
                    dtlsConnector.clearConnectionState();
                }

                @Override
                public void onRegistrationTimeout(DmServerInfo server) {
                    dtlsConnector.clearConnectionState();
                }

                @Override
                public void onUpdateTimeout(DmServerInfo server) {
                    dtlsConnector.clearConnectionState();
                }
            };
        }

    }

}
