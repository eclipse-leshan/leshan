/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.javacoap.client.endpoint;

import java.io.IOException;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.leshan.client.endpoint.ClientEndpointToolbox;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpoint;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpointsProvider;
import org.eclipse.leshan.client.notification.NotificationManager;
import org.eclipse.leshan.client.request.DownlinkRequestReceiver;
import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.peer.OscoreIdentity;
import org.eclipse.leshan.core.peer.PskIdentity;
import org.eclipse.leshan.core.peer.RpkIdentity;
import org.eclipse.leshan.core.peer.X509Identity;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.transport.javacoap.State;
import org.eclipse.leshan.transport.javacoap.client.observe.HashMapObserversStore;
import org.eclipse.leshan.transport.javacoap.client.observe.LwM2mKeys;
import org.eclipse.leshan.transport.javacoap.client.observe.NotificationHandler;
import org.eclipse.leshan.transport.javacoap.client.observe.ObserversManager;
import org.eclipse.leshan.transport.javacoap.client.request.ClientCoapMessageTranslator;
import org.eclipse.leshan.transport.javacoap.client.resource.BootstrapResource;
import org.eclipse.leshan.transport.javacoap.client.resource.ObjectResource;
import org.eclipse.leshan.transport.javacoap.client.resource.RootResource;
import org.eclipse.leshan.transport.javacoap.client.resource.RouterService;
import org.eclipse.leshan.transport.javacoap.client.resource.ServerIdentityExtractor;
import org.eclipse.leshan.transport.javacoap.identity.IdentityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Method;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.transport.TransportContext;
import com.mbed.coap.utils.Service;

