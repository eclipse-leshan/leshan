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
package org.eclipse.leshan.server.californium.bootstrap.endpoint;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider;
import org.eclipse.leshan.core.californium.ExceptionTranslator;
import org.eclipse.leshan.core.californium.identity.IdentityHandler;
import org.eclipse.leshan.core.californium.identity.IdentityHandlerProvider;
import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.eclipse.leshan.server.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.bootstrap.endpoint.BootstrapServerEndpointToolbox;
import org.eclipse.leshan.server.bootstrap.endpoint.LwM2mBootstrapServerEndpoint;
import org.eclipse.leshan.server.bootstrap.endpoint.LwM2mBootstrapServerEndpointsProvider;
import org.eclipse.leshan.server.bootstrap.request.BootstrapUplinkRequestReceiver;
import org.eclipse.leshan.server.californium.RootResource;
import org.eclipse.leshan.server.californium.bootstrap.endpoint.coap.CoapBootstrapServerProtocolProvider;
import org.eclipse.leshan.server.security.ServerSecurityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaliforniumBootstrapServerEndpointsProvider implements LwM2mBootstrapServerEndpointsProvider {

    // TODO TL : provide a COAP/Californium API ? like previous LeshanServer.coapAPI()

    private final Logger LOG = LoggerFactory.getLogger(CaliforniumBootstrapServerEndpointsProvider.class);

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1,
            new NamedThreadFactory("Leshan Async Request timeout"));

    private final Configuration serverConfig;
    private final List<CaliforniumBootstrapServerEndpointFactory> endpointsFactory;
    private final BootstrapServerCoapMessageTranslator messagetranslator = new BootstrapServerCoapMessageTranslator();
    private final List<CaliforniumBootstrapServerEndpoint> endpoints;
    private CoapServer coapServer;

    public CaliforniumBootstrapServerEndpointsProvider() {
        this(new Builder().generateDefaultValue());
    }

    protected CaliforniumBootstrapServerEndpointsProvider(Builder builder) {
        this.serverConfig = builder.serverConfiguration;
        this.endpointsFactory = builder.endpointsFactory;
        this.endpoints = new ArrayList<CaliforniumBootstrapServerEndpoint>();
    }

    @Override
    public List<LwM2mBootstrapServerEndpoint> getEndpoints() {
        return Collections.unmodifiableList(endpoints);
    }

    @Override
    public LwM2mBootstrapServerEndpoint getEndpoint(URI uri) {
        for (CaliforniumBootstrapServerEndpoint endpoint : endpoints) {
            if (endpoint.getURI().equals(uri))
                return endpoint;
        }
        return null;
    }

    @Override
    public void createEndpoints(BootstrapUplinkRequestReceiver requestReceiver, BootstrapServerEndpointToolbox toolbox,
            ServerSecurityInfo serverSecurityInfo, LeshanBootstrapServer server) {
        // create server;
        coapServer = new CoapServer(serverConfig) {
            @Override
            protected Resource createRoot() {
                return new RootResource();
            }
        };

        // create identity handler provider
        IdentityHandlerProvider identityHandlerProvider = new IdentityHandlerProvider();

        // create endpoints
        for (CaliforniumBootstrapServerEndpointFactory endpointFactory : endpointsFactory) {
            // create Californium endpoint
            CoapEndpoint coapEndpoint = endpointFactory.createCoapEndpoint(serverConfig, serverSecurityInfo, server);

            if (coapEndpoint != null) {

                // create identity handler and add it to provider
                final IdentityHandler identityHandler = endpointFactory.createIdentityHandler();
                identityHandlerProvider.addIdentityHandler(coapEndpoint, identityHandler);

                // create exception translator;
                ExceptionTranslator exceptionTranslator = endpointFactory.createExceptionTranslator();

                // create LWM2M endpoint
                CaliforniumBootstrapServerEndpoint lwm2mEndpoint = new CaliforniumBootstrapServerEndpoint(
                        endpointFactory.getProtocol(), coapEndpoint, messagetranslator, toolbox, identityHandler,
                        exceptionTranslator, executor);
                endpoints.add(lwm2mEndpoint);

                // add Californium endpoint to coap server
                coapServer.addEndpoint(coapEndpoint);

            }
        }

        // create resources
        List<Resource> resources = messagetranslator.createResources(requestReceiver, toolbox, identityHandlerProvider);
        coapServer.add(resources.toArray(new Resource[resources.size()]));
    }

    @Override
    public void start() {
        coapServer.start();

    }

    @Override
    public void stop() {
        coapServer.stop();

    }

    @Override
    public void destroy() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Destroying RequestSender was interrupted.", e);
        }
        coapServer.destroy();
    }

    public static class Builder {

        private final List<BootstrapServerProtocolProvider> protocolProviders;
        private Configuration serverConfiguration;
        private final List<CaliforniumBootstrapServerEndpointFactory> endpointsFactory;

        public Builder(BootstrapServerProtocolProvider... protocolProviders) {
            // TODO TL : handle duplicate ?
            this.protocolProviders = new ArrayList<BootstrapServerProtocolProvider>();
            if (protocolProviders.length == 0) {
                this.protocolProviders.add(new CoapBootstrapServerProtocolProvider());
            } else {
                this.protocolProviders.addAll(Arrays.asList(protocolProviders));
            }

            this.endpointsFactory = new ArrayList<>();
        }

        /**
         * create Default CoAP Server Configuration.
         */
        public Configuration createDefaultConfiguration() {
            // Get all Californium modules
            Set<ModuleDefinitionsProvider> moduleProviders = new HashSet<>();
            for (BootstrapServerProtocolProvider protocolProvider : protocolProviders) {
                moduleProviders.addAll(protocolProvider.getModuleDefinitionsProviders());
            }

            // create Californium Configuration
            Configuration configuration = new Configuration(
                    moduleProviders.toArray(new ModuleDefinitionsProvider[moduleProviders.size()]));

            // apply default value
            for (BootstrapServerProtocolProvider protocolProvider : protocolProviders) {
                protocolProvider.applyDefaultValue(configuration);
            }

            return configuration;
        }

        /**
         * @param serverConfiguration the @{link Configuration} used by the {@link CoapServer}.
         */
        public Builder setConfiguration(Configuration serverConfiguration) {
            this.serverConfiguration = serverConfiguration;
            return this;
        }

        public Builder addEndpoint(String uri) {
            return addEndpoint(EndpointUriUtil.createUri(uri));
        }

        public Builder addEndpoint(URI uri) {
            for (BootstrapServerProtocolProvider protocolProvider : protocolProviders) {
                // TODO TL : validate URI
                if (protocolProvider.getProtocol().getUriScheme().equals(uri.getScheme())) {
                    // TODO TL: handle duplicate addr
                    endpointsFactory.add(protocolProvider.createDefaultEndpointFactory(uri));
                }
            }
            // TODO TL: handle missing provider for given protocol
            return this;
        }

        public Builder addEndpoint(InetSocketAddress addr, Protocol protocol) {
            return addEndpoint(EndpointUriUtil.createUri(protocol.getUriScheme(), addr));
        }

        public Builder addEndpoint(CaliforniumBootstrapServerEndpointFactory endpointFactory) {
            // TODO TL: handle duplicate addr
            endpointsFactory.add(endpointFactory);
            return this;
        }

        protected Builder generateDefaultValue() {
            if (serverConfiguration == null) {
                serverConfiguration = createDefaultConfiguration();
            }

            if (endpointsFactory.isEmpty()) {
                for (BootstrapServerProtocolProvider protocolProvider : protocolProviders) {
                    // TODO TL : handle duplicates
                    endpointsFactory.add(protocolProvider
                            .createDefaultEndpointFactory(protocolProvider.getDefaultUri(serverConfiguration)));
                }
            }
            return this;
        }

        public CaliforniumBootstrapServerEndpointsProvider build() {
            generateDefaultValue();
            return new CaliforniumBootstrapServerEndpointsProvider(this);
        }
    }
}
