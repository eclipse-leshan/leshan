/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.client.engine;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.leshan.client.EndpointsManager;
import org.eclipse.leshan.client.bootstrap.BootstrapHandler;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.request.UplinkRequestSender;
import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.core.request.ContentFormat;

/**
 * A default implementation of {@link RegistrationEngineFactory}.
 * <p>
 * It create a {@link DefaultRegistrationEngine} which could be configured. Look at all setter available in this class.
 */
public class DefaultRegistrationEngineFactory implements RegistrationEngineFactory {

    private long requestTimeoutInMs = 2 * 60 * 1000l; // 2min in ms
    private long deregistrationTimeoutInMs = 1000; // 1s in ms
    private int bootstrapSessionTimeoutInSec = 93;
    private int retryWaitingTimeInMs = 10 * 60 * 1000; // 10min in ms
    private Integer communicationPeriodInMs = null;
    private boolean reconnectOnUpdate = false;
    private boolean resumeOnConnect = true;
    private boolean queueMode = false;
    private ContentFormat preferredContentFormat = ContentFormat.SENML_CBOR;

    public DefaultRegistrationEngineFactory() {
    }

    @Override
    public RegistrationEngine createRegistratioEngine(String endpoint, LwM2mObjectTree objectTree,
            EndpointsManager endpointsManager, UplinkRequestSender requestSender, BootstrapHandler bootstrapState,
            LwM2mClientObserver observer, Map<String, String> additionalAttributes,
            Map<String, String> bsAdditionalAttributes, Set<ContentFormat> supportedContentFormat,
            ScheduledExecutorService sharedExecutor) {
        return new DefaultRegistrationEngine(endpoint, objectTree, endpointsManager, requestSender, bootstrapState,
                observer, additionalAttributes, bsAdditionalAttributes, sharedExecutor, requestTimeoutInMs,
                deregistrationTimeoutInMs, bootstrapSessionTimeoutInSec, retryWaitingTimeInMs, communicationPeriodInMs,
                reconnectOnUpdate, resumeOnConnect, queueMode, preferredContentFormat, supportedContentFormat);
    }

    /**
     * Set the period between 2 communications (update request).
     * <p>
     * Client will communicate periodically to refresh its lifetime but if you want to communication periodically more
     * often you can set a smaller communication period.
     *
     * @param communicationPeriodInMs the communication period in ms
     */
    public void setCommunicationPeriod(Integer communicationPeriodInMs) {
        this.communicationPeriodInMs = communicationPeriodInMs;
    }

    /**
     * Timeout used to send request (bootstrap, register, update) in ms.
     * <p>
     * Default value is 120000 ms (2 min).
     * <p>
     * We choose a default value a bit higher to the MAX_TRANSMIT_WAIT(62-93s) which is the time from starting to send a
     * Confirmable message to the time when an acknowledgement is no longer expected.
     *
     * @param requestTimeoutInMs request timeout in milliseconds.
     * @return this for fluent API
     */
    public DefaultRegistrationEngineFactory setRequestTimeoutInMs(long requestTimeoutInMs) {
        this.requestTimeoutInMs = requestTimeoutInMs;
        return this;
    }

    /**
     * Timeout used to deregister in ms.
     * <p>
     * Default value is 1000ms (1s)
     * <p>
     * We choose a smaller default value than other request timeout to be able to stop/destroy a device quickly.
     *
     * @param deregistrationTimeoutInMs deregistration request timeout in milliseconds.
     * @return this for fluent API
     */
    public DefaultRegistrationEngineFactory setDeregistrationTimeoutInMs(long deregistrationTimeoutInMs) {
        this.deregistrationTimeoutInMs = deregistrationTimeoutInMs;
        return this;
    }

