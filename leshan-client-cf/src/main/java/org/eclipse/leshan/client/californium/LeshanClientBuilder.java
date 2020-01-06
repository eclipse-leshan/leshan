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
import java.util.Map;

import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.config.NetworkConfig.Keys;
import org.eclipse.californium.elements.EndpointContextMatcher;
import org.eclipse.californium.elements.UDPConnector;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.leshan.LwM2mId;
import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.californium.DefaultEndpointFactory;
import org.eclipse.leshan.core.californium.EndpointFactory;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.util.Validate;

/**
 * Helper class to build and configure a Californium based Leshan Lightweight M2M client.
 */
public class LeshanClientBuilder {

    private final String endpoint;

    private InetSocketAddress localAddress;
    private List<? extends LwM2mObjectEnabler> objectEnablers;

    private NetworkConfig coapConfig;
    private Builder dtlsConfigBuilder;

    private LwM2mNodeEncoder encoder;
    private LwM2mNodeDecoder decoder;

    private EndpointFactory endpointFactory;
    private Map<String, String> additionalAttributes;

    /**
     * Creates a new instance for setting the configuration options for a {@link LeshanClient} instance.
     * 
     * The builder is initialized with the following default values:
     * <ul>
     * <li><em>local address</em>: a local address and an ephemeral port (picked up during binding)</li>
     * <li><em>object enablers</em>:
     * <ul>
     * <li>Security(0) with one instance (DM server security): uri=<em>coap://leshan.eclipseprojects.io:5683</em>,
     * mode=NoSec</li>
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
     * <p>
     * Sets the list of objects enablers
     */
    public LeshanClientBuilder setObjects(List<? extends LwM2mObjectEnabler> objectEnablers) {
        this.objectEnablers = objectEnablers;
        return this;
    }

    /**
     * Set the {@link LwM2mNodeEncoder} which will encode {@link LwM2mNode} with supported content format.
     * <p>
     * By default the {@link DefaultLwM2mNodeEncoder} is used. It supports Text, Opaque, TLV and JSON format.
     */
    public LeshanClientBuilder setEncoder(LwM2mNodeEncoder encoder) {
        this.encoder = encoder;
        return this;
    }

    /**
     * Set the {@link LwM2mNodeDecoder} which will decode data in supported content format to create {@link LwM2mNode}.
     * <p>
     * By default the {@link DefaultLwM2mNodeDecoder} is used. It supports Text, Opaque, TLV and JSON format.
     */
    public LeshanClientBuilder setDecoder(LwM2mNodeDecoder decoder) {
        this.decoder = decoder;
        return this;
    }

    /**
     * Set the Californium/CoAP {@link NetworkConfig}.
     */
    public LeshanClientBuilder setCoapConfig(NetworkConfig config) {
        this.coapConfig = config;
        return this;
    }

    /**
     * Set the Scandium/DTLS Configuration : {@link DtlsConnectorConfig}.
     */
    public LeshanClientBuilder setDtlsConfig(DtlsConnectorConfig.Builder config) {
        this.dtlsConfigBuilder = config;
        return this;
    }

    /**
     * Advanced setter used to create custom CoAP endpoint.
     * <p>
     * An {@link UDPConnector} is expected for unsecured endpoint and a {@link DTLSConnector} is expected for secured
     * endpoint.
     * 
     * @param endpointFactory An {@link EndpointFactory}, you can extends {@link DefaultEndpointFactory}.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public LeshanClientBuilder setEndpointFactory(EndpointFactory endpointFactory) {
        this.endpointFactory = endpointFactory;
        return this;
    }

    /**
     * Set the additionalAttributes for {@link org.eclipse.leshan.core.request.RegisterRequest}.
     */
    public LeshanClientBuilder setAdditionalAttributes(Map<String, String> additionalAttributes) {
        this.additionalAttributes = additionalAttributes;
        return this;
    }

    public static NetworkConfig createDefaultNetworkConfig() {
        NetworkConfig networkConfig = new NetworkConfig();
        networkConfig.set(Keys.MID_TRACKER, "NULL");
        networkConfig.set(Keys.MAX_ACTIVE_PEERS, 10);
        networkConfig.set(Keys.PROTOCOL_STAGE_THREAD_COUNT, 1);

        return networkConfig;
    }

    /**
     * Creates an instance of {@link LeshanClient} based on the properties set on this builder.
     */
    public LeshanClient build() {
        if (localAddress == null) {
            localAddress = new InetSocketAddress(0);
        }
        if (objectEnablers == null) {
            ObjectsInitializer initializer = new ObjectsInitializer();
            initializer.setInstancesForObject(LwM2mId.SECURITY,
                    Security.noSec("coap://leshan.eclipseprojects.io:5683", 12345));
            initializer.setInstancesForObject(LwM2mId.SERVER, new Server(12345, 5 * 60, BindingMode.U, false));
            initializer.setInstancesForObject(LwM2mId.DEVICE, new Device("Eclipse Leshan", "model12345", "12345", "U"));
            objectEnablers = initializer.createAll();
        }
        if (encoder == null)
            encoder = new DefaultLwM2mNodeEncoder();
        if (decoder == null)
            decoder = new DefaultLwM2mNodeDecoder();
        if (coapConfig == null) {
            coapConfig = createDefaultNetworkConfig();
        }
        if (endpointFactory == null) {
            endpointFactory = new DefaultEndpointFactory("LWM2M Client") {
                @Override
                protected EndpointContextMatcher createSecuredContextMatcher() {
                    return null; // use default californium one.
                }
            };
        }

        // handle dtlsConfig
        if (dtlsConfigBuilder == null) {
            dtlsConfigBuilder = new DtlsConnectorConfig.Builder();
        }
        DtlsConnectorConfig incompleteConfig = dtlsConfigBuilder.getIncompleteConfig();

        // Handle secure address
        if (incompleteConfig.getAddress() == null) {
            if (localAddress == null) {
                localAddress = new InetSocketAddress(0);
            }
            dtlsConfigBuilder.setAddress(localAddress);
        } else if (localAddress != null && !localAddress.equals(incompleteConfig.getAddress())) {
            throw new IllegalStateException(String.format(
                    "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for address: %s != %s",
                    localAddress, incompleteConfig.getAddress()));
        }

        // Handle active peers
        if (incompleteConfig.getMaxConnections() == null)
            dtlsConfigBuilder.setMaxConnections(coapConfig.getInt(Keys.MAX_ACTIVE_PEERS));
        if (incompleteConfig.getStaleConnectionThreshold() == null)
            dtlsConfigBuilder.setStaleConnectionThreshold(coapConfig.getLong(Keys.MAX_PEER_INACTIVITY_PERIOD));

        // Use only 1 thread to handle DTLS connection by default
        if (incompleteConfig.getConnectionThreadCount() == null) {
            dtlsConfigBuilder.setConnectionThreadCount(1);
        }

        // Deactivate SNI by default
        // TODO should we support SNI ?
        if (incompleteConfig.isSniEnabled() == null) {
            dtlsConfigBuilder.setSniEnabled(false);
        }

        return new LeshanClient(endpoint, localAddress, objectEnablers, coapConfig, dtlsConfigBuilder, endpointFactory,
                additionalAttributes, encoder, decoder);
    }
}
