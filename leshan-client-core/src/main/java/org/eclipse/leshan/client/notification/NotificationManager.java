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
package org.eclipse.leshan.client.notification;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.client.notification.NotificationDataStore.NotificationData;
import org.eclipse.leshan.client.request.DownlinkRequestReceiver;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.client.resource.NotificationSender;
import org.eclipse.leshan.client.resource.listener.ObjectsListenerAdapter;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.link.lwm2m.attributes.NotificationAttributeTree;
import org.eclipse.leshan.core.node.LwM2mChildNode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible to modify default notification behavior based on write attributes.
 */
public class NotificationManager {

    private final Logger LOG = LoggerFactory.getLogger(NotificationManager.class);

    private final DownlinkRequestReceiver receiver;
    private final NotificationDataStore store;
    private final LwM2mObjectTree objectTree;
    private final NotificationStrategy strategy = new NotificationStrategy();
    // TODO should be configurable and should be destroyed when needed.
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public NotificationManager(LwM2mObjectTree objectTree, DownlinkRequestReceiver requestReceiver) {
        this.receiver = requestReceiver;
        this.objectTree = objectTree;
        this.store = new NotificationDataStore();

        // Housekeeping
        objectTree.addListener(new ObjectsListenerAdapter() {
            @Override
            public void objectInstancesRemoved(LwM2mObjectEnabler object, int... instanceIds) {
                // TODO cleaning remove data
            }

            @Override
            public void objectRemoved(LwM2mObjectEnabler object) {
                // TODO cleaning remove data
            }
        });
    }

    // TODO an optimization could be to synchronize by observe relation (identify by server / request)
    public synchronized void initRelation(LwM2mServer server, ObserveRequest request, LwM2mNode node,
            NotificationSender sender) {
        // Get Attributes from ObjectTree
        NotificationAttributeTree attributes = getAttributes(server, request);

        // If there is no attributes this is just classic observe so nothing to do.
        if (attributes == null)
            return;

        LOG.debug("Handle observe relation for {} / {}", server, request);

        // Store needed data for this observe relation.
        updateNotificationData(server, request, attributes, node, sender);
    }

    protected NotificationAttributeTree getAttributes(LwM2mServer server, ObserveRequest request) {
        // TODO use objectTree to get attribute;
        // for testing, we return hardcoded value for resource 6/0/1

        LwM2mObjectEnabler objectEnabler = objectTree.getObjectEnabler(request.getPath().getObjectId());
        if (objectEnabler == null)
            return null;
        else {
            return objectEnabler.getAttributesFor(request.getPath());
        }

    }

    // TODO an optimization could be to synchronize by observe relation (identify by server / request)
    public synchronized void notificationTriggered(LwM2mServer server, ObserveRequest request,
            NotificationSender sender) {
        LOG.trace("Notification triggered for observe relation of {} / {}", server, request);

        // Get Notification Data for given server / request
        NotificationData notificationData = store.getNotificationData(server, request);
        if (notificationData == null) {
            // if there no notification data, this is classic observe (without notification attributes)
            ObserveResponse observeResponse = createResponse(server, request);
            sender.sendNotification(observeResponse);
            return;
        }

        // ELSE handle Notification Attributes.
        NotificationAttributeTree attributes = notificationData.getAttributes();

        // if there is criteria based on value
        if (notificationData.hasCriteriaBasedOnValue()) {
            ObserveResponse response = createResponse(server, request);
            if (response.isSuccess()) {
                LwM2mChildNode newValue = response.getContent();

                // if criteria doesn't match do not raise any event.
                if (!strategy.shouldTriggerNotificationBasedOnValueChange(request.getPath(), attributes,
                        notificationData.getLastSentValue(), newValue)) {
                    return;
                }
            }
            // TODO handle error ?
        }

        // TODO use case where PMIN == PMAX
        // If PMIN is used check if we need to delay this notification.
        if (notificationData.usePmin()) {
            LOG.trace("handle pmin for observe relation of {} / {}", server, request);

            if (notificationData.pminTaskScheduled()) {
                // nothing to do if a task is already scheduled
                return;
            }

            // calculate time since last notification
            Long timeSinceLastNotification = TimeUnit.SECONDS
                    .convert(System.nanoTime() - notificationData.getLastSendingTime(), TimeUnit.NANOSECONDS);
            Long pmin = strategy.getPmin(request.getPath(), attributes);
            if (timeSinceLastNotification < pmin) {
                ScheduledFuture<Void> pminTask = executor.schedule(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        sendNotification(server, request, attributes, sender);
                        return null;
                    }
                }, pmin - timeSinceLastNotification, TimeUnit.SECONDS);
                // schedule next task for pmin but do not send notification
                store.addNotificationData(server, request, new NotificationData(notificationData, pminTask));
                return;
            }
        }

        sendNotification(server, request, attributes, sender);
    }

    // TODO an optimization could be to synchronize by observe relation (identify by server / request)
    public synchronized void clear(LwM2mServer server, ObserveRequest request) {
        // remove all data about observe relation for given server / request.
        store.removeNotificationData(server, request);
    }

    // TODO an optimization could be to synchronize by observe relation (identify by server / request)
    public synchronized void clear(LwM2mServer server) {
        // remove all data about observe relation for given server.
        store.clearAllNotificationDataFor(server);
    }

    // TODO an optimization could be to synchronize by observe relation (identify by server / request)
    public synchronized void clear() {
        // remove all data about observe relation.
        store.clearAllNotificationData();
    }

    // TODO an optimization could be to synchronize by observe relation (identify by server / request)
    protected synchronized void updateNotificationData(LwM2mServer server, ObserveRequest request,
            NotificationAttributeTree attributes, LwM2mNode newValue, NotificationSender sender) {
        // Get Request Path
        LwM2mPath path = request.getPath();

        // Store last sending time if needed
        Long lastSendingTime = null;
        if (strategy.hasPmin(attributes, path)) {
            lastSendingTime = System.nanoTime();
        }

        // Store last value sent if needed;
        LwM2mNode lastValue = null;
        if (strategy.hasCriteriaBasedOnValue(path, attributes)) {
            lastValue = newValue;
        }

        // Schedule notification for Max Period if needed
        ScheduledFuture<Void> pmaxTask = null;
        if (strategy.hasPmax(path, attributes)) {
            pmaxTask = executor.schedule(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    sendNotification(server, request, attributes, sender);
                    return null;
                }
            }, strategy.getPmax(path, attributes), TimeUnit.SECONDS);
        }

        // Create State for this observe relation
        store.addNotificationData(server, request,
                new NotificationData(attributes, lastSendingTime, lastValue, pmaxTask));
    }

    protected void sendNotification(LwM2mServer server, ObserveRequest request, NotificationAttributeTree attributes,
            NotificationSender sender) {
        ObserveResponse observeResponse = createResponse(server, request);
        if (!sender.sendNotification(observeResponse)) {
            // remove data as relation doesn't exist anymore
            clear(server, request);
        } else {
            if (observeResponse.isFailure()) {
                // remove data as relation must removed on failure
                clear(server, request);
            } else {
                updateNotificationData(server, request, attributes, observeResponse.getContent(), sender);
            }
        }
    }

    protected ObserveResponse createResponse(LwM2mServer server, ObserveRequest request) {
        // TODO maybe we can remove "receiver" dependencie and directly use ObjectTree ?
        return receiver.requestReceived(server, request).getResponse();
    }

    public void destroy() {
        executor.shutdownNow();
    }
}