    /**
     * The Bootstrap session timeout in seconds. The session begins on Bootstrap request and ends on Bootstrap finished
     * request.
     * <p>
     * Default value is 93s.
     * <p>
     * 93s is the COAP MAX_TRANSMIT_WAIT with default config.
     *
     * @param bootstrapSessionTimeoutInSec the bootstrap session timeout in milliseconds.
     * @return this for fluent API
     */
    public DefaultRegistrationEngineFactory setBootstrapSessionTimeoutInSec(int bootstrapSessionTimeoutInSec) {
        this.bootstrapSessionTimeoutInSec = bootstrapSessionTimeoutInSec;
        return this;
    }

    /**
     * Time wait before to retry when a bootstrap(or registration if there isn't bootstrap server configured) failed.
     * <p>
     * Default value is 600000ms (10 minutes)
     *
     * @return this for fluent API
     */
    public DefaultRegistrationEngineFactory setRetryWaitingTimeInMs(int retryWaitingTimeInMs) {
        this.retryWaitingTimeInMs = retryWaitingTimeInMs;
        return this;
    }

    /**
     * Configure if client reconnects before update. For DTLS "reconnect" means "initiate a new handshake".
     * <p>
     * Default is false.
     *
     * @param reconnectOnUpdate True is client should reconnect on update
     * @return this for fluent API
     */
    public DefaultRegistrationEngineFactory setReconnectOnUpdate(boolean reconnectOnUpdate) {
        this.reconnectOnUpdate = reconnectOnUpdate;
        return this;
    }

    /**
     * Configure if client tries to resume a session. For DTLS this means that when a new handshake is initiated we will
     * try to do an abbreviated one (instead a full one) if possible.
     * <p>
     * Default value is true
     *
     * @param resumeOnConnect True if client should try to resume on connect
     * @return this for fluent API
     */
    public DefaultRegistrationEngineFactory setResumeOnConnect(boolean resumeOnConnect) {
        this.resumeOnConnect = resumeOnConnect;
        return this;
    }

    /**
     * Configure client to use queueMode.
     * <p>
     * Default value is false
     *
     * @param enable True if client must use queueMode
     * @return this for fluent API
     */
    public DefaultRegistrationEngineFactory setQueueMode(boolean enable) {
        this.queueMode = enable;
        return this;
    }

    /**
     * Define preferred content format for bootstrap session.
     * <p>
     * <code>null</code> can be used for "no preferred content format" but using a preferred content format could give a
     * hint to Bootstrap server to know that your client is a LWM2M v1.1 client as this preferredContentFormat does not
     * exist in LWM2M v1.0
     * <p>
     * Default value is {@link ContentFormat#SENML_CBOR}.
     *
     * @param format preferred content format to use during bootstrap session. It must be SenML JSON, SenML CBOR, or TLV
     *        or <code>null</code> if no preferred content format.
     * @return this for fluent API
     *
     * @see <a href=
     *      "http://www.openmobilealliance.org/release/LightweightM2M/V1_1_1-20190617-A/HTML-Version/OMA-TS-LightweightM2M_Core-V1_1_1-20190617-A.html#6-1-7-1-0-6171-Bootstrap-Request-Operation">6.1.7.1.
     *      Bootstrap-Request Operation</a>
     *
     * @throws IllegalArgumentException if content format is not a valid one.
     */
    public DefaultRegistrationEngineFactory setPreferredContentFormat(ContentFormat format)
            throws IllegalArgumentException {
        // handle null case
        if (format == null) {
            this.preferredContentFormat = format;
            return this;
        }

        // check allowed value
        switch (format.getCode()) {
        case ContentFormat.TLV_CODE:
        case ContentFormat.SENML_CBOR_CODE:
        case ContentFormat.SENML_JSON_CODE:
        case ContentFormat.OLD_TLV_CODE:
            this.preferredContentFormat = format;
            return this;
        default:
            throw new IllegalArgumentException(String
                    .format("Invalid preferred content format %s, it MUST be SenML JSON, SenML CBOR, or TLV", format));
        }
    }
}
