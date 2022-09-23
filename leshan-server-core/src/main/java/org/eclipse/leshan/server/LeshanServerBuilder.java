/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use Lwm2mEndpointContextMatcher
 *                                                     for secure endpoint.
 *     Achim Kraus (Bosch Software Innovations GmbH) - use CoapEndpointBuilder
 *     Michał Wadowski (Orange)                      - Improved compliance with rfc6690.
 *******************************************************************************/
package org.eclipse.leshan.server;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.security.auth.login.Configuration;

import org.eclipse.leshan.core.link.lwm2m.DefaultLwM2mLinkParser;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLinkParser;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.exception.ClientSleepingException;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpointsProvider;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.StandardModelProvider;
import org.eclipse.leshan.server.queue.ClientAwakeTimeProvider;
import org.eclipse.leshan.server.queue.StaticClientAwakeTimeProvider;
import org.eclipse.leshan.server.registration.InMemoryRegistrationStore;
import org.eclipse.leshan.server.registration.RandomStringRegistrationIdProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationIdProvider;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.security.Authorizer;
import org.eclipse.leshan.server.security.DefaultAuthorizer;
import org.eclipse.leshan.server.security.InMemorySecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityStore;
import org.eclipse.leshan.server.security.ServerSecurityInfo;

/**
 * Class helping you to build and configure a Californium based Leshan Lightweight M2M server. Usage: create it, call
 * the different setters for changing the configuration and then call the {@link #build()} method for creating the
 * {@link LeshanServer} ready to operate.
 */
public class LeshanServerBuilder {

    private RegistrationStore registrationStore;
    private SecurityStore securityStore;
    private LwM2mModelProvider modelProvider;
    private Authorizer authorizer;
    private ClientAwakeTimeProvider awakeTimeProvider;
    private RegistrationIdProvider registrationIdProvider;

    private LwM2mEncoder encoder;
    private LwM2mDecoder decoder;
    private LwM2mLinkParser linkParser;

    private PublicKey publicKey;
    private PrivateKey privateKey;
    private X509Certificate[] certificateChain;
    private Certificate[] trustedCertificates;

    private boolean noQueueMode = false;
    private boolean updateRegistrationOnNotification;

    private LwM2mServerEndpointsProvider endpointProvider;

    /**
     * <p>
     * Set your {@link RegistrationStore} implementation which stores {@link Registration} and {@link Observation}.
     * </p>
     * By default the {@link InMemoryRegistrationStore} implementation is used.
     *
     */
    public LeshanServerBuilder setRegistrationStore(RegistrationStore registrationStore) {
        this.registrationStore = registrationStore;
        return this;
    }

    /**
     * <p>
     * Set your {@link SecurityStore} implementation which stores {@link SecurityInfo}.
     * </p>
     * By default no security store is set. It is needed for secured connection if you are using the defaultAuthorizer
     * or if you want PSK feature activated. An {@link InMemorySecurityStore} is provided to start using secured
     * connection.
     *
     */
    public LeshanServerBuilder setSecurityStore(SecurityStore securityStore) {
        this.securityStore = securityStore;
        return this;
    }

    /**
     * <p>
     * Set your {@link Authorizer} implementation to define if a device if authorize to register to this server.
     * </p>
     * By default the {@link DefaultAuthorizer} implementation is used, it needs a security store to accept secured
     * connection.
     */
    public LeshanServerBuilder setAuthorizer(Authorizer authorizer) {
        this.authorizer = authorizer;
        return this;
    }

    /**
     * <p>
     * Set your {@link LwM2mModelProvider} implementation.
     * </p>
     * By default the {@link StandardModelProvider}.
     */
    public LeshanServerBuilder setObjectModelProvider(LwM2mModelProvider objectModelProvider) {
        this.modelProvider = objectModelProvider;
        return this;
    }

