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
package org.eclipse.leshan.server.californium.endpoint;

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
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.observe.NotificationListener;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider;
import org.eclipse.leshan.core.californium.EndpointContextUtil;
import org.eclipse.leshan.core.californium.ObserveUtil;
import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.AbstractLwM2mResponse;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.eclipse.leshan.server.californium.RootResource;
import org.eclipse.leshan.server.californium.endpoint.coap.CoapProtocolProvider;
import org.eclipse.leshan.server.endpoint.ClientProfile;
import org.eclipse.leshan.server.endpoint.LwM2mEndpoint;
import org.eclipse.leshan.server.endpoint.LwM2mEndpointToolbox;
import org.eclipse.leshan.server.endpoint.LwM2mEndpointsProvider;
import org.eclipse.leshan.server.endpoint.LwM2mNotificationReceiver;
import org.eclipse.leshan.server.endpoint.LwM2mRequestReceiver;
import org.eclipse.leshan.server.endpoint.LwM2mServer;
import org.eclipse.leshan.server.endpoint.Protocol;
import org.eclipse.leshan.server.endpoint.ServerSecurityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaliforniumEndpointsProvider implements LwM2mEndpointsProvider {

    // TODO TL : provide a COAP/Californium API ? like previous LeshanServer.coapAPI()

    private final Logger LOG = LoggerFactory.getLogger(CaliforniumEndpointsProvider.class);

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1,
            new NamedThreadFactory("Leshan Async Request timeout"));

    private final Configuration serverConfig;
    private final List<CaliforniumEndpointFactory> endpointsFactory;
    private final CoapMessageTranslator messagetranslator = new ServerCoapMessageTranslator();
    private final List<LwM2mCoapEndpoint> endpoints;
    private CoapServer coapServer;

    public CaliforniumEndpointsProvider() {
        this(new Builder().generateDefaultValue());
    }

    protected CaliforniumEndpointsProvider(Builder builder) {
        this.serverConfig = builder.serverConfiguration;
        this.endpointsFactory = builder.endpointsFactory;
        this.endpoints = new ArrayList<LwM2mCoapEndpoint>();
    }

    @Override
    public List<LwM2mEndpoint> getEndpoints() {
        return Collections.unmodifiableList(endpoints);
    }

    @Override
    public LwM2mEndpoint getEndpoint(URI uri) {
        for (LwM2mCoapEndpoint endpoint : endpoints) {
            if (endpoint.getURI().equals(uri))
                return endpoint;
        }
        return null;
    }

    @Override
    public void createEndpoints(LwM2mRequestReceiver requestReceiver, LwM2mNotificationReceiver notificatonReceiver,
            LwM2mEndpointToolbox toolbox, ServerSecurityInfo serverSecurityInfo, LwM2mServer server) {
        // create server;
        coapServer = new CoapServer(serverConfig) {
            @Override
            protected Resource createRoot() {
                return new RootResource();
            }
        };

        // create endpoints
        for (CaliforniumEndpointFactory endpointFactory : endpointsFactory) {
            // create Californium endpoint
            Endpoint coapEndpoint = endpointFactory.createEndpoint(serverConfig, serverSecurityInfo, server,
                    notificatonReceiver);

            if (coapEndpoint != null) {
                // create LWM2M endpoint
                LwM2mCoapEndpoint lwm2mEndpoint = new LwM2mCoapEndpoint(endpointFactory.getProtocol(), coapEndpoint,
                        messagetranslator, toolbox, notificatonReceiver, executor);
                endpoints.add(lwm2mEndpoint);

                // add Californium endpoint to coap server
                coapServer.addEndpoint(coapEndpoint);

                // add NotificationListener
                coapEndpoint.addNotificationListener(new NotificationListener() {

                    @Override
                    public void onNotification(Request coapRequest, Response coapResponse) {
                        // Get Observation
                        String regid = coapRequest.getUserContext().get(ObserveUtil.CTX_REGID);
                        Observation observation = server.getRegistrationStore().getObservation(regid,
                                coapResponse.getToken().getBytes());
                        if (observation == null) {
                            LOG.error("Unexpected error: Unable to find observation with token {} for registration {}",
                                    coapResponse.getToken(), regid);
                            return;
                        }
                        // Get profile
                        Identity identity = EndpointContextUtil.extractIdentity(coapResponse.getSourceContext());
                        ClientProfile profile = (ClientProfile) toolbox.getProfileProvider().getProfile(identity);

                        // create Observe Response
                        try {
                            AbstractLwM2mResponse response = messagetranslator.createObservation(observation,
                                    coapResponse, toolbox, profile);
                            if (observation instanceof SingleObservation) {
                                notificatonReceiver.onNotification((SingleObservation) observation, profile,
                                        (ObserveResponse) response);
                            } else if (observation instanceof CompositeObservation) {
                                notificatonReceiver.onNotification((CompositeObservation) observation, profile,
                                        (ObserveCompositeResponse) response);
                            }
                        } catch (Exception e) {
                            notificatonReceiver.onError(observation, profile, e);
                        }

                    }
                });
            }
        }

        // create resources
        List<Resource> resources = messagetranslator.createResources(requestReceiver, toolbox);
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

        private final List<CaliforniumProtocolProvider> protocolProviders;
        private Configuration serverConfiguration;
        private final List<CaliforniumEndpointFactory> endpointsFactory;

        public Builder(CaliforniumProtocolProvider... protocolProviders) {
            // TODO TL : handle duplicate ?
            this.protocolProviders = new ArrayList<CaliforniumProtocolProvider>();
            if (protocolProviders.length == 0) {
                this.protocolProviders.add(new CoapProtocolProvider());
            } else {
                this.protocolProviders.addAll(Arrays.asList(protocolProviders));
            }

            this.endpointsFactory = new ArrayList<>();
        }

        /**
         * create Default CoAP Server Configuration.
         */
        public Configuration createDefaultCoapServerConfiguration() {
            // Get all Californium modules
            Set<ModuleDefinitionsProvider> moduleProviders = new HashSet<>();
            for (CaliforniumProtocolProvider protocolProvider : protocolProviders) {
                moduleProviders.addAll(protocolProvider.getModuleDefinitionsProviders());
            }

            // create Californium Configuration
            Configuration configuration = new Configuration(
                    moduleProviders.toArray(new ModuleDefinitionsProvider[moduleProviders.size()]));

            // apply default value
            for (CaliforniumProtocolProvider protocolProvider : protocolProviders) {
                protocolProvider.applyDefaultValue(configuration);
            }

            return configuration;
        }

        /**
         * @param serverConfiguration the @{link Configuration} used by the {@link CoapServer}.
         */
        public Builder setCoapServerConfiguration(Configuration serverConfiguration) {
            this.serverConfiguration = serverConfiguration;
            return this;
        }

        public Builder addEndpoint(URI uri) {
            for (CaliforniumProtocolProvider protocolProvider : protocolProviders) {
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

        public Builder addEndpoint(CaliforniumEndpointFactory endpointFactory) {
            // TODO TL: handle duplicate addr
            endpointsFactory.add(endpointFactory);
            return this;
        }

        protected Builder generateDefaultValue() {
            if (serverConfiguration == null) {
                serverConfiguration = createDefaultCoapServerConfiguration();
            }

            if (endpointsFactory.isEmpty()) {
                for (CaliforniumProtocolProvider protocolProvider : protocolProviders) {
                    // TODO TL : handle duplicates
                    endpointsFactory.add(protocolProvider
                            .createDefaultEndpointFactory(protocolProvider.getDefaultUri(serverConfiguration)));
                }
            }
            return this;
        }

        public CaliforniumEndpointsProvider build() {
            generateDefaultValue();
            return new CaliforniumEndpointsProvider(this);
        }
    }
}
