/*******************************************************************************
 * Copyright (c) 2016 Bosch Software Innovations GmbH and others.
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
 *    Achim Kraus (Bosch Software Innovations GmbH) - Initial contribution
 ******************************************************************************/
package org.eclipse.leshan.server.demo.extensions.clientsetup;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base client setup. Adjust device current time and enables observes. The setup is executed after the registration and
 * a configured {@link ClientSetupConfig#delayInMs}. It uses the {@link #state} to determine the next setup task and
 * increments it. The setup is intended to always start with adjusting the device current time at {@link #state}
 * {@link #STATE_START}, if enabled in configuration {@link ClientSetupConfig#syncTime}. The next steps may be
 * customized by setting {@link #stateStartObserve} to a custom value and implement a
 * {@link #process(LeshanServer, Client, int)} in a derived class. After that, the provided {@link #observes} are
 * enabled and finally {@link #process(LeshanServer, Client, int)} is called until it returns that setup has finished
 * with true.
 */
public abstract class BaseClientSetup {
    private static final Logger LOG = LoggerFactory.getLogger(BaseClientSetup.class);

    /**
     * LWM2M path of security object.
     */
    private static final String LWM2M_SECURITY_OBJECT = "/0/";

    /**
     * State to initialize client setup.
     */
    protected final static int STATE_INIT = 0;
    /**
     * State for waiting for setup. Delay after registration.
     */
    protected final static int STATE_DELAY = 1;
    /**
     * State for starting initialization.
     */
    protected final static int STATE_START = 2;

    /**
     * Endpoint name of client.
     */
    private volatile String endpoint;
    /**
     * Configuration for client setup.
     */
    protected volatile ClientSetupConfig configuration;
    /**
     * State for starting observers during initialization.
     */
    protected volatile int stateStartObserve = STATE_START + 1;
    /**
     * System time in nanos to start initialization.
     * 
     * @see ClientSetupConfig#delayInMs
     */
    private long nanos;
    /**
     * Observes to be established during setup.
     */
    private ObserveRequest[] observes;
    /**
     * Current setup state.
     * 
     * @see #process(LeshanServer, Client)
     */
    private volatile int state = STATE_INIT;

    /**
     * Create a client setup.
     */
    public BaseClientSetup() {
    }

    /**
     * Set configuration for client setup.
     * 
     * @param client client to be setup
     * @param configuration configuration for setup
     */
    public void setConfiguration(Client client, ClientSetupConfig configuration) {
        this.endpoint = client.getEndpoint();
        this.configuration = configuration;
    }

    /**
     * Set observes. May be used from derived classes to overwrite the configured observes
     * {@link ClientSetupConfig#observeUris}. Intended to be called in the context of
     * {@link #process(LeshanServer, Client)}.
     * 
     * @param stateStartObserve state to start enabling observes.
     * @param observes observes to be enabled during setup. May be null, when configured observes should be used.
     */
    protected void enableObserves(int stateStartObserve, ObserveRequest[] observes) {
        this.stateStartObserve = stateStartObserve;
        if (null != observes) {
            this.observes = observes;
        }
    }

    /**
     * Apply configuration for client setup. Intended to be called in the context of
     * {@link #process(LeshanServer, Client)}.
     * 
     * @param client client to be setup
     * @return state number after enabling observations. Used for setup tasks after observes are enabled.
     * @see #STATE_INIT
     */
    public int applyConfiguration(Client client) {
        this.nanos = System.nanoTime() + TimeUnit.NANOSECONDS.convert(configuration.delayInMs, TimeUnit.MILLISECONDS);
        String rootPath = client.getRootPath();
        if (null == this.observes) {
            List<ObserveRequest> observes = new ArrayList<ObserveRequest>(configuration.observeUris.size());
            for (String uri : configuration.observeUris) {
                if (uri.equals("*")) {
                    if (1 == configuration.observeUris.size()) {
                        for (LinkObject link : client.getObjectLinks()) {
                            String url = link.getUrl();
                            if (!url.equals(rootPath) && !url.startsWith(LWM2M_SECURITY_OBJECT)) {
                                LOG.info("Client {} add * observe {}", endpoint, url);
                                observes.add(new ObserveRequest(url));
                            }
                        }
                    }
                } else {
                    if (!uri.startsWith("/")) {
                        LOG.warn("Client setup {} add observe {}?", configuration.name, uri);
                    } else if (hasAvailableInstances(client, uri)) {
                        observes.add(new ObserveRequest(uri));
                        LOG.info("Client {} add observe {}", endpoint, uri);
                    }
                }
            }
            this.observes = observes.toArray(new ObserveRequest[observes.size()]);
        }
        return stateStartObserve + this.observes.length;
    }

    /**
     * Get endpoint name of client.
     * 
     * @return endpoint name of client
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Process client setup. Setup device current time and enables observes. Calls
     * {@link #process(LeshanServer, Client, int)} setup customization.
     * 
     * @param server LWM2M server to send request.
     * @param client client to be setup.
     * @return true, if setup finished, false, otherwise.
     */
    public boolean process(LeshanServer server, Client client) {
        boolean done = false;
        if (STATE_INIT == state) {
            applyConfiguration(client);
            ++state;
        }
        if (System.nanoTime() > nanos) {
            while (true) {
                ++state;
                try {
                    if (STATE_START == state) {
                        LOG.error("start initialization for " + getEndpoint());
                        if (null != configuration && configuration.syncTime) {
                            Date now = new Date();
                            LOG.info("set client time for " + getEndpoint() + " " + now);
                            WriteResponse response = server.send(client, new WriteRequest(3, 0, 13, now));
                            if (null == response || response.getCode() != ResponseCode.CHANGED) {
                                LOG.error("error set time for " + getEndpoint());
                            }
                        }
                    } else if (null != observes && stateStartObserve <= state
                            && observes.length > state - stateStartObserve) {
                        ObserveRequest request = observes[state - stateStartObserve];
                        if (null != request && hasAvailableInstances(client, request.getPath().toString())) {
                            ObserveResponse response = server.send(client, request);
                            if (null == response || response.getCode() != ResponseCode.CONTENT) {
                                LOG.error("error enable observer for " + getEndpoint() + " object " + request.getPath());
                            }
                        } else
                            continue;
                    } else {
                        done = process(server, client, state);
                        if (done)
                            LOG.info("finished initialization for " + getEndpoint());
                    }
                } catch (Throwable ex) {
                    LOG.error("error during initialization for " + getEndpoint(), ex);
                }
                break;
            }
        }

        return done;
    }

    /**
     * Process client setup state. Only called for states other then {@link #STATE_START} and states enabling observes (
     * {@link #stateStartObserve} to {@link #stateStartObserve} + {@link #observes}.length).
     * 
     * @param server LWM2M server to send request.
     * @param client client to be setup.
     * @param state state of setup.
     * @return true, if setup finished, false, otherwise.
     */
    public abstract boolean process(LeshanServer server, Client client, int stateIndex) throws InterruptedException;

    /**
     * Check, if the registration of the client contains the provided LWM2M object instance. Instances of the
     * {@link #LWM2M_SECURITY_OBJECT} will always be excluded.
     * 
     * @param client client of the registration.
     * @param path path to be checked. Paths to resources are truncated to their instance (e.g. "/3/0/13" will be
     *        "/3/0")
     * @return true, if contained in registration, false, otherwise.
     */
    public static boolean hasAvailableInstances(Client client, String path) {
        if (!path.startsWith("/")) {
            LOG.warn("Instance {}?", path);
            return false;
        }
        if (path.startsWith(LWM2M_SECURITY_OBJECT))
            return false;
        String[] resPath = path.split("/", 4);
        if (4 == resPath.length) {
            path = "/" + resPath[1] + "/" + resPath[2];
        }
        int len = path.length();
        for (LinkObject link : client.getObjectLinks()) {
            String url = link.getUrl();
            if (url.startsWith(path)) {
                if (url.length() > len) {
                    return url.charAt(len) == '/';
                } else {
                    return true;
                }
            }
        }
        return false;
    }

}
