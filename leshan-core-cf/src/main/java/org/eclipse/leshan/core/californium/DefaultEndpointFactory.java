/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
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
package org.eclipse.leshan.core.californium;

import java.net.InetSocketAddress;
import java.security.Principal;

import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.CoapEndpoint.Builder;
import org.eclipse.californium.core.network.EndpointContextMatcherFactory;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.observe.ObservationStore;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.EndpointContextMatcher;
import org.eclipse.californium.elements.PrincipalEndpointContextMatcher;
import org.eclipse.californium.elements.UDPConnector;
import org.eclipse.californium.oscore.HashMapCtxDB;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;

/**
 * A default implementation of {@link EndpointFactory}.
 * <p>
 * Extends this class to create a custom {@link EndpointFactory}.
 * <p>
 * E.g. This could be useful if you need to use a custom {@link Connector}.
 */
public class DefaultEndpointFactory implements EndpointFactory {

    protected EndpointContextMatcher securedContextMatcher;
    protected EndpointContextMatcher unsecuredContextMatcher;
    protected String loggingTag;

    public DefaultEndpointFactory() {
        this(null);
    }

    public DefaultEndpointFactory(String loggingTag) {
        this(loggingTag, false);
    }

    /**
     * @param loggingTag Logging tag
     * @param isClient Indication whether this factory is for client or for server.
     */
    public DefaultEndpointFactory(String loggingTag, boolean isClient) {
        securedContextMatcher = createSecuredContextMatcher(isClient);
        unsecuredContextMatcher = createUnsecuredContextMatcher();
        if (loggingTag != null) {
            this.loggingTag = loggingTag;
        }
    }

    /**
     * For server {@link Lwm2mEndpointContextMatcher} is created. <br>
     * For client {@link PrincipalEndpointContextMatcher} is created.
     * <p>
     * This method is intended to be overridden.
     * 
     * @return the {@link EndpointContextMatcher} used for secured communication
     * @param isClient Indication whether to use client side endpoint context matcher of server side.
     */
    protected EndpointContextMatcher createSecuredContextMatcher(boolean isClient) {
        if (isClient) {
            return new PrincipalEndpointContextMatcher() {
                @Override
                protected boolean matchPrincipals(Principal requestedPrincipal, Principal availablePrincipal) {
                    // As we are using 1 connector/endpoint by server at client side,
                    // and connector strongly limit connection from/to the expected foreign peer,
                    // we don't need to re-check principal at EndpointContextMatcher level.
                    return true;
                }
            };
        } else {
            return new Lwm2mEndpointContextMatcher();
        }
    }

    /**
     * By default the Californium default one is used. See {@link EndpointContextMatcherFactory} for more details.
     * <p>
     * This method is intended to be overridden.
     * 
     * @return the {@link EndpointContextMatcher} used for unsecured communication
     */
    protected EndpointContextMatcher createUnsecuredContextMatcher() {
        return null;
    }

    @Override
    public CoapEndpoint createUnsecuredEndpoint(InetSocketAddress address, NetworkConfig coapConfig,
            ObservationStore store, HashMapCtxDB db) {
        // TODO OSCORE : db should maybe be replaced by OscoreEStore
        return createUnsecuredEndpointBuilder(address, coapConfig, store).build();
    }

    /**
     * This method is intended to be overridden.
     * 
     * @param address the IP address and port, if null the connector is bound to an ephemeral port on the wildcard
     *        address.
     * @param coapConfig the CoAP config used to create this endpoint.
     * @param store the CoAP observation store used to create this endpoint.
     * @return the {@link Builder} used for unsecured communication.
     */
    protected CoapEndpoint.Builder createUnsecuredEndpointBuilder(InetSocketAddress address, NetworkConfig coapConfig,
            ObservationStore store) {
        CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
        builder.setConnector(createUnsecuredConnector(address));
        builder.setNetworkConfig(coapConfig);
        if (loggingTag != null) {
            builder.setLoggingTag("[" + loggingTag + "-coap://]");
        } else {
            builder.setLoggingTag("[coap://]");
        }
        if (unsecuredContextMatcher != null) {
            builder.setEndpointContextMatcher(unsecuredContextMatcher);
        }
        if (store != null) {
            builder.setObservationStore(store);
        }
        return builder;
    }

    /**
     * By default create an {@link UDPConnector}.
     * <p>
     * This method is intended to be overridden.
     * 
     * @param address the IP address and port, if null the connector is bound to an ephemeral port on the wildcard
     *        address
     * @return the {@link Connector} used for unsecured {@link CoapEndpoint}
     */
    protected Connector createUnsecuredConnector(InetSocketAddress address) {
        return new UDPConnector(address);
    }

    @Override
    public CoapEndpoint createSecuredEndpoint(DtlsConnectorConfig dtlsConfig, NetworkConfig coapConfig,
            ObservationStore store, HashMapCtxDB db) {
        // TODO OSCORE : db should maybe be replaced by OscoreEStore
        return createSecuredEndpointBuilder(dtlsConfig, coapConfig, store).build();
    }

    /**
     * This method is intended to be overridden.
     * 
     * @param dtlsConfig the DTLS config used to create this endpoint.
     * @param coapConfig the CoAP config used to create this endpoint.
     * @param store the CoAP observation store used to create this endpoint.
     * @return the {@link Builder} used for secured communication.
     */
    protected CoapEndpoint.Builder createSecuredEndpointBuilder(DtlsConnectorConfig dtlsConfig,
            NetworkConfig coapConfig, ObservationStore store) {
        CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
        builder.setConnector(createSecuredConnector(dtlsConfig));
        builder.setNetworkConfig(coapConfig);
        if (loggingTag != null) {
            builder.setLoggingTag("[" + loggingTag + "-coaps://]");
        } else {
            builder.setLoggingTag("[coaps://]");
        }
        if (securedContextMatcher != null) {
            builder.setEndpointContextMatcher(securedContextMatcher);
        }
        if (store != null) {
            builder.setObservationStore(store);
        }
        return builder;
    }

    /**
     * By default create a {@link DTLSConnector}.
     * <p>
     * This method is intended to be overridden.
     * 
     * @param dtlsConfig the DTLS config used to create the Secured Connector.
     * @return the {@link Connector} used for unsecured {@link CoapEndpoint}
     */
    protected Connector createSecuredConnector(DtlsConnectorConfig dtlsConfig) {
        return new DTLSConnector(dtlsConfig);
    }
}
