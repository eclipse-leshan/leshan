/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.californium.server.endpoint.coap;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.config.CoapConfig.TrackerMode;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.CoapEndpoint.Builder;
import org.eclipse.californium.core.network.serialization.UdpDataParser;
import org.eclipse.californium.core.network.serialization.UdpDataSerializer;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.UDPConnector;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider;
import org.eclipse.californium.elements.config.SystemConfig;
import org.eclipse.californium.elements.config.UdpConfig;
import org.eclipse.leshan.core.endpoint.DefaultEndPointUriHandler;
import org.eclipse.leshan.core.endpoint.EndPointUriHandler;
import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.endpoint.EffectiveEndpointUriProvider;
import org.eclipse.leshan.server.observation.LwM2mNotificationReceiver;
import org.eclipse.leshan.servers.security.ServerSecurityInfo;
import org.eclipse.leshan.transport.californium.DefaultExceptionTranslator;
import org.eclipse.leshan.transport.californium.ExceptionTranslator;
import org.eclipse.leshan.transport.californium.identity.DefaultCoapIdentityHandler;
import org.eclipse.leshan.transport.californium.identity.IdentityHandler;
import org.eclipse.leshan.transport.californium.server.endpoint.CaliforniumServerEndpointFactory;
import org.eclipse.leshan.transport.californium.server.observation.LwM2mObservationStore;
import org.eclipse.leshan.transport.californium.server.observation.ObservationSerDes;

public class CoapServerEndpointFactory implements CaliforniumServerEndpointFactory {

    public static Protocol getSupportedProtocol() {
        return Protocol.COAP;
    }

    public static void applyDefaultValue(Configuration configuration) {
        configuration.set(CoapConfig.MID_TRACKER, TrackerMode.NULL);
    }

    public static List<ModuleDefinitionsProvider> getModuleDefinitionsProviders() {
        return Arrays.asList(SystemConfig.DEFINITIONS, CoapConfig.DEFINITIONS, UdpConfig.DEFINITIONS);
    }

    protected final EndpointUri endpointUri;
    protected final String loggingTagPrefix;
    protected final Configuration configuration;
    protected final Consumer<CoapEndpoint.Builder> coapEndpointConfigInitializer;
    protected final EndPointUriHandler uriHandler;

    public CoapServerEndpointFactory(EndpointUri uri) {
        this(uri, null, null, null, new DefaultEndPointUriHandler());
    }

    public CoapServerEndpointFactory(EndpointUri uri, String loggingTagPrefix, Configuration configuration,
            Consumer<CoapEndpoint.Builder> coapEndpointConfigInitializer, EndPointUriHandler uriHandler) {
        this.uriHandler = uriHandler;
        uriHandler.validateURI(uri);

        this.endpointUri = uri;
        this.loggingTagPrefix = loggingTagPrefix == null ? "LWM2M Server" : loggingTagPrefix;
        this.configuration = configuration;
        this.coapEndpointConfigInitializer = coapEndpointConfigInitializer;
    }

    @Override
    public Protocol getProtocol() {
        return getSupportedProtocol();
    }

    @Override
    public String getEndpointDescription() {
        return "CoAP over UDP endpoint based on Californium library";
    }

    @Override
    public EndpointUri getUri() {
        return endpointUri;
    }

    protected String getLoggingTag() {
        if (loggingTagPrefix != null) {
            return String.format("[%s-%s]", loggingTagPrefix, getUri().toString());
        } else {
            return String.format("[%s]", getUri().toString());
        }
    }

    @Override
    public CoapEndpoint createCoapEndpoint(Configuration defaultConfiguration, ServerSecurityInfo serverSecurityInfo,
            LwM2mNotificationReceiver notificationReceiver, LeshanServer server,
            EffectiveEndpointUriProvider uriProvider) {

        // defined Configuration to use
        Configuration configurationToUse;
        if (configuration == null) {
            // if specific configuration for this endpoint is null, used the default one which is the coapServer
            // Configuration shared with all endpoints by default.
            configurationToUse = defaultConfiguration;
        } else {
            configurationToUse = configuration;
        }

        return createEndpointBuilder(uriHandler.getSocketAddr(endpointUri), configurationToUse, notificationReceiver,
                server, uriProvider).build();
    }

    /**
     * This method is intended to be overridden.
     *
     * @param address the IP address and port, if null the connector is bound to an ephemeral port on the wildcard
     *        address.
     * @param coapConfig the CoAP config used to create this endpoint.
     * @return the {@link Builder} used for unsecured communication.
     */
    protected CoapEndpoint.Builder createEndpointBuilder(InetSocketAddress address, Configuration coapConfig,
            LwM2mNotificationReceiver notificationReceiver, LeshanServer server,
            EffectiveEndpointUriProvider uriProvider) {
        CoapEndpoint.Builder builder = new CoapEndpoint.Builder();

        builder.setConnector(createConnector(address, coapConfig));
        builder.setConfiguration(coapConfig);
        builder.setLoggingTag(getLoggingTag());

        builder.setObservationStore(new LwM2mObservationStore(uriProvider, server.getRegistrationStore(),
                notificationReceiver, new ObservationSerDes(new UdpDataParser(), new UdpDataSerializer())));

        if (coapEndpointConfigInitializer != null)
            coapEndpointConfigInitializer.accept(builder);

        return builder;
    }

    /**
     * By default create an {@link UDPConnector}.
     * <p>
     * This method is intended to be overridden.
     *
     * @param address the IP address and port, if null the connector is bound to an ephemeral port on the wildcard
     *        address
     * @param coapConfig the Configuration
     * @return the {@link Connector} used for unsecured {@link CoapEndpoint}
     */
    protected Connector createConnector(InetSocketAddress address, Configuration coapConfig) {
        return new UDPConnector(address, coapConfig);
    }

    @Override
    public IdentityHandler createIdentityHandler() {
        return new DefaultCoapIdentityHandler();
    }

    @Override
    public ExceptionTranslator createExceptionTranslator() {
        return new DefaultExceptionTranslator();
    }
}
