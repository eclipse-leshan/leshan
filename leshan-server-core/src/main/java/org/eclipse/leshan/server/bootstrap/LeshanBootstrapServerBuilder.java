/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use CoapEndpointBuilder
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.server.bootstrap;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import org.eclipse.leshan.core.link.lwm2m.DefaultLwM2mLinkParser;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLinkParser;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.server.bootstrap.endpoint.LwM2mBootstrapServerEndpointsProvider;
import org.eclipse.leshan.server.bootstrap.request.BootstrapDownlinkRequestSender;
import org.eclipse.leshan.server.model.LwM2mBootstrapModelProvider;
import org.eclipse.leshan.server.model.StandardBootstrapModelProvider;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.eclipse.leshan.server.security.ServerSecurityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class helping you to build and configure a Californium based Leshan Bootstrap Lightweight M2M server.
 * <p>
 * Usage: create it, call the different setters for changing the configuration and then call the {@link #build()} method
 * for creating the {@link LeshanBootstrapServer} ready to operate.
 */
public class LeshanBootstrapServerBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(LeshanBootstrapServerBuilder.class);

    private BootstrapConfigStore configStore;
    private BootstrapSecurityStore securityStore;
    private BootstrapSessionManager sessionManager;
    private BootstrapHandlerFactory bootstrapHandlerFactory;

    private LwM2mBootstrapModelProvider modelProvider;

    private LwM2mEncoder encoder;
    private LwM2mDecoder decoder;

    private PublicKey publicKey;
    private PrivateKey privateKey;
    private X509Certificate[] certificateChain;
    private Certificate[] trustedCertificates;

    private LwM2mLinkParser linkParser;
    private LwM2mBootstrapServerEndpointsProvider endpointsProvider;

    /**
     * Set the {@link PublicKey} of the server which will be used for Raw Public Key DTLS authentication.
     * <p>
     * This should be used for RPK support only.
     * <p>
     * Setting <code>publicKey</code> and <code>privateKey</code> will enable RawPublicKey DTLS authentication, see also
     * {@link LeshanBootstrapServerBuilder#setPrivateKey(PrivateKey)}.
     *
     * @param publicKey the Raw Public Key of the bootstrap server.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public LeshanBootstrapServerBuilder setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    /**
     * Set the CertificateChain of the server which will be used for X.509 DTLS authentication.
     * <p>
     * Setting <code>publicKey</code> and <code>privateKey</code> will enable RPK and X.509 DTLS authentication, see
     * also {@link LeshanBootstrapServerBuilder#setPrivateKey(PrivateKey)}.
     * <p>
     * For RPK the public key will be extracted from the first X.509 certificate of the certificate chain. If you only
     * need RPK support, use {@link LeshanBootstrapServerBuilder#setPublicKey(PublicKey)} instead.
     *
     * @param certificateChain the certificate chain of the bootstrap server.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public <T extends X509Certificate> LeshanBootstrapServerBuilder setCertificateChain(T[] certificateChain) {
        this.certificateChain = certificateChain;
        return this;
    }

    /**
     * Set the {@link PrivateKey} of the server which will be used for RawPublicKey(RPK) and/or X.509 DTLS
     * authentication.
     *
     * @param privateKey the Private Key of the bootstrap server.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public LeshanBootstrapServerBuilder setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    /**
     * The list of trusted certificates used to authenticate devices using X.509 DTLS authentication.
     *
     * @param trustedCertificates certificates trusted by the bootstrap server.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public <T extends Certificate> LeshanBootstrapServerBuilder setTrustedCertificates(T[] trustedCertificates) {
        this.trustedCertificates = trustedCertificates;
        return this;
    }

    /**
     * Set the {@link BootstrapConfigStore} containing bootstrap configuration to apply to each devices.
     * <p>
     * By default an {@link InMemoryBootstrapConfigStore} is used.
     * <p>
     * See {@link BootstrapConfig} to see what is could be done during a bootstrap session.
     *
     * @param configStore the bootstrap configuration store.
     * @return the builder for fluent Bootstrap Server creation.
     *
     */
    public LeshanBootstrapServerBuilder setConfigStore(BootstrapConfigStore configStore) {
        this.configStore = configStore;
        return this;
    }

    /**
     * Set the {@link BootstrapSecurityStore} which contains data needed to authenticate devices.
     * <p>
     * WARNING: without security store all devices will be accepted which is not really recommended in production
     * environment.
     * <p>
     * There is not default implementation.
     *
     * @param securityStore the security store used to authenticate devices.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public LeshanBootstrapServerBuilder setSecurityStore(BootstrapSecurityStore securityStore) {
        this.securityStore = securityStore;
        return this;
    }

    /**
     * Advanced setter used to define {@link BootstrapSessionManager}.
     * <p>
     * See {@link BootstrapSessionManager} and {@link DefaultBootstrapSessionManager} for more details.
     *
     * @param sessionManager the manager responsible to handle bootstrap session.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public LeshanBootstrapServerBuilder setSessionManager(BootstrapSessionManager sessionManager) {
        this.sessionManager = sessionManager;
        return this;
    }

    /**
     * Advanced setter used to customize default bootstrap server behavior.
     * <p>
     * If default bootstrap server behavior is not flexible enough, you can create your own {@link BootstrapHandler} by
     * inspiring yourself from {@link DefaultBootstrapHandler}.
     *
     * @param bootstrapHandlerFactory the factory used to create {@link BootstrapHandler}.
     * @return the builder for fluent Bootstrap Server creation.
     */
    public LeshanBootstrapServerBuilder setBootstrapHandlerFactory(BootstrapHandlerFactory bootstrapHandlerFactory) {
        this.bootstrapHandlerFactory = bootstrapHandlerFactory;
        return this;
    }

    /**
     * <p>
     * Set your {@link LwM2mBootstrapModelProvider} implementation.
     * </p>
     * By default the {@link StandardBootstrapModelProvider}.
     *
     */
    public LeshanBootstrapServerBuilder setObjectModelProvider(LwM2mBootstrapModelProvider objectModelProvider) {
        this.modelProvider = objectModelProvider;
        return this;
    }

    /**
     * <p>
     * Set the {@link LwM2mEncoder} which will encode {@link LwM2mNode} with supported content format.
     * </p>
     * By default the {@link DefaultLwM2mEncoder} is used. It supports Text, Opaque, TLV and JSON format.
     */
    public LeshanBootstrapServerBuilder setEncoder(LwM2mEncoder encoder) {
        this.encoder = encoder;
        return this;
    }

    /**
     * <p>
     * Set the {@link LwM2mDecoder} which will decode data in supported content format to create {@link LwM2mNode}.
     * </p>
     * By default the {@link DefaultLwM2mDecoder} is used. It supports Text, Opaque, TLV and JSON format.
     */
    public LeshanBootstrapServerBuilder setDecoder(LwM2mDecoder decoder) {
        this.decoder = decoder;
        return this;
    }

    /**
     * Set the CoRE Link parser {@link LwM2mLinkParser}
     * <p>
     * By default the {@link DefaultLwM2mLinkParser} is used.
     */
    public void setLinkParser(LwM2mLinkParser linkParser) {
        this.linkParser = linkParser;
    }

    public LeshanBootstrapServerBuilder setEndpointsProvider(LwM2mBootstrapServerEndpointsProvider endpointsProvider) {
        this.endpointsProvider = endpointsProvider;
        return this;
    }

    /**
     * Create the {@link LeshanBootstrapServer}.
     * <p>
     * Next step will be to start it : {@link LeshanBootstrapServer#start()}.
     *
     * @return the LWM2M Bootstrap server.
     * @throws IllegalStateException if builder configuration is not consistent.
     */
    public LeshanBootstrapServer build() {
        if (bootstrapHandlerFactory == null)
            bootstrapHandlerFactory = new BootstrapHandlerFactory() {
                @Override
                public BootstrapHandler create(BootstrapDownlinkRequestSender sender,
                        BootstrapSessionManager sessionManager, BootstrapSessionListener listener) {
                    return new DefaultBootstrapHandler(sender, sessionManager, listener);
                }
            };
        if (configStore == null) {
            configStore = new InMemoryBootstrapConfigStore();
        } else if (sessionManager != null) {
            LOG.warn("configStore is set but you also provide a custom SessionManager so this store will not be used");
        }
        if (modelProvider == null) {
            modelProvider = new StandardBootstrapModelProvider();
        } else if (sessionManager != null) {
            LOG.warn(
                    "modelProvider is set but you also provide a custom SessionManager so this provider will not be used");
        }
        if (sessionManager == null) {
            sessionManager = new DefaultBootstrapSessionManager(securityStore, new SecurityChecker(),
                    new BootstrapConfigStoreTaskProvider(configStore), modelProvider);
        }
        if (encoder == null)
            encoder = new DefaultLwM2mEncoder();
        if (decoder == null)
            decoder = new DefaultLwM2mDecoder();
        if (linkParser == null)
            linkParser = new DefaultLwM2mLinkParser();

        return createBootstrapServer(endpointsProvider, sessionManager, bootstrapHandlerFactory, encoder, decoder,
                linkParser, securityStore,
                new ServerSecurityInfo(privateKey, publicKey, certificateChain, trustedCertificates));
    }

    /**
     * Create the <code>LeshanBootstrapServer</code>.
     * <p>
     * You can extend <code>LeshanBootstrapServerBuilder</code> and override this method to create a new builder which
     * will be able to build an extended <code>LeshanBootstrapServer</code>.
     *
     * @param bsSessionManager the manager responsible to handle bootstrap session.
     * @param bsHandlerFactory the factory used to create {@link BootstrapHandler}.
     * @param decoder decoder used to decode response payload.
     * @param encoder encode used to encode request payload.
     * @param linkParser a parser {@link LwM2mLinkParser} used to parse a CoRE Link.
     * @return the LWM2M Bootstrap server.
     */
    protected LeshanBootstrapServer createBootstrapServer(LwM2mBootstrapServerEndpointsProvider endpointsProvider,
            BootstrapSessionManager bsSessionManager, BootstrapHandlerFactory bsHandlerFactory, LwM2mEncoder encoder,
            LwM2mDecoder decoder, LwM2mLinkParser linkParser, BootstrapSecurityStore securityStore,
            ServerSecurityInfo serverSecurityInfo) {
        return new LeshanBootstrapServer(endpointsProvider, bsSessionManager, bsHandlerFactory, encoder, decoder,
                linkParser, securityStore, serverSecurityInfo);
    }
}