    /**
     * <p>
     * Set the {@link PublicKey} of the server which will be used for RawPublicKey DTLS authentication.
     * </p>
     * This should be used for RPK support only. If you support RPK and X509,
     * {@link LeshanServerBuilder#setCertificateChain(X509Certificate[])} should be used.
     */
    public LeshanServerBuilder setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    /**
     * Set the {@link PrivateKey} of the server which will be used for RawPublicKey(RPK) and X509 DTLS authentication.
     */
    public LeshanServerBuilder setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    /**
     * <p>
     * Set the CertificateChain of the server which will be used for X509 DTLS authentication.
     * </p>
     * For RPK the public key will be extract from the first X509 certificate of the certificate chain. If you only need
     * RPK support, use {@link LeshanServerBuilder#setPublicKey(PublicKey)} instead.
     */
    public <T extends X509Certificate> LeshanServerBuilder setCertificateChain(T[] certificateChain) {
        this.certificateChain = certificateChain;
        return this;
    }

    /**
     * The list of trusted certificates used to authenticate devices.
     */
    public <T extends Certificate> LeshanServerBuilder setTrustedCertificates(T[] trustedCertificates) {
        this.trustedCertificates = trustedCertificates;
        return this;
    }

    /**
     * <p>
     * Set the {@link LwM2mEncoder} which will encode {@link LwM2mNode} with supported content format.
     * </p>
     * By default the {@link DefaultLwM2mEncoder} is used. It supports Text, Opaque, TLV and JSON format.
     */
    public LeshanServerBuilder setEncoder(LwM2mEncoder encoder) {
        this.encoder = encoder;
        return this;
    }

