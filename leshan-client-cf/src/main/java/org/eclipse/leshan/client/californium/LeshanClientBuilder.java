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

import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.config.NetworkConfig.Keys;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.leshan.LwM2mId;
import org.eclipse.leshan.client.californium.impl.SecurityObjectPskStore;
import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.californium.EndpointFactory;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to build and configure a Californium based Leshan Lightweight M2M client.
 */
public class LeshanClientBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(LeshanClientBuilder.class);

    private final String endpoint;

    private InetSocketAddress localAddress;
    private InetSocketAddress localSecureAddress;
    private List<? extends LwM2mObjectEnabler> objectEnablers;

    private NetworkConfig coapConfig;
    private Builder dtlsConfigBuilder;

    private boolean noSecuredEndpoint;
    private boolean noUnsecuredEndpoint;

    private EndpointFactory endpointFactory;
    private Map<String, String> additionalAttributes;

    /**
     * Creates a new instance for setting the configuration options for a {@link LeshanClient} instance.
     * 
     * The builder is initialized with the following default values:
     * <ul>
     * <li><em>local address</em>: a local address and an ephemeral port (picked up during binding)</li>
     * <li><em>local secure address</em>: a local address and an ephemeral port (picked up during binding)</li>
     * <li><em>object enablers</em>:
     * <ul>
     * <li>Security(0) with one instance (DM server security): uri=<em>coap://leshan.eclipse.org:5683</em>, mode=NoSec
     * </li>
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
     * <p>
     * Sets the list of objects enablers
     * </p>
     * Warning : The Security ObjectEnabler should not contains 2 or more entries with the same identity. This is not a
     * LWM2M specification constraint but an implementation limitation.
     */
    public LeshanClientBuilder setObjects(List<? extends LwM2mObjectEnabler> objectEnablers) {
        this.objectEnablers = objectEnablers;
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
     * Used to create custom CoAP endpoint, this is only for advanced users. <br>
     * DTLSConnector is expected for secured endpoint.
     */
    public LeshanClientBuilder setEndpointFactory(EndpointFactory endpointFactory) {
        this.endpointFactory = endpointFactory;
        return this;
    }

    /**
     * deactivate unsecured CoAP endpoint
     */
    public LeshanClientBuilder disableUnsecuredEndpoint() {
        this.noUnsecuredEndpoint = true;
        return this;
    }

    /**
     * deactivate secured CoAP endpoint (DTLS)
     */
    public LeshanClientBuilder disableSecuredEndpoint() {
        this.noSecuredEndpoint = true;
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
                    Security.noSec("coap://leshan.eclipse.org:5683", 12345));
            initializer.setInstancesForObject(LwM2mId.SERVER, new Server(12345, 5 * 60, BindingMode.U, false));
            initializer.setInstancesForObject(LwM2mId.DEVICE, new Device("Eclipse Leshan", "model12345", "12345", "U"));
            objectEnablers = initializer.createMandatory();
        }
        if (coapConfig == null) {
            coapConfig = createDefaultNetworkConfig();
        }

        // handle dtlsConfig
        DtlsConnectorConfig dtlsConfig = null;
        if (dtlsConfigBuilder == null) {
            dtlsConfigBuilder = new DtlsConnectorConfig.Builder();
        }
        DtlsConnectorConfig incompleteConfig = dtlsConfigBuilder.getIncompleteConfig();

        // Handle PSK Store
        LwM2mObjectEnabler securityEnabler = this.objectEnablers.get(LwM2mId.SECURITY);
        if (securityEnabler == null) {
            throw new IllegalArgumentException("Security object is mandatory");
        }
        if (incompleteConfig.getPskStore() == null) {
            dtlsConfigBuilder.setPskStore(new SecurityObjectPskStore(securityEnabler));
        } else {
            LOG.warn("PskStore should be automatically set by Leshan. Using a custom implementation is not advised.");
        }

        // Handle secure address
        if (incompleteConfig.getAddress() == null) {
            if (localSecureAddress == null) {
                localSecureAddress = new InetSocketAddress(0);
            }
            dtlsConfigBuilder.setAddress(localSecureAddress);
        } else if (localSecureAddress != null && !localSecureAddress.equals(incompleteConfig.getAddress())) {
            throw new IllegalStateException(String.format(
                    "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for secure address: %s != %s",
                    localSecureAddress, incompleteConfig.getAddress()));
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

        dtlsConfig = dtlsConfigBuilder.build();

        // create endpoints
        CoapEndpoint unsecuredEndpoint = null;
        if (!noUnsecuredEndpoint) {
            if (endpointFactory != null) {
                unsecuredEndpoint = endpointFactory.createUnsecuredEndpoint(localAddress, coapConfig, null);
            } else {
                unsecuredEndpoint = new CoapEndpoint(localAddress, coapConfig);
            }
        }

        CoapEndpoint securedEndpoint = null;
        if (!noSecuredEndpoint) {
            if (endpointFactory != null) {
                securedEndpoint = endpointFactory.createSecuredEndpoint(dtlsConfig, coapConfig, null);
            } else {
                securedEndpoint = new CoapEndpoint(new DTLSConnector(dtlsConfig), coapConfig, null, null);
            }
        }

        if (securedEndpoint == null && unsecuredEndpoint == null) {
            throw new IllegalStateException(
                    "All CoAP enpoints are deactivated, at least one endpoint should be activated");
        }

        return new LeshanClient(endpoint, unsecuredEndpoint, securedEndpoint, objectEnablers, coapConfig, additionalAttributes);
    }
}
