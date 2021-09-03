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
 *******************************************************************************/
package org.eclipse.leshan.client.engine;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.leshan.client.EndpointsManager;
import org.eclipse.leshan.client.RegistrationUpdate;
import org.eclipse.leshan.client.bootstrap.BootstrapHandler;
import org.eclipse.leshan.client.bootstrap.InvalidStateException;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.request.LwM2mRequestSender;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.client.servers.DmServerInfo;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.client.servers.ServersInfoExtractor;
import org.eclipse.leshan.client.util.LinkFormatHelper;
import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.response.BootstrapResponse;
import org.eclipse.leshan.core.response.DeregisterResponse;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.core.response.UpdateResponse;
import org.eclipse.leshan.core.util.NamedThreadFactory;
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
public class DefaultRegistrationEngine implements RegistrationEngine {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultRegistrationEngine.class);

    private static final long NOW = 0;
    private static final ServerIdentity ALL = new ServerIdentity(null, null);

    // Timeout for bootstrap/register/update request
    private final long requestTimeoutInMs;
    // de-registration is only used on stop/destroy for now.
    private final long deregistrationTimeoutInMs;
    // Bootstrap session timeout
    private final int bootstrapSessionTimeoutInSec;
    // Time between bootstrap retry should incremental
    private final int retryWaitingTimeInMs;
    // Time between 2 update requests (used only if it is smaller than the lifetime)
    private Integer communicationPeriodInMs;
    // True if client should re-initiate a connection (DTLS) on registration update
    private boolean reconnectOnUpdate;
    // True if client should try to resume connection if possible.
    private boolean resumeOnConnect;
    // True if client use queueMode : for now this just add Q parameter on register request.
    private final boolean queueMode;

    private static enum Status {
        SUCCESS, FAILURE, TIMEOUT
    }

    // device state
    private final String endpoint;
    private final ContentFormat preferredContentFormat; // used for bootstrap
    private final Set<ContentFormat> supportedContentFormats;
    private final Map<String, String> additionalAttributes;
    private final Map<String, String> bsAdditionalAttributes; // @since 1.1
    private final Map<Integer /* objectId */, LwM2mObjectEnabler> objectEnablers;
    private final Map<String /* registrationId */, ServerIdentity> registeredServers;
    private final List<ServerIdentity> registeringServers;
    private final AtomicReference<ServerIdentity> currentBoostrapServer;

    // helpers
    private final LwM2mRequestSender sender;
    private final BootstrapHandler bootstrapHandler;
    private final EndpointsManager endpointsManager;
    private final LwM2mClientObserver observer;

    // tasks stuff
    private boolean started = false;
    private Future<?> bootstrapFuture;
    private Future<?> registerFuture;
    private Future<?> updateFuture;
    private Object taskLock = new Object(); // a lock to avoid several task to be executed at the same time
    private final ScheduledExecutorService schedExecutor;
    private final boolean attachedExecutor;

    public DefaultRegistrationEngine(String endpoint, LwM2mObjectTree objectTree, EndpointsManager endpointsManager,
            LwM2mRequestSender requestSender, BootstrapHandler bootstrapState, LwM2mClientObserver observer,
            Map<String, String> additionalAttributes, Map<String, String> bsAdditionalAttributes,
            ScheduledExecutorService executor, long requestTimeoutInMs, long deregistrationTimeoutInMs,
            int bootstrapSessionTimeoutInSec, int retryWaitingTimeInMs, Integer communicationPeriodInMs,
            boolean reconnectOnUpdate, boolean resumeOnConnect, boolean useQueueMode,
            ContentFormat preferredContentFormat, Set<ContentFormat> supportedContentFormats) {
        this.endpoint = endpoint;
        this.objectEnablers = objectTree.getObjectEnablers();
        this.bootstrapHandler = bootstrapState;
        this.endpointsManager = endpointsManager;
        this.observer = observer;
        this.additionalAttributes = additionalAttributes;
        this.bsAdditionalAttributes = bsAdditionalAttributes;
        this.registeredServers = new ConcurrentHashMap<>();
        this.registeringServers = new CopyOnWriteArrayList<>();
        this.currentBoostrapServer = new AtomicReference<>();
        this.requestTimeoutInMs = requestTimeoutInMs;
        this.deregistrationTimeoutInMs = deregistrationTimeoutInMs;
        this.bootstrapSessionTimeoutInSec = bootstrapSessionTimeoutInSec;
        this.retryWaitingTimeInMs = retryWaitingTimeInMs;
        this.communicationPeriodInMs = communicationPeriodInMs;
        this.reconnectOnUpdate = reconnectOnUpdate;
        this.resumeOnConnect = resumeOnConnect;
        this.queueMode = useQueueMode;
        this.preferredContentFormat = preferredContentFormat;
        this.supportedContentFormats = supportedContentFormats;

        if (executor == null) {
            schedExecutor = createScheduledExecutor();
            attachedExecutor = true;
        } else {
            schedExecutor = executor;
            attachedExecutor = false;
        }

        sender = requestSender;
    }

    protected ScheduledExecutorService createScheduledExecutor() {
        return Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("RegistrationEngine#%d"));
    }

    @Override
    public void start() {
        stop(false); // Stop without de-register
        synchronized (this) {
            started = true;
            // Try factory bootstrap
            // TODO support multi server
            ServerIdentity dmServer = factoryBootstrap();

            if (dmServer == null) {
                // If it failed try client initiated bootstrap
                if (!scheduleClientInitiatedBootstrap(NOW))
                    throw new IllegalStateException("Unable to start client : No valid server available!");
            } else {
                registerFuture = schedExecutor.submit(new RegistrationTask(dmServer));
            }
        }
    }

    private ServerIdentity factoryBootstrap() {
        ServerInfo serverInfo = selectServer(ServersInfoExtractor.getInfo(objectEnablers).deviceManagements);
        if (serverInfo != null) {
            return endpointsManager.createEndpoint(serverInfo);
        }
        return null;
    }

    private ServerIdentity clientInitiatedBootstrap() throws InterruptedException {
        ServerInfo bootstrapServerInfo = ServersInfoExtractor.getBootstrapServerInfo(objectEnablers);

        if (bootstrapServerInfo == null) {
            LOG.error("Trying to bootstrap device but there is no bootstrap server config.");
            return null;
        }

        if (bootstrapHandler.tryToInitSession()) {
            LOG.info("Trying to start bootstrap session to {} ...", bootstrapServerInfo.getFullUri());

            // Clear all registered server, cancel all current task and recreate all endpoints
            registeredServers.clear();
            cancelRegistrationTask();
            cancelUpdateTask(true);
            ServerIdentity bootstrapServer = endpointsManager.createEndpoint(bootstrapServerInfo);
            if (bootstrapServer != null) {
                currentBoostrapServer.set(bootstrapServer);
            }

            // Send bootstrap request
            BootstrapRequest request = null;
            try {
                request = new BootstrapRequest(endpoint, preferredContentFormat, bsAdditionalAttributes);
                if (observer != null) {
                    observer.onBootstrapStarted(bootstrapServer, request);
                }
                BootstrapResponse response = sender.send(bootstrapServer, request, requestTimeoutInMs);
                if (response == null) {
                    LOG.info("Unable to start bootstrap session: Timeout.");
                    if (observer != null) {
                        observer.onBootstrapTimeout(bootstrapServer, request);
                    }
                    return null;
                } else if (response.isSuccess()) {
                    LOG.info("Bootstrap started");
                    // Wait until it is finished (or too late)
                    try {
                        boolean timeout = !bootstrapHandler.waitBoostrapFinished(bootstrapSessionTimeoutInSec);
                        if (timeout) {
                            LOG.info("Bootstrap sequence aborted: Timeout.");
                            if (observer != null) {
                                observer.onBootstrapTimeout(bootstrapServer, request);
                            }
                            return null;
                        } else {
                            LOG.info("Bootstrap finished {}.", bootstrapServer.getUri());
                            ServerInfo serverInfo = selectServer(
                                    ServersInfoExtractor.getInfo(objectEnablers).deviceManagements);
                            ServerIdentity dmServer = null;
                            if (serverInfo != null) {
                                dmServer = endpointsManager.createEndpoint(serverInfo);
                            }
                            if (observer != null) {
                                observer.onBootstrapSuccess(bootstrapServer, request);
                            }
                            return dmServer;
                        }
                    } catch (InvalidStateException e) {
                        LOG.info("Bootstrap finished with failure because of consistency check failure.", e);
                        if (observer != null) {
                            observer.onBootstrapFailure(bootstrapServer, request, null, null, e);
                        }
                        return null;
                    }
                } else {
                    LOG.info("Bootstrap failed: {} {}.", response.getCode(), response.getErrorMessage());
                    if (observer != null) {
                        observer.onBootstrapFailure(bootstrapServer, request, response.getCode(),
                                response.getErrorMessage(), null);
                    }
                    return null;
                }
            } catch (RuntimeException e) {
                logExceptionOnSendRequest("Unable to send Bootstrap request", e);
                if (observer != null) {
                    observer.onBootstrapFailure(bootstrapServer, request, null, null, e);
                }
                return null;
            } finally {
                currentBoostrapServer.set(null);
                bootstrapHandler.closeSession();
            }
        } else {
            LOG.warn("Bootstrap sequence already started.");
            return null;
        }
    }

    private boolean registerWithRetry(ServerIdentity server) throws InterruptedException {
        Status registerStatus = register(server);
        if (registerStatus == Status.TIMEOUT) {
            // if register timeout maybe server lost the session,
            // so we reconnect (new handshake) and retry
            endpointsManager.forceReconnection(server, resumeOnConnect);
            registerStatus = register(server);
        }
        return registerStatus == Status.SUCCESS;
    }

    private Status register(ServerIdentity server) throws InterruptedException {
        DmServerInfo dmInfo = ServersInfoExtractor.getDMServerInfo(objectEnablers, server.getId());

        if (dmInfo == null) {
            LOG.info("Trying to register device but there is no LWM2M server config.");
            return Status.FAILURE;
        }

        // Send register request
        LOG.info("Trying to register to {} ...", server.getUri());
        RegisterRequest request = null;
        try {
            LwM2mVersion lwM2mVersion = LwM2mVersion.lastSupported();
            EnumSet<BindingMode> supportedBindingMode = ServersInfoExtractor
                    .getDeviceSupportedBindingMode(objectEnablers.get(LwM2mId.DEVICE), 0);
            Link[] links = LinkFormatHelper.getClientDescription(objectEnablers.values(), null,
                    ContentFormat.getOptionalContentFormatForClient(supportedContentFormats, lwM2mVersion));

            request = new RegisterRequest(endpoint, dmInfo.lifetime, lwM2mVersion.toString(), supportedBindingMode,
                    queueMode, null, links, additionalAttributes);
            if (observer != null) {
                observer.onRegistrationStarted(server, request);
            }
            registeringServers.add(server);
            RegisterResponse response = sender.send(server, request, requestTimeoutInMs);

            if (response == null) {
                LOG.info("Registration failed: Timeout.");
                if (observer != null) {
                    observer.onRegistrationTimeout(server, request);
                }
                return Status.TIMEOUT;
            } else if (response.isSuccess()) {
                // Add server to registered one
                String registrationID = response.getRegistrationID();
                registeredServers.put(registrationID, server);
                LOG.info("Registered with location '{}'.", registrationID);

                // Update every lifetime period
                long delay = calculateNextUpdate(server, dmInfo.lifetime);
                scheduleUpdate(server, registrationID, new RegistrationUpdate(), delay);

                if (observer != null) {
                    observer.onRegistrationSuccess(server, request, registrationID);
                }
                return Status.SUCCESS;
            } else {
                LOG.info("Registration failed: {} {}.", response.getCode(), response.getErrorMessage());
                if (observer != null) {
                    observer.onRegistrationFailure(server, request, response.getCode(), response.getErrorMessage(),
                            null);
                }
                return Status.FAILURE;
            }
        } catch (RuntimeException e) {
            logExceptionOnSendRequest("Unable to send register request", e);
            if (observer != null) {
                observer.onRegistrationFailure(server, request, null, null, e);
            }
            return Status.FAILURE;
        } finally {
            registeringServers.remove(server);
        }
    }

    private boolean deregister(ServerIdentity server, String registrationID) throws InterruptedException {
        if (registrationID == null)
            return true;

        // Send deregister request
        LOG.info("Trying to deregister to {} ...", server.getUri());
        DeregisterRequest request = null;
        try {
            request = new DeregisterRequest(registrationID);
            if (observer != null) {
                observer.onDeregistrationStarted(server, request);
            }
            DeregisterResponse response = sender.send(server, request, deregistrationTimeoutInMs);
            if (response == null) {
                registrationID = null;
                LOG.info("Deregistration failed: Timeout.");
                if (observer != null) {
                    observer.onDeregistrationTimeout(server, request);
                }
                return false;
            } else if (response.isSuccess() || response.getCode() == ResponseCode.NOT_FOUND) {
                registeredServers.remove(registrationID);
                registrationID = null;
                cancelUpdateTask(true);
                LOG.info("De-register response {} {}.", response.getCode(), response.getErrorMessage());
                if (observer != null) {
                    if (response.isSuccess()) {
                        observer.onDeregistrationSuccess(server, request);
                    } else {
                        observer.onDeregistrationFailure(server, request, response.getCode(),
                                response.getErrorMessage(), null);
                    }
                }
                return true;
            } else {
                LOG.info("Deregistration failed: {} {}.", response.getCode(), response.getErrorMessage());
                if (observer != null) {
                    observer.onDeregistrationFailure(server, request, response.getCode(), response.getErrorMessage(),
                            null);
                }
                return false;
            }
        } catch (RuntimeException e) {
            logExceptionOnSendRequest("Unable to send deregister request", e);
            if (observer != null) {
                observer.onDeregistrationFailure(server, request, null, null, e);
            }
            return false;
        }
    }

    private boolean updateWithRetry(ServerIdentity server, String registrationId, RegistrationUpdate registrationUpdate)
            throws InterruptedException {

        Status updateStatus = update(server, registrationId, registrationUpdate);
        if (updateStatus == Status.TIMEOUT) {
            // if register timeout maybe server lost the session,
            // so we reconnect (new handshake) and retry
            endpointsManager.forceReconnection(server, resumeOnConnect);
            updateStatus = update(server, registrationId, registrationUpdate);
        }
        return updateStatus == Status.SUCCESS;
    }

    private Status update(ServerIdentity server, String registrationID, RegistrationUpdate registrationUpdate)
            throws InterruptedException {
        DmServerInfo dmInfo = ServersInfoExtractor.getDMServerInfo(objectEnablers, server.getId());
        if (dmInfo == null) {
            LOG.info("Trying to update registration but there is no LWM2M server config.");
            return Status.FAILURE;
        }

        // Send update
        LOG.info("Trying to update registration to {} (response timeout {}ms)...", server.getUri(), requestTimeoutInMs);
        UpdateRequest request = null;
        try {
            request = new UpdateRequest(registrationID, registrationUpdate.getLifeTimeInSec(),
                    registrationUpdate.getSmsNumber(), registrationUpdate.getBindingMode(),
                    registrationUpdate.getObjectLinks(), registrationUpdate.getAdditionalAttributes());
            if (observer != null) {
                observer.onUpdateStarted(server, request);
            }
            if (reconnectOnUpdate) {
                endpointsManager.forceReconnection(server, resumeOnConnect);
            }
            UpdateResponse response = sender.send(server, request, requestTimeoutInMs);
            if (response == null) {
                registrationID = null;
                LOG.info("Registration update failed: Timeout.");
                if (observer != null) {
                    observer.onUpdateTimeout(server, request);
                }
                return Status.TIMEOUT;
            } else if (response.getCode() == ResponseCode.CHANGED) {
                // Update successful, so we reschedule new update
                LOG.info("Registration update succeed.");
                long delay = calculateNextUpdate(server, dmInfo.lifetime);
                scheduleUpdate(server, registrationID, new RegistrationUpdate(), delay);
                if (observer != null) {
                    observer.onUpdateSuccess(server, request);
                }
                return Status.SUCCESS;
            } else {
                LOG.info("Registration update failed: {} {}.", response.getCode(), response.getErrorMessage());
                if (observer != null) {
                    observer.onUpdateFailure(server, request, response.getCode(), response.getErrorMessage(), null);
                }
                registeredServers.remove(registrationID);
                return Status.FAILURE;
            }
        } catch (RuntimeException e) {
            logExceptionOnSendRequest("Unable to send update request", e);
            if (observer != null) {
                observer.onUpdateFailure(server, request, null, null, e);
            }
            return Status.FAILURE;
        }
    }

    private long calculateNextUpdate(ServerIdentity server, long lifetimeInSeconds) {
        long maxComminucationPeriod = endpointsManager.getMaxCommunicationPeriodFor(server, lifetimeInSeconds * 1000);
        if (communicationPeriodInMs != null) {
            return Math.min(communicationPeriodInMs, maxComminucationPeriod);
        } else {
            return maxComminucationPeriod;
        }
    }

    private synchronized boolean scheduleClientInitiatedBootstrap(long timeInMs) {
        if (!started)
            return false;

        ServerInfo bootstrapServerInfo = ServersInfoExtractor.getBootstrapServerInfo(objectEnablers);
        if (bootstrapServerInfo == null) {
            // It seems we have no bootstrap server available in this case we can't schedule a new bootstraps
            return false;
        }

        // Schedule a client initiated bootstrap only if there is not already one scheduled or in execution
        if (bootstrapFuture == null || bootstrapFuture.isDone() || bootstrapFuture.isCancelled()) {
            if (timeInMs > 0) {
                LOG.info("Try to initiated bootstarp in {}s...", timeInMs / 1000);
                bootstrapFuture = schedExecutor.schedule(new ClientInitiatedBootstrapTask(), timeInMs,
                        TimeUnit.MILLISECONDS);
            } else {
                bootstrapFuture = schedExecutor.submit(new ClientInitiatedBootstrapTask());
            }
        }
        // We succeed to schedule a bootstrap or there is already one schedule so it's ok.
        return true;
    }

    private class ClientInitiatedBootstrapTask implements Runnable {
        @Override
        public void run() {
            synchronized (taskLock) {
                try {
                    ServerIdentity dmServer = clientInitiatedBootstrap();
                    if (dmServer == null) {
                        // clientInitiatatedBootstrapTask is considered as finished.
                        // see https://github.com/eclipse/leshan/issues/701
                        bootstrapFuture = null;
                        // last thing to do reschedule a new bootstrap.
                        scheduleClientInitiatedBootstrap(retryWaitingTimeInMs);
                    } else {
                        if (!registerWithRetry(dmServer))
                            scheduleRegistrationTask(dmServer, retryWaitingTimeInMs);
                    }
                } catch (InterruptedException e) {
                    LOG.info("Bootstrap task interrupted. ");
                } catch (RuntimeException e) {
                    LOG.error("Unexpected exception during bootstrap task", e);
                    observer.onUnexpectedError(e);
                }
            }
        }
    }

    private synchronized void scheduleRegistrationTask(ServerIdentity dmServer, long timeInMs) {
        if (!started)
            return;

        if (timeInMs > 0) {
            LOG.info("Try to register to {} again in {}s...", dmServer.getUri(), timeInMs / 1000);
            registerFuture = schedExecutor.schedule(new RegistrationTask(dmServer), timeInMs, TimeUnit.MILLISECONDS);
        } else {
            registerFuture = schedExecutor.submit(new RegistrationTask(dmServer));
        }
        return;
    }

    private class RegistrationTask implements Runnable {
        private final ServerIdentity server;

        public RegistrationTask(ServerIdentity server) {
            this.server = server;
        }

        @Override
        public void run() {
            synchronized (taskLock) {
                try {
                    if (!registerWithRetry(server)) {
                        if (!scheduleClientInitiatedBootstrap(NOW)) {
                            scheduleRegistrationTask(server, retryWaitingTimeInMs);
                        }
                    }
                } catch (InterruptedException e) {
                    LOG.info("Registration task interrupted. ");
                } catch (RuntimeException e) {
                    LOG.error("Unexpected exception during registration task", e);
                    observer.onUnexpectedError(e);
                }
            }
        }

    }

    private synchronized void scheduleUpdate(ServerIdentity server, String registrationId,
            RegistrationUpdate registrationUpdate, long timeInMs) {
        if (!started)
            return;

        if (timeInMs > 0) {
            LOG.info("Next registration update to {} in {}s...", server.getUri(), timeInMs / 1000);
            updateFuture = schedExecutor.schedule(
                    new UpdateRegistrationTask(server, registrationId, registrationUpdate), timeInMs,
                    TimeUnit.MILLISECONDS);
        } else {
            updateFuture = schedExecutor.submit(new UpdateRegistrationTask(server, registrationId, registrationUpdate));
        }
    }

    private class UpdateRegistrationTask implements Runnable {
        private final ServerIdentity server;
        private final String registrationId;
        private final RegistrationUpdate registrationUpdate;

        public UpdateRegistrationTask(ServerIdentity server, String registrationId,
                RegistrationUpdate registrationUpdate) {
            this.server = server;
            this.registrationId = registrationId;
            this.registrationUpdate = registrationUpdate;
        }

        @Override
        public void run() {
            synchronized (taskLock) {
                try {
                    if (!updateWithRetry(server, registrationId, registrationUpdate)) {
                        if (!registerWithRetry(server)) {
                            if (!scheduleClientInitiatedBootstrap(NOW)) {
                                scheduleRegistrationTask(server, retryWaitingTimeInMs);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    LOG.info("Registration update task interrupted.");
                } catch (RuntimeException e) {
                    LOG.error("Unexpected exception during update registration task", e);
                    observer.onUnexpectedError(e);
                }
            }
        }
    }

    private void cancelUpdateTask(boolean mayinterrupt) {
        if (updateFuture != null) {
            updateFuture.cancel(mayinterrupt);
        }
    }

    private void cancelRegistrationTask() {
        if (registerFuture != null) {
            registerFuture.cancel(true);
        }
    }

    private void cancelBootstrapTask() {
        if (bootstrapFuture != null) {
            bootstrapFuture.cancel(true);
        }
    }

    @Override
    public void stop(boolean deregister) {
        synchronized (this) {
            if (!started)
                return;
            started = false;
            cancelUpdateTask(true);
            cancelRegistrationTask();
            // TODO we should manage the case where we stop in the middle of a bootstrap session ...
            cancelBootstrapTask();
        }
        try {
            if (deregister) {
                if (!registeredServers.isEmpty()) {
                    for (Entry<String, ServerIdentity> registeredServer : registeredServers.entrySet()) {
                        deregister(registeredServer.getValue(), registeredServer.getKey());
                    }
                }
            }
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void destroy(boolean deregister) {
        boolean wasStarted = false;
        synchronized (this) {
            wasStarted = started;
            started = false;
        }
        try {
            // TODO we should manage the case where we stop in the middle of a bootstrap session ...
            if (attachedExecutor) {
                schedExecutor.shutdownNow();
                schedExecutor.awaitTermination(bootstrapSessionTimeoutInSec, TimeUnit.SECONDS);
            } else {
                cancelUpdateTask(true);
                cancelRegistrationTask();
                // TODO we should manage the case where we stop in the middle of a bootstrap session ...
                cancelBootstrapTask();
            }
            if (wasStarted && deregister) {
                if (!registeredServers.isEmpty()) {
                    for (Entry<String, ServerIdentity> registeredServer : registeredServers.entrySet()) {
                        deregister(registeredServer.getValue(), registeredServer.getKey());
                    }
                }
            }
        } catch (InterruptedException e) {
        }
    }

    private class QueueUpdateTask implements Runnable {

        private RegistrationUpdate registrationUpdate;
        private ServerIdentity server;

        public QueueUpdateTask(ServerIdentity server, RegistrationUpdate registrationUpdate) {
            this.registrationUpdate = registrationUpdate;
            this.server = server;
        }

        @Override
        public void run() {
            synchronized (taskLock) {
                cancelUpdateTask(true);
                if (ALL.equals(server)) {
                    // TODO support multi server
                    Entry<String, ServerIdentity> currentServer = registeredServers.entrySet().iterator().next();
                    if (currentServer != null) {
                        scheduleUpdate(currentServer.getValue(), currentServer.getKey(), registrationUpdate, NOW);
                    }
                } else {
                    String registrationId = getRegistrationId(server);
                    if (registrationId != null) {
                        scheduleUpdate(server, registrationId, registrationUpdate, NOW);
                    }
                }
            }
        }
    }

    @Override
    public void triggerRegistrationUpdate() {
        triggerRegistrationUpdate(new RegistrationUpdate());
    }

    @Override
    public void triggerRegistrationUpdate(RegistrationUpdate registrationUpdate) {
        synchronized (this) {
            if (started) {
                LOG.info("Triggering registration update...");
                if (registeredServers.isEmpty()) {
                    LOG.info("No server registered!");
                } else {
                    schedExecutor.submit(new QueueUpdateTask(ALL, registrationUpdate));
                }
            }
        }
    }

    @Override
    public void triggerRegistrationUpdate(ServerIdentity server) {
        triggerRegistrationUpdate(server, new RegistrationUpdate());
    }

    @Override
    public void triggerRegistrationUpdate(ServerIdentity server, RegistrationUpdate registrationUpdate) {
        if (server == null)
            return;

        synchronized (this) {
            if (started) {
                LOG.info("Triggering registration update...");
                if (registeredServers.isEmpty()) {
                    LOG.info("No server registered!");
                } else {
                    schedExecutor.submit(new QueueUpdateTask(server, registrationUpdate));
                }
            }
        }
    }

    @Override
    public boolean triggerClientInitiatedBootstrap(final boolean deregister) {
        synchronized (this) {
            if (started) {

                // check if we have a bootstrap server
                ServerInfo bootstrapServerInfo = ServersInfoExtractor.getBootstrapServerInfo(objectEnablers);
                if (bootstrapServerInfo == null) {
                    return false;
                }

                // stop every
                cancelUpdateTask(true);
                cancelRegistrationTask();
                // TODO we should manage the case where we stop in the middle of a bootstrap session ...
                cancelBootstrapTask();

                schedExecutor.submit(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            // deregister if needed
                            if (deregister) {
                                if (!registeredServers.isEmpty()) {
                                    for (Entry<String, ServerIdentity> registeredServer : registeredServers
                                            .entrySet()) {
                                        deregister(registeredServer.getValue(), registeredServer.getKey());
                                    }
                                }
                            }
                        } catch (InterruptedException e) {
                        }

                        // schedule a new bootstrap.
                        scheduleClientInitiatedBootstrap(NOW);
                    }
                });
            }
        }
        return true;
    }

    private void logExceptionOnSendRequest(String message, Exception e) {
        if (LOG.isDebugEnabled()) {
            LOG.warn(message, e);
            return;
        }
        if (e instanceof SendFailedException) {
            if (e.getCause() != null && e.getMessage() != null) {
                LOG.info("{} : {}", message, e.getCause().getMessage());
                return;
            }
        }
        LOG.info("{} : {}", message, e.getMessage());
    }

    @Override
    public String getRegistrationId(ServerIdentity server) {
        if (server == null)
            return null;
        for (Entry<String, ServerIdentity> entry : registeredServers.entrySet()) {
            if (server.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public Map<String, ServerIdentity> getRegisteredServers() {
        return Collections.unmodifiableMap(registeredServers);
    }

    @Override
    public ServerIdentity getRegisteredServer(long serverId) {
        for (ServerIdentity server : registeringServers) {
            if (server != null && server.getId() == serverId) {
                return server;
            }
        }
        for (Entry<String, ServerIdentity> entry : registeredServers.entrySet()) {
            ServerIdentity server = entry.getValue();
            if (server != null && server.getId() == serverId) {
                return server;
            }
        }
        return null;
    }

    @Override
    public boolean isAllowedToCommunicate(ServerIdentity foreingPeer) {
        if (foreingPeer == null)
            return false;
        ServerIdentity bootstrapServer = currentBoostrapServer.get();
        if (bootstrapServer != null && foreingPeer.equals(bootstrapServer)) {
            return true;
        } else {
            for (ServerIdentity server : registeringServers) {
                if (foreingPeer.equals(server)) {
                    return true;
                }
            }
            for (ServerIdentity server : registeredServers.values()) {
                if (foreingPeer.equals(server)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This class support to be connected to only one LWM2M server. This methods select the server to be used. Default
     * implementation select the first one.
     */
    protected DmServerInfo selectServer(Map<Long, DmServerInfo> servers) {
        if (servers != null && !servers.isEmpty()) {
            if (servers.size() > 1) {
                LOG.warn(
                        "DefaultRegistrationEngine support only connection to 1 LWM2M server, first server will be used from the server list of {}",
                        servers.size());
                TreeMap<Long, DmServerInfo> sortedServers = new TreeMap<>(servers);
                return sortedServers.values().iterator().next();
            } else {
                return servers.values().iterator().next();
            }
        }
        return null;
    }

    /**
     * @return the LWM2M client endpoint identifier.
     */
    @Override
    public String getEndpoint() {
        return endpoint;
    }
}