    /**
     * <p>
     * Set the {@link LwM2mDecoder} which will decode data in supported content format to create {@link LwM2mNode}.
     * </p>
     * By default the {@link DefaultLwM2mDecoder} is used. It supports Text, Opaque, TLV and JSON format.
     */
    public LeshanServerBuilder setDecoder(LwM2mDecoder decoder) {
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

    /**
     * deactivate PresenceService which tracks presence of devices using LWM2M Queue Mode. When Queue Mode is
     * deactivated request is always sent immediately and {@link ClientSleepingException} will never be raised.
     * Deactivate QueueMode can make sense if you want to handle it on your own or if you don't plan to support devices
     * with queue mode.
     */
    public LeshanServerBuilder disableQueueModeSupport() {
        this.noQueueMode = true;
        return this;
    }

    /**
     * Sets a new {@link ClientAwakeTimeProvider} object different from the default one.
     * <p>
     * By default a {@link StaticClientAwakeTimeProvider} will be used initialized with the
     * <code>MAX_TRANSMIT_WAIT</code> value available in CoAP {@link Configuration} which should be by default 93s as
     * defined in <a href="https://tools.ietf.org/html/rfc7252#section-4.8.2">RFC7252</a>.
     *
     * @param awakeTimeProvider the {@link ClientAwakeTimeProvider} to set.
     */
    public LeshanServerBuilder setClientAwakeTimeProvider(ClientAwakeTimeProvider awakeTimeProvider) {
        this.awakeTimeProvider = awakeTimeProvider;
        return this;
    }

    /**
     * Sets a new {@link RegistrationIdProvider} object different from the default one (Random string).
     *
     * @param registrationIdProvider the {@link RegistrationIdProvider} to set.
     */
    public void setRegistrationIdProvider(RegistrationIdProvider registrationIdProvider) {
        this.registrationIdProvider = registrationIdProvider;
    }

    /**
     * Update Registration on notification.
     * <p>
     * There is some use cases where device can have a dynamic IP (E.g. NAT environment), the specification says to use
     * an UPDATE request to notify server about IP address/ port changes. But it seems there is some rare use case where
     * this update REQUEST can not be done.
     * <p>
     * With this option you can allow Leshan to update Registration on observe notification. This is clearly OUT OF
     * SPECIFICATION and so this is not recommended and should be used only if there is no other way.
     *
     * For {@code coap://} you probably need to use a the Relaxed response matching mode.
     *
     * <pre>
     * coapConfig.setString(NetworkConfig.Keys.RESPONSE_MATCHING, "RELAXED");
     * </pre>
     *
     * @since 1.1
     *
     * @see <a href=
     *      "https://github.com/eclipse/leshan/wiki/LWM2M-Devices-with-Dynamic-IP#is-the-update-request-mandatory--should-i-update-registration-on-notification-">Dynamic
     *      IP environnement documentaiton</a>
     */
    public LeshanServerBuilder setUpdateRegistrationOnNotification(boolean updateRegistrationOnNotification) {
        this.updateRegistrationOnNotification = updateRegistrationOnNotification;
        return this;
    }

    public LeshanServerBuilder setEndpointsProvider(LwM2mServerEndpointsProvider endpointProvider) {
        this.endpointProvider = endpointProvider;
        return this;
    }

    /**
     * Create the {@link LeshanServer}.
     * <p>
     * Next step will be to start it : {@link LeshanServer#start()}.
     *
     * @return the LWM2M server.
     * @throws IllegalStateException if builder configuration is not consistent.
     */
    public LeshanServer build() {
        if (registrationStore == null)
            registrationStore = new InMemoryRegistrationStore();
        if (authorizer == null)
            authorizer = new DefaultAuthorizer(securityStore);
        if (modelProvider == null)
            modelProvider = new StandardModelProvider();
        if (encoder == null)
            encoder = new DefaultLwM2mEncoder();
        if (decoder == null)
            decoder = new DefaultLwM2mDecoder();
        if (linkParser == null)
            linkParser = new DefaultLwM2mLinkParser();
        if (awakeTimeProvider == null) {
            awakeTimeProvider = new StaticClientAwakeTimeProvider();
        }
        if (registrationIdProvider == null)
            registrationIdProvider = new RandomStringRegistrationIdProvider();

        ServerSecurityInfo serverSecurityInfo = new ServerSecurityInfo(privateKey, publicKey, certificateChain,
                trustedCertificates);

        return createServer(endpointProvider, registrationStore, securityStore, authorizer, modelProvider, encoder,
                decoder, noQueueMode, awakeTimeProvider, registrationIdProvider, linkParser, serverSecurityInfo,
                updateRegistrationOnNotification);
    }

    /**
     * Create the <code>LeshanServer</code>.
     * <p>
     * You can extend <code>LeshanServerBuilder</code> and override this method to create a new builder which will be
     * able to build an extended <code>LeshanServer</code>.
     *
     * @see LeshanServer#LeshanServer(LwM2mServerEndpointsProvider, RegistrationStore, SecurityStore, Authorizer,
     *      LwM2mModelProvider, LwM2mEncoder, LwM2mDecoder, boolean, ClientAwakeTimeProvider, RegistrationIdProvider,
     *      boolean, LwM2mLinkParser, ServerSecurityInfo)
     */
    protected LeshanServer createServer(LwM2mServerEndpointsProvider endpointsProvider,
            RegistrationStore registrationStore, SecurityStore securityStore, Authorizer authorizer,
            LwM2mModelProvider modelProvider, LwM2mEncoder encoder, LwM2mDecoder decoder, boolean noQueueMode,
            ClientAwakeTimeProvider awakeTimeProvider, RegistrationIdProvider registrationIdProvider,
            LwM2mLinkParser linkParser, ServerSecurityInfo serverSecurityInfo,
            boolean updateRegistrationOnNotification) {
        return new LeshanServer(endpointsProvider, registrationStore, securityStore, authorizer, modelProvider, encoder,
                decoder, noQueueMode, awakeTimeProvider, registrationIdProvider, updateRegistrationOnNotification,
                linkParser, serverSecurityInfo);
    }
}