public abstract class AbstractJavaCoapClientEndpointsProvider implements LwM2mClientEndpointsProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractJavaCoapClientEndpointsProvider.class);

    private final Protocol supportedProtocol;
    private final String endpointDescription;
    private final ClientCoapMessageTranslator messagetranslator;
    private LwM2mObjectTree objectTree;
    private Service<CoapRequest, CoapResponse> router;
    private ClientEndpointToolbox toolbox;
    private final IdentityHandler identityHandler;

    private JavaCoapClientEndpoint lwm2mEndpoint;
    private CoapServer coapServer;
    private volatile ServerInfo currentServerInfo;
    private volatile List<Certificate> currentTrustStore;
    private volatile LwM2mServer currentServer;

    protected State state = State.INITIAL;
    private ObserversManager observersManager;

    public AbstractJavaCoapClientEndpointsProvider(Protocol protocol, String endpointDescription,
            IdentityHandler identityHandler) {
        this.supportedProtocol = protocol;
        this.endpointDescription = endpointDescription;
        this.identityHandler = identityHandler;
        this.messagetranslator = new ClientCoapMessageTranslator(identityHandler);
    }

    @Override
    public void init(LwM2mObjectTree objectTree, DownlinkRequestReceiver requestReceiver,
            NotificationManager notificationManager, ClientEndpointToolbox toolbox) {
        this.objectTree = objectTree;
        this.toolbox = toolbox;

        ServerIdentityExtractor identityExtractor = new ServerIdentityExtractor() {
            @Override
            public LwM2mServer extractIdentity(IpPeer foreignPeer) {
                if (currentServer == null) {
                    return null;
                }
                if (supportedProtocol != Protocol.COAP || currentServer.getTransportData().equals(foreignPeer)) {
                    return currentServer;
                }
                return null;
            }
        };

        // Create Observers Manager
        observersManager = new ObserversManager(new HashMapObserversStore() {
            @Override
            public void add(CoapRequest observeRequest) {
                if (observeRequest.getMethod() == Method.FETCH && observeRequest.options().getContentFormat() != null) {
                    // TODO code below is duplicate from RootResource :
                    // because for now there is no way to avoid to decode twice on observe :/
                    ContentFormat requestContentFormat = ContentFormat
                            .fromCode(observeRequest.options().getContentFormat());
                    List<LwM2mPath> paths = toolbox.getDecoder().decodePaths(observeRequest.getPayload().getBytes(),
                            requestContentFormat, null);

                    // optimization for LWM2M Composite Observe : to not decode LWM2M paths each time
                    TransportContext extendedContext = observeRequest.getTransContext() //
                            .with(LwM2mKeys.LESHAN_OBSERVED_PATHS, paths);

                    CoapRequest modifiedObservedRequest = new CoapRequest(observeRequest.getMethod(),
                            observeRequest.getToken(), observeRequest.options(), observeRequest.getPayload(),
                            observeRequest.getPeerAddress(), extendedContext);

                    super.add(modifiedObservedRequest);
                } else {
                    super.add(observeRequest);
                }
            }
        });

        // Create Resources / Routes
        RouterService.RouteBuilder routerBuilder = new RouterService.RouteBuilder();
        routerBuilder //
                .any("/",
                        observersManager
                                .then(new RootResource(requestReceiver, toolbox, identityHandler, identityExtractor))) //
                .any("/bs", new BootstrapResource(requestReceiver, identityHandler, identityExtractor)) //
                .any("/*", observersManager.then(new ObjectResource(requestReceiver, "/", toolbox, identityHandler,
                        identityExtractor, notificationManager, observersManager)));
        router = routerBuilder.build();

        // Create notification handler
        NotificationHandler notificationHandler = new NotificationHandler(
                // use router but change Observe request in Read request and also flag request as notification
                req -> router.apply(req.modify() //
                        .addContext(LwM2mKeys.LESHAN_NOTIFICATION, true) //
                        .options(coapOptionsBuilder -> coapOptionsBuilder.observe(null)) //
                        .build()) //
                , observersManager);
        objectTree.addListener(notificationHandler);
    }

    @Override
    public synchronized LwM2mServer createEndpoint(ServerInfo serverInfo, boolean clientInitiatedOnly,
            List<Certificate> trustStore, ClientEndpointToolbox toolbox) {

        if (!this.supportedProtocol.getUriScheme().equals(serverInfo.getFullUri().getScheme())) {
            return null;
        }

        // As we support only 1 server destroy previous one first
        destroyEndpoints();

        // Create a new endpoints
        currentServerInfo = serverInfo;
        currentTrustStore = trustStore;
        createLwM2mEndpoint(serverInfo, trustStore);
        currentServer = extractIdentity(serverInfo);

        // Start it if needed.
        if (state.isStarted()) {
            try {
                coapServer.start();
            } catch (IOException e) {
                LOG.warn("Unable to start endpoint", e);
                return null;
            }
        }
        return currentServer;
    }

    public void createLwM2mEndpoint(ServerInfo lwm2mServer, List<Certificate> trustStore) {
        // Create a new endpoints
        coapServer = createCoapServer(lwm2mServer, router, trustStore);
        observersManager.init(coapServer);
        lwm2mEndpoint = new JavaCoapClientEndpoint(supportedProtocol, endpointDescription, coapServer,
                messagetranslator, toolbox, objectTree.getModel());
    }

    protected abstract CoapServer createCoapServer(ServerInfo serverInfo, Service<CoapRequest, CoapResponse> router,
            List<Certificate> trustStore);

    @Override
    public synchronized Collection<LwM2mServer> createEndpoints(Collection<? extends ServerInfo> serverInfo,
            boolean clientInitiatedOnly, List<Certificate> trustStore, ClientEndpointToolbox toolbox) {
        // TODO TL : need to be implemented or removed ?
        return null;
    }

    @Override
    public synchronized void destroyEndpoints() {
        // java-coap does not support several endpoints by server so we just kill current server
        if (coapServer != null)
            coapServer.stop();
        coapServer = null;
        lwm2mEndpoint = null;
    }

    @Override
    public List<LwM2mClientEndpoint> getEndpoints() {
        return Arrays.asList(lwm2mEndpoint);
    }

    @Override
    public LwM2mClientEndpoint getEndpoint(LwM2mServer server) {
        if (server.equals(currentServer)) {
            return lwm2mEndpoint;
        } else {
            return null;
        }
    }

    @Override
    public synchronized void start() {
        try {
            if (state.isDestroyed()) {
                throw new IllegalStateException("Can not start a destroyed provider");
            }
            if (state.isStarted()) {
                return;
            }
            if (state.isStopped()) {
                if (currentServer != null) {
                    createLwM2mEndpoint(currentServerInfo, currentTrustStore);
                }
            }
            if (coapServer != null)
                coapServer.start();
            state = State.STARTED;
        } catch (

        IOException e) {
            throw new IllegalStateException("Unable to start java-coap endpoint", e);
        }
    }

    @Override
    public synchronized void stop() {
        if (state.isDestroyed()) {
            throw new IllegalStateException("Can not start a destroyed provider");
        }
        if (state.isStopped()) {
            return;
        }
        state = State.STOPPED;
        destroyEndpoints();
    }

    @Override
    public synchronized void destroy() {
        state = State.DESTROYED;
        destroyEndpoints();
    }

    private LwM2mServer extractIdentity(ServerInfo serverInfo) {
        IpPeer transportData;
        if (serverInfo.isSecure()) {
            // Support PSK
            if (serverInfo.secureMode == SecurityMode.PSK) {
                transportData = new IpPeer(serverInfo.getAddress(), new PskIdentity(serverInfo.pskId));
            } else if (serverInfo.secureMode == SecurityMode.RPK) {
                transportData = new IpPeer(serverInfo.getAddress(), new RpkIdentity(serverInfo.serverPublicKey));
            } else if (serverInfo.secureMode == SecurityMode.X509) {
                // TODO We set CN with '*' as we are not able to know the CN for some certificate usage and so this is
                // not used anymore to identify a server with x509.
                // See : https://github.com/eclipse/leshan/issues/992
                transportData = new IpPeer(serverInfo.getAddress(), new X509Identity("*"));
            } else {
                throw new RuntimeException("Unable to create connector : unsupported security mode");
            }
        } else if (serverInfo.useOscore) {
            // Build server identity for OSCORE
            transportData = new IpPeer(serverInfo.getAddress(),
                    new OscoreIdentity(serverInfo.oscoreSetting.getRecipientId()));
        } else {
            transportData = new IpPeer((serverInfo.getAddress()));
        }

        if (serverInfo.bootstrap) {
            return new LwM2mServer(transportData, serverInfo.serverUri);
        } else {
            return new LwM2mServer(transportData, serverInfo.serverId, serverInfo.serverUri);
        }
    }
}
