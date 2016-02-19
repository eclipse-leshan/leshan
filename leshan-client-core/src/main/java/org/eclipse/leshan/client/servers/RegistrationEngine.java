/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
 *******************************************************************************/
package org.eclipse.leshan.client.servers;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.client.request.LwM2mClientRequestSender;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.util.LinkFormatHelper;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.response.BootstrapResponse;
import org.eclipse.leshan.core.response.DeregisterResponse;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.core.response.UpdateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage the registration life-cycle:
 * <ul>
 * <li>Start bootstrap session if no device Management server is available</li>
 * <li>Register to device management server when available (at startup or after bootstrap)</li>
 * <li>Update registration periodically.</li>
 * <li>If communication failed with device management server, try to bootstrap again each 10 minutes until succeed</li>
 * </ul>
 * <br>
 * <b>For now support only one device management server.</b>
 */
public class RegistrationEngine {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrationEngine.class);
    // TODO bootstrap timeout should be configurable
    private static final int BS_TIMEOUT = 93; // in seconds (93s is the COAP MAX_TRANSMIT_WAIT with default config)
    private static final long DEREGISTRATION_TIMEOUT = 1000; // in ms, de-registration is only used on stop for now.
    // TODO time between bootstrap retry should be configurable and incremental
    private static final int BS_RETRY = 10 * 60; // in seconds

    private final String endpoint;
    private final LwM2mClientRequestSender sender;
    private final Map<Integer, LwM2mObjectEnabler> objectEnablers;
    private final BootstrapHandler bootstrapHandler;

    // registration update
    private String registrationID;
    private ScheduledFuture<?> updateFuture;
    private final ScheduledExecutorService schedExecutor = Executors.newScheduledThreadPool(2);

    public RegistrationEngine(String endpoint, Map<Integer, LwM2mObjectEnabler> objectEnablers,
            LwM2mClientRequestSender requestSender, BootstrapHandler bootstrapState) {
        this.endpoint = endpoint;
        this.objectEnablers = objectEnablers;
        this.bootstrapHandler = bootstrapState;

        sender = requestSender;
    }

    public void start() {
        schedExecutor.submit(new RegistrationTask());
    }

    private boolean bootstrap() throws InterruptedException {
        ServersInfo serversInfo = ServersInfoExtractor.getInfo(objectEnablers);

        if (serversInfo.bootstrap == null) {
            LOG.error("Trying to bootstrap device but there is no bootstrap server config.");
            return false;
        }

        if (bootstrapHandler.tryToInitSession(serversInfo.bootstrap)) {
            LOG.info("Trying to start bootstrap session to {} ...", serversInfo.bootstrap.serverUri);
            try {
                // Send bootstrap request
                ServerInfo boostrapServer = serversInfo.bootstrap;
                BootstrapResponse response = sender.send(boostrapServer.getAddress(), boostrapServer.isSecure(),
                        new BootstrapRequest(endpoint), null);
                if (response == null) {
                    LOG.error("Unable to start bootstrap session: Timeout.");
                    return false;
                } else if (response.isSuccess()) {
                    LOG.info("Bootstrap started");
                    // wait until it is finished (or too late)
                    boolean timeout = !bootstrapHandler.waitBoostrapFinished(BS_TIMEOUT);
                    if (timeout) {
                        LOG.error("Bootstrap sequence aborted: Timeout.");
                        return false;
                    } else {
                        serversInfo = ServersInfoExtractor.getInfo(objectEnablers);
                        LOG.info("Bootstrap finished {}.", serversInfo);
                        return true;
                    }
                } else {
                    LOG.error("Bootstrap failed: {}.", response.getCode());
                    return false;
                }
            } finally {
                bootstrapHandler.closeSession();
            }
        } else {
            LOG.warn("Bootstrap sequence already started.");
            return false;
        }
    }

    private boolean register() throws InterruptedException {
        ServersInfo serversInfo = ServersInfoExtractor.getInfo(objectEnablers);
        DmServerInfo dmInfo = serversInfo.deviceMangements.values().iterator().next();

        if (dmInfo == null) {
            LOG.error("Trying to register device but there is no LWM2M server config.");
            return false;
        }

        // send register request
        LOG.info("Trying to register to {} ...", dmInfo.serverUri);
        RegisterResponse response = sender.send(
                dmInfo.getAddress(),
                dmInfo.isSecure(),
                new RegisterRequest(endpoint, dmInfo.lifetime, null, dmInfo.binding, null, LinkFormatHelper
                        .getClientDescription(objectEnablers.values(), null), null), null);
        if (response == null) {
            registrationID = null;
            LOG.error("Registration failed: Timeout.");
        } else if (response.isSuccess()) {
            registrationID = response.getRegistrationID();

            // update every lifetime period
            scheduleUpdate(dmInfo);

            LOG.info("Registered with location '{}'.", response.getRegistrationID());
        } else {
            registrationID = null;
            LOG.error("Registration failed: {}.", response.getCode());
        }

        return registrationID != null;
    }

    private boolean deregister() throws InterruptedException {
        if (registrationID == null)
            return true;

        ServersInfo serversInfo = ServersInfoExtractor.getInfo(objectEnablers);
        DmServerInfo dmInfo = serversInfo.deviceMangements.values().iterator().next();
        if (dmInfo == null) {
            LOG.error("Trying to deregister device but there is no LWM2M server config.");
            return false;
        }

        // Send deregister request
        LOG.info("Trying to deregister to {} ...", dmInfo.serverUri);
        DeregisterResponse response = sender.send(dmInfo.getAddress(), dmInfo.isSecure(), new DeregisterRequest(
                registrationID), DEREGISTRATION_TIMEOUT);
        if (response == null) {
            registrationID = null;
            LOG.error("Deregistration failed: Timeout.");
            return false;
        } else if (response.isSuccess() || response.getCode() == ResponseCode.NOT_FOUND) {
            registrationID = null;
            cancelUpdateTask();
            LOG.info("De-register response {} {}.", response.getCode(), response.getErrorMessage());
            return true;
        } else {
            LOG.error("Deregistration failed: {} {}.", response.getCode(), response.getErrorMessage());
            return false;
        }
    }

    private boolean update() throws InterruptedException {
        cancelUpdateTask();

        ServersInfo serversInfo = ServersInfoExtractor.getInfo(objectEnablers);
        DmServerInfo dmInfo = serversInfo.deviceMangements.values().iterator().next();
        if (dmInfo == null) {
            LOG.error("Trying to update registration but there is no LWM2M server config.");
            return false;
        }

        // Send update
        LOG.info("Trying to update registration to {} ...", dmInfo.serverUri);
        final UpdateResponse response = sender.send(dmInfo.getAddress(), dmInfo.isSecure(), new UpdateRequest(
                registrationID, null, null, null, null), null);
        if (response == null) {
            registrationID = null;
            LOG.error("Registration update failed: Timeout.");
            return false;
        } else if (response.getCode() == ResponseCode.CHANGED) {
            // Update successful, so we reschedule new update
            scheduleUpdate(dmInfo);
            LOG.info("Registration update succeed.");
            return true;
        } else {
            LOG.error("Registration update failed: {} {}.", response.getCode(), response.getErrorMessage());
            return false;
        }

    }

    private class RegistrationTask implements Runnable {
        @Override
        public void run() {
            try {
                ServersInfo serversInfo = ServersInfoExtractor.getInfo(objectEnablers);
                if (!serversInfo.deviceMangements.isEmpty()) {
                    if (!register()) {
                        if (!bootstrap()) {
                            scheduleRegistration();
                        } else {
                            if (!register()) {
                                scheduleRegistration();
                            }
                        }
                    }
                } else {
                    if (!bootstrap()) {
                        scheduleRegistration();
                    } else {
                        if (!register()) {
                            scheduleRegistration();
                        }
                    }
                }
            } catch (InterruptedException e) {
                LOG.info("Registration task interrupted.");
            }
        }
    }

    private void scheduleRegistration() {
        LOG.info("Unable to connect to any server, next retry in {}s...", BS_RETRY);
        schedExecutor.schedule(new RegistrationTask(), BS_RETRY, TimeUnit.SECONDS);
    }

    private void scheduleUpdate(DmServerInfo dmInfo) {
        // calculate next update : lifetime - 10%
        // dmInfo.lifetime is in seconds
        long nextUpdate = dmInfo.lifetime * 900l;
        LOG.info("Next registration update in {}s...", nextUpdate / 1000.0);
        updateFuture = schedExecutor.schedule(new UpdateRegistrationTask(), nextUpdate, TimeUnit.MILLISECONDS);
    }

    private class UpdateRegistrationTask implements Runnable {
        @Override
        public void run() {
            try {
                if (!update()) {
                    if (!register()) {
                        if (!bootstrap()) {
                            scheduleRegistration();
                        } else {
                            if (!register()) {
                                scheduleRegistration();
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                LOG.info("Registration update task interrupted.");
            }
        }
    }

    private void cancelUpdateTask() {
        if (updateFuture != null) {
            updateFuture.cancel(false);
        }
    }

    public void stop(boolean deregister) {
        // TODO we should manage the case where we stop in the middle of a bootstrap session ...
        schedExecutor.shutdownNow();
        try {
            schedExecutor.awaitTermination(BS_TIMEOUT, TimeUnit.SECONDS);
            if (deregister)
                deregister();
        } catch (InterruptedException e) {
        }
    }
}
