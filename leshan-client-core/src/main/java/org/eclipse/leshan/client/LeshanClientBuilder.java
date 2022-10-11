/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.client;

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.leshan.client.bootstrap.BootstrapConsistencyChecker;
import org.eclipse.leshan.client.bootstrap.DefaultBootstrapConsistencyChecker;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpointsProvider;
import org.eclipse.leshan.client.engine.DefaultRegistrationEngineFactory;
import org.eclipse.leshan.client.engine.RegistrationEngine;
import org.eclipse.leshan.client.engine.RegistrationEngineFactory;
import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.send.DataSender;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.link.DefaultLinkSerializer;
import org.eclipse.leshan.core.link.LinkSerializer;
import org.eclipse.leshan.core.link.lwm2m.attributes.DefaultLwM2mAttributeParser;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeParser;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.util.Validate;

/**
 * Helper class to build and configure a Californium based Leshan Lightweight M2M client.
 */
public class LeshanClientBuilder {

    private final String endpoint;

    private List<? extends LwM2mObjectEnabler> objectEnablers;
    private List<DataSender> dataSenders;

    private List<Certificate> trustStore;

    private LwM2mEncoder encoder;
    private LwM2mDecoder decoder;
    private LinkSerializer linkSerializer;
    private LwM2mAttributeParser attributeParser;

    private RegistrationEngineFactory engineFactory;
    private Map<String, String> additionalAttributes;
    private Map<String, String> bsAdditionalAttributes;

    private BootstrapConsistencyChecker bootstrapConsistencyChecker;

    private ScheduledExecutorService executor;

    private LwM2mClientEndpointsProvider endpointsProvider;

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
     * Sets the list of objects enablers.
     * <p>
     * The easier way to create {@link LwM2mObjectEnabler} is to use the {@link ObjectsInitializer} but you can
     * implement your own {@link LwM2mObjectEnabler} if you need more flexibility.
     */
    public LeshanClientBuilder setObjects(List<? extends LwM2mObjectEnabler> objectEnablers) {
        this.objectEnablers = objectEnablers;
        return this;
    }

    /**
     * Set the list of {@link DataSender} used by the client.
     * <p>
     * Note that each sender should have a different name.
     */
    public LeshanClientBuilder setDataSenders(DataSender... dataSenders) {
        this.dataSenders = Arrays.asList(dataSenders);

        // check DataSender has name and this name is not shared by several senders
        ArrayList<String> usedNames = new ArrayList<>();
        for (int i = 0; i < dataSenders.length; i++) {
            DataSender dataSender = dataSenders[i];
            if (dataSender.getName() == null) {
                throw new IllegalArgumentException(
                        String.format("%s at index %d have a null name.", dataSender.getClass().getSimpleName(), i));
            }
            if (usedNames.contains(dataSender.getName())) {
                throw new IllegalArgumentException(String.format("name '%s' of %s at index %d is already used.",
                        dataSender.getName(), dataSender.getClass().getSimpleName(), i));
            }
            usedNames.add(dataSender.getName());
        }
        return this;
    }

    /**
     * Set the {@link LwM2mEncoder} which will encode {@link LwM2mNode} with supported content format.
     * <p>
     * By default the {@link DefaultLwM2mEncoder} is used. It supports Text, Opaque, TLV and JSON format.
     */
    public LeshanClientBuilder setEncoder(LwM2mEncoder encoder) {
        this.encoder = encoder;
        return this;
    }

    /**
     * Set the {@link LwM2mDecoder} which will decode data in supported content format to create {@link LwM2mNode}.
     * <p>
     * By default the {@link DefaultLwM2mDecoder} is used. It supports Text, Opaque, TLV and JSON format.
     */
    public LeshanClientBuilder setDecoder(LwM2mDecoder decoder) {
        this.decoder = decoder;
        return this;
    }

    /**
     * Set the CoRE Link serializer {@link LinkSerializer}.
     * <p>
     * By default the {@link DefaultLinkSerializer} is used.
     */
    public LeshanClientBuilder setLinkSerializer(LinkSerializer linkSerializer) {
        this.linkSerializer = linkSerializer;
        return this;
    }

    /**
     * Set the {@link LwM2mAttributeParser} used to parse {@link LwM2mAttribute} from {@link WriteAttributesRequest}.
     * <p>
     * By default the {@link DefaultLwM2mAttributeParser} is used.
     */
    public LeshanClientBuilder setAttributeParser(LwM2mAttributeParser attributeParser) {
        this.attributeParser = attributeParser;
        return this;
    }

    /**
     * Set optional trust store for verifying X.509 server certificates.
     *
     * @param trustStore List of trusted CA certificates
     */
    public LeshanClientBuilder setTrustStore(List<Certificate> trustStore) {
        this.trustStore = trustStore;
        return this;
    }

    /**
     * Set the {@link RegistrationEngineFactory} which is responsible to create the {@link RegistrationEngine}.
     * <p>
     * The {@link RegistrationEngine} is responsible to manage all the client lifecycle
     * (bootstrap/register/update/deregister ...)
     * <p>
     * By default a {@link DefaultRegistrationEngineFactory} is used. Look at this class to change some default timeout
     * value.
     *
     * @return the builder for fluent client creation.
     */
    public LeshanClientBuilder setRegistrationEngineFactory(RegistrationEngineFactory engineFactory) {
        this.engineFactory = engineFactory;
        return this;
    }

    /**
     * Set the additionalAttributes for {@link org.eclipse.leshan.core.request.RegisterRequest}.
     */
    public LeshanClientBuilder setAdditionalAttributes(Map<String, String> additionalAttributes) {
        this.additionalAttributes = additionalAttributes;
        return this;
    }

