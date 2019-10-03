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
 *     Rikard Höglund (RISE SICS) - Additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.client.californium.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.cose.AlgorithmID;
import org.eclipse.californium.cose.CoseException;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.auth.RawPublicKeyIdentity;
import org.eclipse.californium.oscore.HashMapCtxDB;
import org.eclipse.californium.oscore.OSCoreCtx;
import org.eclipse.californium.oscore.OSException;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertDescription;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertLevel;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.pskstore.StaticPskStore;
import org.eclipse.californium.scandium.dtls.rpkstore.TrustedRpkStore;
import org.eclipse.californium.scandium.dtls.x509.CertificateVerifier;
import org.eclipse.leshan.SecurityMode;
import org.eclipse.leshan.client.californium.OscoreHandler;
import org.eclipse.leshan.client.servers.EndpointsManager;
import org.eclipse.leshan.client.servers.Server;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.core.californium.EndpointContextUtil;
import org.eclipse.leshan.core.californium.EndpointFactory;
import org.eclipse.leshan.core.request.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.upokecenter.cbor.CBORObject;

public class CaliforniumEndpointsManager implements EndpointsManager {

    private static final Logger LOG = LoggerFactory.getLogger(CaliforniumEndpointsManager.class);

    private boolean started = false;

    private CoapEndpoint currentEndpoint;
    private Builder dtlsConfigbuilder;
    private NetworkConfig coapConfig;
    private InetSocketAddress localAddress;
    private CoapServer coapServer;
    private EndpointFactory endpointFactory;

    public CaliforniumEndpointsManager(CoapServer coapServer, InetSocketAddress localAddress, NetworkConfig coapConfig,
            Builder dtlsConfigBuilder, EndpointFactory endpointFactory) {
        this.coapServer = coapServer;
        this.localAddress = localAddress;
        this.coapConfig = coapConfig;
        this.dtlsConfigbuilder = dtlsConfigBuilder;
        this.endpointFactory = endpointFactory;
    }

    @Override
    public synchronized Server createEndpoint(ServerInfo serverInfo) {
        // Clear previous endpoint
        if (currentEndpoint != null) {
            coapServer.getEndpoints().remove(currentEndpoint);
            currentEndpoint.destroy();
        }

        // Create new endpoint
        Identity serverIdentity;
        if (serverInfo.isSecure()) {
            Builder newBuilder = new Builder(dtlsConfigbuilder.getIncompleteConfig());

            // Support PSK
            if (serverInfo.secureMode == SecurityMode.PSK) {
                StaticPskStore staticPskStore = new StaticPskStore(serverInfo.pskId, serverInfo.pskKey);
                newBuilder.setPskStore(staticPskStore);
                serverIdentity = Identity.psk(serverInfo.getAddress(), serverInfo.pskId);
            } else if (serverInfo.secureMode == SecurityMode.RPK) {
                // set identity
                newBuilder.setIdentity(serverInfo.privateKey, serverInfo.publicKey);
                // set RPK truststore
                final PublicKey expectedKey = serverInfo.serverPublicKey;
                newBuilder.setRpkTrustStore(new TrustedRpkStore() {
                    @Override
                    public boolean isTrusted(RawPublicKeyIdentity id) {
                        PublicKey receivedKey = id.getKey();
                        if (receivedKey == null) {
                            LOG.warn("The server public key is null {}", id);
                            return false;
                        }
                        if (!receivedKey.equals(expectedKey)) {
                            LOG.debug(
                                    "Server public key received does match with the expected one.\nReceived: {}\nExpected: {}",
                                    receivedKey, expectedKey);
                            return false;
                        }
                        return true;
                    }
                });
                serverIdentity = Identity.rpk(serverInfo.getAddress(), expectedKey);
            } else if (serverInfo.secureMode == SecurityMode.X509) {
                // set identity
                newBuilder.setIdentity(serverInfo.privateKey, new Certificate[] { serverInfo.clientCertificate });

                // set X509 verifier
                final Certificate expectedServerCertificate = serverInfo.serverCertificate;
                newBuilder.setCertificateVerifier(new CertificateVerifier() {

                    @Override
                    public void verifyCertificate(CertificateMessage message, DTLSSession session)
                            throws HandshakeException {
                        // As specify in the LWM2M spec 1.0, we only support "domain-issued certificate" usage
                        // Defined in : https://tools.ietf.org/html/rfc6698#section-2.1.1 (3 -- Certificate usage 3)

                        // Get server certificate from certificate message
                        if (message.getCertificateChain().getCertificates().size() == 0) {
                            AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.BAD_CERTIFICATE,
                                    session.getPeer());
                            throw new HandshakeException("Certificate chain could not be validated", alert);
                        }
                        Certificate receivedServerCertificate = message.getCertificateChain().getCertificates().get(0);

                        // Validate certificate
                        if (!expectedServerCertificate.equals(receivedServerCertificate)) {
                            AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.BAD_CERTIFICATE,
                                    session.getPeer());
                            throw new HandshakeException("Certificate chain could not be validated", alert);
                        }
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                });
                serverIdentity = Identity.x509(serverInfo.getAddress(), EndpointContextUtil
                        .extractCN(((X509Certificate) expectedServerCertificate).getSubjectX500Principal().getName()));
            } else {
                throw new RuntimeException("Unable to create connector : unsupported security mode");
            }
            if (endpointFactory != null) {
                currentEndpoint = endpointFactory.createSecuredEndpoint(newBuilder.build(), coapConfig, null, null);
            } else {
                CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
                builder.setConnector(new DTLSConnector(newBuilder.build()));
                builder.setNetworkConfig(coapConfig);
                currentEndpoint = builder.build();
            }
        } else if (serverInfo.useOscore) {
            // oscore only mode
            LOG.info("Adding OSCORE context for " + serverInfo.getFullUri().toASCIIString());
            HashMapCtxDB db = OscoreHandler.getContextDB(); // TODO: Do not use singleton here but give it to endpoint
                                                          // builder (for Cf-M16)

            AlgorithmID hkdfAlg = null;
            try {
                hkdfAlg = AlgorithmID.FromCBOR(CBORObject.FromObject(serverInfo.hkdfAlgorithm));
            } catch (CoseException e) {
                LOG.error("Failed to decode OSCORE HMAC algorithm");
            }

            AlgorithmID aeadAlg = null;
            try {
                aeadAlg = AlgorithmID.FromCBOR(CBORObject.FromObject(serverInfo.aeadAlgorithm));
            } catch (CoseException e) {
                LOG.error("Failed to decode OSCORE AEAD algorithm");
            }

            try {
                OSCoreCtx ctx = new OSCoreCtx(serverInfo.masterSecret, true, aeadAlg, serverInfo.senderId,
                        serverInfo.recipientId, hkdfAlg, 32, serverInfo.masterSalt, serverInfo.idContext);
                db.addContext(serverInfo.getFullUri().toASCIIString(), ctx);

                // Also add the context by the IP of the server since requests may use that
                String serverIP = InetAddress.getByName(serverInfo.getFullUri().getHost()).getHostAddress();
                db.addContext("coap://" + serverIP, ctx);

            } catch (OSException | UnknownHostException e) {
                LOG.error("Failed to generate OSCORE context information");
                return null;
            }

            if (endpointFactory != null) {
                currentEndpoint = endpointFactory.createUnsecuredEndpoint(localAddress, coapConfig, null, db);
            } else {
                CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
                builder.setInetSocketAddress(localAddress);
                builder.setNetworkConfig(coapConfig);
                currentEndpoint = builder.build();
            }
            serverIdentity = Identity.unsecure(serverInfo.getAddress()); // TODO: FIX?

        } else {
            if (endpointFactory != null) {
                currentEndpoint = endpointFactory.createUnsecuredEndpoint(localAddress, coapConfig, null, null);
            } else {
                CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
                builder.setInetSocketAddress(localAddress);
                builder.setNetworkConfig(coapConfig);
                currentEndpoint = builder.build();
            }
            serverIdentity = Identity.unsecure(serverInfo.getAddress());
        }

