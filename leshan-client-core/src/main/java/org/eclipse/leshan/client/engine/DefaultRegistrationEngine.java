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
package org.eclipse.leshan.client.engine;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.LwM2m;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.client.EndpointsManager;
import org.eclipse.leshan.client.RegistrationUpdate;
import org.eclipse.leshan.client.bootstrap.BootstrapHandler;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.request.LwM2mRequestSender;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.client.servers.DmServerInfo;
import org.eclipse.leshan.client.servers.Server;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.client.servers.ServersInfo;
import org.eclipse.leshan.client.servers.ServersInfoExtractor;
import org.eclipse.leshan.client.util.LinkFormatHelper;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.response.BootstrapResponse;
import org.eclipse.leshan.core.response.DeregisterResponse;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.core.response.UpdateResponse;
import org.eclipse.leshan.util.NamedThreadFactory;
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

    // We choose a default timeout a bit higher to the MAX_TRANSMIT_WAIT(62-93s) which is the time from starting to
    // send a Confirmable message to the time when an acknowledgement is no longer expected.
    private final long requestTimeoutInMs;
    // de-registration is only used on stop/destroy for now.
    private final long deregistrationTimeoutInMs;
    // bootstrap timeout
    private final int bootstrapSessionTimeoutInSec;
    // time between bootstrap retry should be configurable and incremental
    private final int retryWaitingTimeInMs;

    private static enum Status {
        SUCCESS, FAILURE, TIMEOUT
    }

    // device state
    private final String endpoint;
    private final Map<String, String> additionalAttributes;
    private final Map<Integer /* objectId */, LwM2mObjectEnabler> objectEnablers;
    private final Map<String /* registrationId */, Server> registeredServers;

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
            Map<String, String> additionalAttributes, ScheduledExecutorService executor, long requestTimeoutInMs,
            long deregistrationTimeoutInMs, int bootstrapSessionTimeoutInSec, int retryWaitingTimeInMs) {
        this.endpoint = endpoint;
        this.objectEnablers = objectTree.getObjectEnablers();
        this.bootstrapHandler = bootstrapState;
        this.endpointsManager = endpointsManager;
        this.observer = observer;
        this.additionalAttributes = additionalAttributes;
        this.registeredServers = new ConcurrentHashMap<>();
        this.requestTimeoutInMs = requestTimeoutInMs;
        this.deregistrationTimeoutInMs = deregistrationTimeoutInMs;
        this.bootstrapSessionTimeoutInSec = bootstrapSessionTimeoutInSec;
        this.retryWaitingTimeInMs = retryWaitingTimeInMs;

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
            Collection<Server> dmServers = factoryBootstrap();

            if (dmServers == null || dmServers.isEmpty()) {
                // If it failed try client initiated bootstrap
                if (!scheduleClientInitiatedBootstrap(NOW))
                    throw new IllegalStateException("Unable to start client : No valid server available!");
            } else {
                // Try to register to dm servers.
                // TODO we currently support only one dm server.
                Server dmServer = dmServers.iterator().next();
                registerFuture = schedExecutor.submit(new RegistrationTask(dmServer));
            }
        }
    }

    public Collection<Server> factoryBootstrap() {
        ServersInfo serversInfo = ServersInfoExtractor.getInfo(objectEnablers);
        if (!serversInfo.deviceManagements.isEmpty()) {
            Collection<Server> servers = endpointsManager.createEndpoints(serversInfo.deviceManagements.values());
            return servers;
        }
        return null;
    }

    private Collection<Server> clientInitiatedBootstrap() throws InterruptedException {
        ServerInfo bootstrapServerInfo = ServersInfoExtractor.getBootstrapServerInfo(objectEnablers);

        if (bootstrapServerInfo == null) {
            LOG.error("Trying to bootstrap device but there is no bootstrap server config.");
            return null;
        }

        if (bootstrapHandler.tryToInitSession(bootstrapServerInfo)) {
            LOG.info("Trying to start bootstrap session to {} ...", bootstrapServerInfo.getFullUri());

            // Clear all registered server, cancel all current task and recreate all endpoints
            registeredServers.clear();
            cancelRegistrationTask();
            cancelUpdateTask(true);
            Server bootstrapServer = endpointsManager.createEndpoint(bootstrapServerInfo);

            // Send bootstrap request
            try {
                BootstrapResponse response = sender.send(bootstrapServerInfo.getAddress(),
                        bootstrapServerInfo.isSecure(), new BootstrapRequest(endpoint), requestTimeoutInMs);
                if (response == null) {
                    LOG.error("Unable to start bootstrap session: Timeout.");
                    if (observer != null) {
                        observer.onBootstrapTimeout(bootstrapServer);
                    }
                    return null;
                } else if (response.isSuccess()) {
                    LOG.info("Bootstrap started");
                    // Wait until it is finished (or too late)
                    boolean timeout = !bootstrapHandler.waitBoostrapFinished(bootstrapSessionTimeoutInSec);
                    if (timeout) {
                        LOG.error("Bootstrap sequence aborted: Timeout.");
                        if (observer != null) {
                            observer.onBootstrapTimeout(bootstrapServer);
                        }
                        return null;
                    } else {
                        LOG.info("Bootstrap finished {}.", bootstrapServerInfo);
                        ServersInfo serverInfos = ServersInfoExtractor.getInfo(objectEnablers);
                        Collection<Server> dmServers = null;
                        if (!serverInfos.deviceManagements.isEmpty()) {
                            dmServers = endpointsManager.createEndpoints(serverInfos.deviceManagements.values());
                        }
                        if (observer != null) {
                            observer.onBootstrapSuccess(bootstrapServer);
                        }
                        return dmServers;
                    }
                } else {
                    LOG.error("Bootstrap failed: {} {}.", response.getCode(), response.getErrorMessage());
                    if (observer != null) {
                        observer.onBootstrapFailure(bootstrapServer, response.getCode(), response.getErrorMessage());
                    }
                    return null;
                }
            } catch (SendFailedException e) {
                logExceptionOnSendRequest("Unable to send Bootstrap request", e);
                return null;
            } finally {
                bootstrapHandler.closeSession();
            }
        } else {
            LOG.warn("Bootstrap sequence already started.");
            return null;
        }
    }

    private boolean registerWithRetry(Server server) throws InterruptedException {
        Status registerStatus = register(server);
        if (registerStatus == Status.TIMEOUT) {
            // if register timeout maybe server lost the session,
            // so we reconnect (new handshake) and retry
            endpointsManager.forceReconnection(server);
            registerStatus = register(server);
        }
        return registerStatus == Status.SUCCESS;
    }

    private Status register(Server server) throws InterruptedException {
        DmServerInfo dmInfo = ServersInfoExtractor.getDMServerInfo(objectEnablers, server.getId());

        if (dmInfo == null) {
            LOG.error("Trying to register device but there is no LWM2M server config.");
            return Status.FAILURE;
        }

        // Send register request
        LOG.info("Trying to register to {} ...", server.getUri());
        try {
            RegisterRequest regRequest = new RegisterRequest(endpoint, dmInfo.lifetime, LwM2m.VERSION, dmInfo.binding,
                    null, LinkFormatHelper.getClientDescription(objectEnablers.values(), null), additionalAttributes);
            RegisterResponse response = sender.send(dmInfo.getAddress(), dmInfo.isSecure(), regRequest,
                    requestTimeoutInMs);

            if (response == null) {
                LOG.error("Registration failed: Timeout.");
                if (observer != null) {
                    observer.onRegistrationTimeout(server);
                }
                return Status.TIMEOUT;
            } else if (response.isSuccess()) {
                // Add server to registered one
                String registrationID = response.getRegistrationID();
                registeredServers.put(registrationID, server);
                LOG.info("Registered with location '{}'.", registrationID);

                // Update every lifetime period
                long delay = calculateNextUpdate(dmInfo.lifetime);
                scheduleUpdate(server, registrationID, new RegistrationUpdate(), delay);

                if (observer != null) {
                    observer.onRegistrationSuccess(server, registrationID);
                }
                return Status.SUCCESS;
            } else {
                LOG.error("Registration failed: {} {}.", response.getCode(), response.getErrorMessage());
                if (observer != null) {
                    observer.onRegistrationFailure(server, response.getCode(), response.getErrorMessage());
                }
                return Status.FAILURE;
            }
        } catch (SendFailedException e) {
            logExceptionOnSendRequest("Unable to send register request", e);
            return Status.FAILURE;
        }
    }

    private boolean deregister(Server server, String registrationID) throws InterruptedException {
        if (registrationID == null)
            return true;

        DmServerInfo dmInfo = ServersInfoExtractor.getDMServerInfo(objectEnablers, server.getId());
        if (dmInfo == null) {
            LOG.error("Trying to deregister device but there is no LWM2M server config.");
            return false;
        }

        // Send deregister request
        LOG.info("Trying to deregister to {} ...", server.getUri());
        try {
            DeregisterResponse response = sender.send(server.getIdentity().getPeerAddress(),
                    server.getIdentity().isSecure(), new DeregisterRequest(registrationID), deregistrationTimeoutInMs);
            if (response == null) {
                registrationID = null;
                LOG.error("Deregistration failed: Timeout.");
                if (observer != null) {
                    observer.onDeregistrationTimeout(server);
                }
                return false;
            } else if (response.isSuccess() || response.getCode() == ResponseCode.NOT_FOUND) {
                registeredServers.remove(registrationID);
                registrationID = null;
                cancelUpdateTask(true);
                LOG.info("De-register response {} {}.", response.getCode(), response.getErrorMessage());
                if (observer != null) {
                    if (response.isSuccess()) {
                        observer.onDeregistrationSuccess(server, registrationID);
                    } else {
                        observer.onDeregistrationFailure(server, response.getCode(), response.getErrorMessage());
                    }
                }
                return true;
            } else {
                LOG.error("Deregistration failed: {} {}.", response.getCode(), response.getErrorMessage());
                if (observer != null) {
                    observer.onDeregistrationFailure(server, response.getCode(), response.getErrorMessage());
                }
                return false;
            }
        } catch (SendFailedException e) {
            logExceptionOnSendRequest("Unable to send deregister request", e);
            return false;
        }
    }

    private boolean updateWithRetry(Server server, String registrationId, RegistrationUpdate registrationUpdate)
            throws InterruptedException {

        Status updateStatus = update(server, registrationId, registrationUpdate);
        if (updateStatus == Status.TIMEOUT) {
            // if register timeout maybe server lost the session,
            // so we reconnect (new handshake) and retry
            endpointsManager.forceReconnection(server);
            updateStatus = update(server, registrationId, registrationUpdate);
        }
        return updateStatus == Status.SUCCESS;
    }

    private Status update(Server server, String registrationID, RegistrationUpdate registrationUpdate)
            throws InterruptedException {
        DmServerInfo dmInfo = ServersInfoExtractor.getDMServerInfo(objectEnablers, server.getId());
        if (dmInfo == null) {
            LOG.error("Trying to update registration but there is no LWM2M server config.");
            return Status.FAILURE;
        }

        // Send update
        LOG.info("Trying to update registration to {} ...", server.getUri());
        try {
            UpdateResponse response = sender.send(dmInfo.getAddress(), dmInfo.isSecure(),
                    new UpdateRequest(registrationID, registrationUpdate.getLifeTimeInSec(),
                            registrationUpdate.getSmsNumber(), registrationUpdate.getBindingMode(),
                            registrationUpdate.getObjectLinks(), registrationUpdate.getAdditionalAttributes()),
                    requestTimeoutInMs);
            if (response == null) {
                registrationID = null;
                LOG.error("Registration update failed: Timeout.");
                if (observer != null) {
                    observer.onUpdateTimeout(server);
                }
                return Status.TIMEOUT;
            } else if (response.getCode() == ResponseCode.CHANGED) {
                // Update successful, so we reschedule new update
                LOG.info("Registration update succeed.");
                long delay = calculateNextUpdate(dmInfo.lifetime);
                scheduleUpdate(server, registrationID, new RegistrationUpdate(), delay);
                if (observer != null) {
                    observer.onUpdateSuccess(server, registrationID);
                }
                return Status.SUCCESS;
            } else {
                LOG.error("Registration update failed: {} {}.", response.getCode(), response.getErrorMessage());
                if (observer != null) {
                    observer.onUpdateFailure(server, response.getCode(), response.getErrorMessage());
                }
                registeredServers.remove(registrationID);
                return Status.FAILURE;
            }
        } catch (SendFailedException e) {
            logExceptionOnSendRequest("Unable to send update request", e);
            return Status.FAILURE;
        }
    }

    private long calculateNextUpdate(long lifetimeInSeconds) {
        // lifetime - 10%
        // life time is in seconds and we return the delay in milliseconds
        return lifetimeInSeconds * 900l;
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
                    Collection<Server> dmServers = clientInitiatedBootstrap();
                    if (dmServers == null || dmServers.isEmpty()) {
                        // clientInitiatatedBootstrapTask is considered as finished.
                        // see https://github.com/eclipse/leshan/issues/701
                        bootstrapFuture = null;
                        // last thing to do reschedule a new bootstrap.
                        scheduleClientInitiatedBootstrap(retryWaitingTimeInMs);
                    } else {
                        Server dmServer = dmServers.iterator().next();
                        if (!registerWithRetry(dmServer))
                            scheduleRegistrationTask(dmServer, retryWaitingTimeInMs);
                    }
                } catch (InterruptedException e) {
                    LOG.info("Bootstrap task interrupted. ");
                } catch (RuntimeException e) {
                    LOG.error("Unexpected exception during bootstrap task", e);
                }
            }
        }
    }

    private synchronized void scheduleRegistrationTask(Server dmServer, long timeInMs) {
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
        private final Server server;

        public RegistrationTask(Server server) {
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
                }
            }
        }

    }

    private synchronized void scheduleUpdate(Server server, String registrationId,
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
        private final Server server;
        private final String registrationId;
        private final RegistrationUpdate registrationUpdate;

        public UpdateRegistrationTask(Server server, String registrationId, RegistrationUpdate registrationUpdate) {
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
                // TODO we currently support only one dm server.
                if (!registeredServers.isEmpty()) {
                    Entry<String, Server> currentServer = registeredServers.entrySet().iterator().next();
                    deregister(currentServer.getValue(), currentServer.getKey());
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
                // TODO we currently support only one dm server.
                if (!registeredServers.isEmpty()) {
                    Entry<String, Server> currentServer = registeredServers.entrySet().iterator().next();
                    deregister(currentServer.getValue(), currentServer.getKey());
                }
            }
        } catch (InterruptedException e) {
        }
    }

    private class QueueUpdateTask implements Runnable {

        private RegistrationUpdate registrationUpdate;

        public QueueUpdateTask(RegistrationUpdate registrationUpdate) {
            this.registrationUpdate = registrationUpdate;
        }

        @Override
        public void run() {
            synchronized (taskLock) {
                cancelUpdateTask(true);
                // TODO we currently support only one dm server.
                Entry<String, Server> currentServer = registeredServers.entrySet().iterator().next();
                if (currentServer != null) {
                    scheduleUpdate(currentServer.getValue(), currentServer.getKey(), registrationUpdate, NOW);
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
                    schedExecutor.submit(new QueueUpdateTask(registrationUpdate));
                }
            }
        }
    }

    private void logExceptionOnSendRequest(String message, Exception e) {
        if (LOG.isDebugEnabled()) {
            LOG.warn(message, e);
            return;
        }
        if (e instanceof SendFailedException) {
            if (e.getCause() != null && e.getMessage() != null) {
                LOG.warn("{} : {}", message, e.getCause().getMessage());
                return;
            }
        }
        LOG.warn("{} : {}", message, e.getMessage());
    }

    /**
     * @return the current registration Id or <code>null</code> if the client is not registered.
     */
    @Override
    public String getRegistrationId() {
        // TODO we currently support only one dm server.
        Iterator<String> it = registeredServers.keySet().iterator();
        if (it.hasNext()) {
            return it.next();
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
