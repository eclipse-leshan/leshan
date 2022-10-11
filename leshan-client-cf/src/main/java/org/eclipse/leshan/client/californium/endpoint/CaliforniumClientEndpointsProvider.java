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
package org.eclipse.leshan.client.californium.endpoint;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider;
import org.eclipse.leshan.client.californium.endpoint.coap.CoapClientProtocolProvider;
import org.eclipse.leshan.client.endpoint.ClientEndpointToolbox;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpoint;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpointsProvider;
import org.eclipse.leshan.client.request.DownlinkRequestReceiver;
import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.client.servers.ServerIdentity.Role;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.californium.identity.IdentityHandler;
import org.eclipse.leshan.core.californium.identity.IdentityHandlerProvider;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.oscore.OscoreIdentity;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaliforniumClientEndpointsProvider implements LwM2mClientEndpointsProvider {

    // TODO TL : provide a COAP/Californium API ? like previous LeshanClient.coapAPI()

    private final Logger LOG = LoggerFactory.getLogger(CaliforniumClientEndpointsProvider.class);

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1,
            new NamedThreadFactory("Leshan Async Request timeout"));

    protected boolean started = false;

    private final Configuration serverConfig;
    private final List<CaliforniumClientEndpointFactory> endpointsFactory;
    private final ClientCoapMessageTranslator messagetranslator = new ClientCoapMessageTranslator();
    private final ServerIdentityExtractor identityExtrator;
    private final IdentityHandlerProvider identityHandlerProvider;
    private LwM2mObjectTree objectTree;

    private final InetAddress clientAddress;

    // we support only 1 endpoint at a time by
    private ServerIdentity currentServer;
    private CaliforniumClientEndpoint endpoint;
    private CoapServer coapServer;

    public CaliforniumClientEndpointsProvider() {
        this(new Builder().generateDefaultValue());
    }

    protected CaliforniumClientEndpointsProvider(Builder builder) {
        this.serverConfig = builder.configuration;
        this.endpointsFactory = builder.endpointsFactory;
        this.clientAddress = builder.clientAddress;

        // create identity handler provider
        identityHandlerProvider = new IdentityHandlerProvider();

        // create identity extractor
        identityExtrator = new ServerIdentityExtractor() {

            @Override
            public ServerIdentity extractIdentity(Exchange exchange, Identity foreignPeerIdentity) {
                // TODO support multi server
                Endpoint currentCoapEndpoint = endpoint.getCoapEndpoint();

                // get coap endpoint used for this exchanged :
                Endpoint coapEndpoint = exchange.getEndpoint();

                // knowing used CoAP endpoint we should be able to know the server identity because :
                // - we create 1 CoAP endpoint by server.
                // - the lower layer should ensure that only 1 server with expected credential is able to talk.
                // (see https://github.com/eclipse/leshan/issues/992 for more details)
                if (coapEndpoint != null && coapEndpoint.equals(currentCoapEndpoint)
                        && currentCoapEndpoint.isStarted()) {
                    // For UDP (not secure) endpoint we also check socket address as anybody send data to this kind of
                    // endpoint.
                    if (endpoint.getProtocol().equals(Protocol.COAP) && !currentServer.getIdentity().getPeerAddress()
                            .equals(foreignPeerIdentity.getPeerAddress())) {
                        return null;
                    }
                    // For OSCORE, be sure OSCORE is used.
                    if (currentServer.getIdentity().isOSCORE()) {
                        if (!foreignPeerIdentity.isOSCORE() //
                                // we also check OscoreIdentity but this is probably not useful
                                // because we are using static OSCOREstore which holds only 1 OscoreParameter,
                                // so if the request was successfully decrypted and OSCORE is used, this MUST be the
                                // right
                                // server.
                                || !foreignPeerIdentity.getOscoreIdentity()
                                        .equals(currentServer.getIdentity().getOscoreIdentity())) {
                            return null;
                        }
                    }
                    return currentServer;
                }
                return null;
            }
        };
    }

    @Override
    public void init(LwM2mObjectTree objectTree, DownlinkRequestReceiver requestReceiver,
            ClientEndpointToolbox toolbox) {
        this.objectTree = objectTree;

        // create coap server
        coapServer = new CoapServer(serverConfig) {
            @Override
            protected Resource createRoot() {
                return messagetranslator.createRootResource(coapServer, identityHandlerProvider, identityExtrator,
                        requestReceiver, toolbox, objectTree);
            }
        };

        // create resources
        List<Resource> resources = messagetranslator.createResources(coapServer, identityHandlerProvider,
                identityExtrator, requestReceiver, toolbox, objectTree);
        coapServer.add(resources.toArray(new Resource[resources.size()]));
    }

    @Override
    public ServerIdentity createEndpoint(ServerInfo serverInfo, boolean clientInitiatedOnly,
            List<Certificate> trustStore, ClientEndpointToolbox toolbox) {

        // create endpoints
        for (CaliforniumClientEndpointFactory endpointFactory : endpointsFactory) {

            if (endpointFactory.getProtocol().getUriScheme().equals(serverInfo.getFullUri().getScheme())) {
                // create Californium endpoint
                Endpoint coapEndpoint = endpointFactory.createCoapEndpoint(clientAddress, serverConfig, serverInfo,
                        clientInitiatedOnly, trustStore, toolbox);

                if (coapEndpoint != null) {
                    // create identity handler and add it to provider
                    final IdentityHandler identityHandler = endpointFactory.createIdentityHandler();
                    identityHandlerProvider.addIdentityHandler(coapEndpoint, identityHandler);

                    // create LWM2M endpoint
                    endpoint = new CaliforniumClientEndpoint(endpointFactory.getProtocol(), coapEndpoint,
                            messagetranslator, toolbox, identityHandler, endpointFactory.createConnectionController(),
                            objectTree.getModel(), endpointFactory.createExceptionTranslator(), executor);

                    // add Californium endpoint to coap server
                    coapServer.addEndpoint(coapEndpoint);
                    currentServer = extractIdentity(serverInfo);

                    if (started) {
                        coapServer.start();
                        try {
                            coapEndpoint.start();
                            LOG.info("New endpoint created for server {} at {}", currentServer.getUri(),
                                    coapEndpoint.getUri());
                        } catch (IOException e) {
                            throw new RuntimeException("Unable to start endpoint", e);
                        }
                    }

                    return currentServer;
                }
            }
        }
        return null;
    }

    private ServerIdentity extractIdentity(ServerInfo serverInfo) {
        Identity serverIdentity;
        if (serverInfo.isSecure()) {
            // Support PSK
            if (serverInfo.secureMode == SecurityMode.PSK) {
                serverIdentity = Identity.psk(serverInfo.getAddress(), serverInfo.pskId);
            } else if (serverInfo.secureMode == SecurityMode.RPK) {
                serverIdentity = Identity.rpk(serverInfo.getAddress(), serverInfo.serverPublicKey);
            } else if (serverInfo.secureMode == SecurityMode.X509) {
                // TODO We set CN with '*' as we are not able to know the CN for some certificate usage and so this is
                // not used anymore to identify a server with x509.
                // See : https://github.com/eclipse/leshan/issues/992
                serverIdentity = Identity.x509(serverInfo.getAddress(), "*");
            } else {
                throw new RuntimeException("Unable to create connector : unsupported security mode");
            }
        } else if (serverInfo.useOscore) {
            // Build server identity for OSCORE
            serverIdentity = Identity.oscoreOnly(serverInfo.getAddress(),
                    new OscoreIdentity(serverInfo.oscoreSetting.getRecipientId()));
        } else {
            serverIdentity = Identity.unsecure(serverInfo.getAddress());
        }

        if (serverInfo.bootstrap) {
            return new ServerIdentity(serverIdentity, serverInfo.serverId, Role.LWM2M_BOOTSTRAP_SERVER,
                    serverInfo.serverUri);
        } else {
            return new ServerIdentity(serverIdentity, serverInfo.serverId, serverInfo.serverUri);
        }
    }

    @Override
    public Collection<ServerIdentity> createEndpoints(Collection<? extends ServerInfo> serverInfo,
            boolean clientInitiatedOnly, List<Certificate> trustStore, ClientEndpointToolbox toolbox) {
        // TODO TL : need to be implemented or removed ?
        return null;
    }

    @Override
    public void destroyEndpoints() {
        identityHandlerProvider.clear();
        for (Endpoint endpoint : coapServer.getEndpoints()) {
            coapServer.getEndpoints().remove(endpoint);
            endpoint.destroy();
        }
    }

    @Override
    public LwM2mClientEndpoint getEndpoint(ServerIdentity server) {
        if (currentServer.equals(server)) {
            return endpoint;
        }
        return null;
    }

    @Override
    public List<LwM2mClientEndpoint> getEndpoints() {
        return Arrays.asList(endpoint);
    }

    @Override
    public synchronized void start() {
        if (started)
            return;
        started = true;

        // we don't have any endpoint so nothing to start
        if (coapServer.getEndpoints().isEmpty())
            return;

        coapServer.start();
    }

    @Override
    public synchronized void stop() {
        if (!started)
            return;
        started = false;

        // If we have no endpoint this means that we never start coap server
        if (coapServer.getEndpoints().isEmpty())
            return;

        coapServer.stop();
    }

    @Override
    public synchronized void destroy() {
        if (started)
            started = false;

        executor.shutdownNow();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Destroying RequestSender was interrupted.", e);
        }

        coapServer.destroy();
    }

    public static class Builder {

        private final List<ClientProtocolProvider> protocolProviders;
        private Configuration configuration;
        private final List<CaliforniumClientEndpointFactory> endpointsFactory;
        private InetAddress clientAddress;

        public Builder(ClientProtocolProvider... protocolProviders) {
            // TODO TL : handle duplicate ?
            this.protocolProviders = new ArrayList<ClientProtocolProvider>();
            if (protocolProviders.length == 0) {
                this.protocolProviders.add(new CoapClientProtocolProvider());
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
            for (ClientProtocolProvider protocolProvider : protocolProviders) {
                moduleProviders.addAll(protocolProvider.getModuleDefinitionsProviders());
            }

            // create Californium Configuration
            Configuration configuration = new Configuration(
                    moduleProviders.toArray(new ModuleDefinitionsProvider[moduleProviders.size()]));

            // apply default value
            for (ClientProtocolProvider protocolProvider : protocolProviders) {
                protocolProvider.applyDefaultValue(configuration);
            }

            return configuration;
        }

        /**
         * @param configuration the @{link Configuration} used by the {@link CoapServer}.
         */
        public Builder setConfiguration(Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder setClientAddress(InetAddress addr) {
            clientAddress = addr;
            return this;
        }

        protected Builder generateDefaultValue() {
            if (configuration == null) {
                configuration = createDefaultConfiguration();
            }
            if (clientAddress == null) {
                clientAddress = new InetSocketAddress(0).getAddress();
            }
            if (endpointsFactory.isEmpty()) {
                for (ClientProtocolProvider protocolProvider : protocolProviders) {
                    // TODO TL : handle duplicates
                    endpointsFactory.add(protocolProvider.createDefaultEndpointFactory());
                }
            }
            return this;
        }

        public CaliforniumClientEndpointsProvider build() {
            generateDefaultValue();
            return new CaliforniumClientEndpointsProvider(this);
        }
    }
}