    /**
     * Set the additionalAttributes for {@link BootstrapRequest}
     *
     * @since 1.1
     */
    public LeshanClientBuilder setBootstrapAdditionalAttributes(Map<String, String> additionalAttributes) {
        this.bsAdditionalAttributes = additionalAttributes;
        return this;
    }

    /**
     * Set a {@link BootstrapConsistencyChecker} which is used to valid client state after a bootstrap session.
     * <p>
     * By default a {@link DefaultBootstrapConsistencyChecker} is used.
     *
     * @return the builder for fluent client creation.
     */
    public LeshanClientBuilder setBootstrapConsistencyChecker(BootstrapConsistencyChecker checker) {
        this.bootstrapConsistencyChecker = checker;
        return this;
    }

    /**
     * Set a shared executor. This executor will be used everywhere it is possible. This is generally used when you want
     * to limit the number of thread to use or if you want to simulate a lot of clients sharing the same thread pool.
     * <p>
     * Currently UDP and DTLS receiver and sender thread could not be share meaning that you will at least consume 2
     * thread by client + the number of thread available in the shared executor (see <a
     * href=https://github.com/eclipse/californium/issues/1203>californium#1203 issue</a>)
     * <p>
     * Executor will not be shutdown automatically on {@link LeshanClient#destroy(boolean)}, this should be done
     * manually.
     *
     * @param executor the executor to share.
     * @return the builder for fluent client creation.
     */
    public LeshanClientBuilder setSharedExecutor(ScheduledExecutorService executor) {
        this.executor = executor;
        return this;
    }

    /**
     * @return the builder for fluent client creation.
     */
    public LeshanClientBuilder setEndpointsProvider(LwM2mClientEndpointsProvider endpointsProvider) {
        this.endpointsProvider = endpointsProvider;
        return this;
    }

    /**
     * Creates an instance of {@link LeshanClient} based on the properties set on this builder.
     */
    public LeshanClient build() {
        if (objectEnablers == null) {
            ObjectsInitializer initializer = new ObjectsInitializer();
            initializer.setInstancesForObject(LwM2mId.SECURITY,
                    Security.noSec("coap://leshan.eclipseprojects.io:5683", 12345));
            initializer.setInstancesForObject(LwM2mId.SERVER, new Server(12345, 5 * 60));
            initializer.setInstancesForObject(LwM2mId.DEVICE,
                    new Device("Eclipse Leshan", "model12345", "12345", EnumSet.of(BindingMode.U)));
            objectEnablers = initializer.createAll();
        }
        if (dataSenders == null)
            dataSenders = new ArrayList<>();
        if (encoder == null)
            encoder = new DefaultLwM2mEncoder();
        if (decoder == null)
            decoder = new DefaultLwM2mDecoder();
        if (linkSerializer == null)
            linkSerializer = new DefaultLinkSerializer();
        if (attributeParser == null)
            attributeParser = new DefaultLwM2mAttributeParser();
        if (engineFactory == null) {
            engineFactory = new DefaultRegistrationEngineFactory();
        }
        if (bootstrapConsistencyChecker == null) {
            bootstrapConsistencyChecker = new DefaultBootstrapConsistencyChecker();
        }

        return createLeshanClient(endpoint, objectEnablers, dataSenders, this.trustStore, engineFactory,
                bootstrapConsistencyChecker, additionalAttributes, bsAdditionalAttributes, encoder, decoder, executor,
                linkSerializer, attributeParser, endpointsProvider);
    }

    /**
     * Create the <code>LeshanClient</code>.
     * <p>
     * You can extend <code>LeshanClientBuilder</code> and override this method to create a new builder which will be
     * able to build an extended <code>LeshanClient </code>.
     * <p>
     * See all the setters of this builder for more documentation about parameters.
     *
     * @param endpoint The endpoint name for this client.
     * @param objectEnablers The list of object enablers. An enabler adds to support for a given LWM2M object to the
     *        client.
     * @param trustStore The optional trust store for verifying X.509 server certificates.
     * @param checker Used to check if client state is consistent after a bootstrap session.
     * @param additionalAttributes Some extra (out-of-spec) attributes to add to the register request.
     * @param bsAdditionalAttributes Some extra (out-of-spec) attributes to add to the bootstrap request.
     * @param encoder used to encode request payload.
     * @param decoder used to decode response payload.
     * @param sharedExecutor an optional shared executor.
     * @param linkSerializer a serializer {@link LinkSerializer} used to serialize a CoRE Link.
     * @param attributeParser a {@link LwM2mAttributeParser} used to parse {@link LwM2mAttribute} from
     *        {@link WriteAttributesRequest}.
     *
     * @return the new {@link LeshanClient}
     */
    protected LeshanClient createLeshanClient(String endpoint, List<? extends LwM2mObjectEnabler> objectEnablers,
            List<DataSender> dataSenders, List<Certificate> trustStore, RegistrationEngineFactory engineFactory,
            BootstrapConsistencyChecker checker, Map<String, String> additionalAttributes,
            Map<String, String> bsAdditionalAttributes, LwM2mEncoder encoder, LwM2mDecoder decoder,
            ScheduledExecutorService sharedExecutor, LinkSerializer linkSerializer,
            LwM2mAttributeParser attributeParser, LwM2mClientEndpointsProvider endpointsProvider) {
        return new LeshanClient(endpoint, objectEnablers, dataSenders, trustStore, engineFactory, checker,
                additionalAttributes, bsAdditionalAttributes, encoder, decoder, sharedExecutor, linkSerializer,
                attributeParser, endpointsProvider);
    }
}