        // Add new endpoint
        coapServer.addEndpoint(currentEndpoint);

        // Start endpoint if needed
        Server server = new Server(serverIdentity, serverInfo.serverId);
        if (started) {
            coapServer.start();
            try {
                currentEndpoint.start();
                LOG.info("New endpoint created for server {} at {}", server.getUri(), currentEndpoint.getUri());
            } catch (IOException e) {
                throw new RuntimeException("Unable to start endpoint", e);
            }
        }
        return server;
    }

    @Override
    public synchronized Collection<Server> createEndpoints(Collection<? extends ServerInfo> serverInfo) {
        if (serverInfo == null || serverInfo.isEmpty())
            return null;
        else {
            // TODO support multi server;
            ServerInfo firstServer = serverInfo.iterator().next();
            Collection<Server> servers = new ArrayList<>(1);
            servers.add(createEndpoint(firstServer));
            return servers;
        }
    }

    @Override
    public synchronized void forceReconnection(Server server) {
        // TODO support multi server
        Connector connector = currentEndpoint.getConnector();
        if (connector instanceof DTLSConnector) {
            ((DTLSConnector) connector).forceResumeAllSessions();
        }
        LOG.info("Clear DTLS session for server {}", server.getUri());
    }

    public synchronized Endpoint getEndpoint(Identity server) {
        // TODO support multi server;
        if (currentEndpoint.isStarted())
            return currentEndpoint;
        return null;
    }

    @Override
    public synchronized void start() {
        if (started)
            return;
        started = true;

        // we don't have any endpoint so nothing to start
        if (currentEndpoint == null)
            return;

        coapServer.start();
    }

    @Override
    public synchronized void stop() {
        if (!started)
            return;
        started = false;

        // If we have no endpoint this means that we never start coap server
        if (currentEndpoint == null)
            return;

        coapServer.stop();
    }

    @Override
    public synchronized void destroy() {
        // TODO we should be able to destroy a not started coapServer.
        if (!started)
            return;
        started = false;

        coapServer.destroy();
    }
}
