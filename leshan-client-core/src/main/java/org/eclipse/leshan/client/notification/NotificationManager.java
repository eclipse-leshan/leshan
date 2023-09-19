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
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
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
        LwM2mAttributeSet attributes = getAttributes(server, request);

        // If there is no attributes this is just classic observe so nothing to do.
        if (attributes == null)
            return;

        LOG.debug("Handle observe relation for {} / {}", server, request);

        // Store needed data for this observe relation.
        updateNotificationData(server, request, attributes, node, sender);
    }

    protected LwM2mAttributeSet getAttributes(LwM2mServer server, ObserveRequest request) {
        // TODO use objectTree to get attribute;
        // for testing, we return hardcoded value for resource 6/0/1
        if (request.getPath().equals(new LwM2mPath(6, 0, 1))) {
            return new LwM2mAttributeSet( //
                    LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 5l),
                    LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 10l));
        }
        return null;
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
        LwM2mAttributeSet attributes = notificationData.getAttributes();

        // if previous value
        // Get new value
        // check LT / GT / ST criteria
        // Then send notification if criteria doesn't match, stop

        // TODO use case where PMIN == PMAX
        // If PMIN is used
        if (notificationData.usePmin()) {
            LOG.trace("handle pmin for observe relation of {} / {}", server, request);

            if (notificationData.pminTaskScheduled()) {
                // nothing to do if a task is already scheduled
                return;
            }

            // calculate time since last notification
            Long timeSinceLastNotification = TimeUnit.SECONDS
                    .convert(System.nanoTime() - notificationData.getLastSendingTime(), TimeUnit.NANOSECONDS);
            Long pmin = attributes.get(LwM2mAttributes.MINIMUM_PERIOD).getValue();
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
    }

    // TODO an optimization could be to synchronize by observe relation (identify by server / request)
    public synchronized void clear(LwM2mServer server) {
        // remove all data about observe relation for given server.
    }

    // TODO an optimization could be to synchronize by observe relation (identify by server / request)
    public synchronized void clear() {
        // remove all data about observe relation.
    }

    // TODO an optimization could be to synchronize by observe relation (identify by server / request)
    protected synchronized void updateNotificationData(LwM2mServer server, ObserveRequest request,
            LwM2mAttributeSet attributes, LwM2mNode newValue, NotificationSender sender) {

        // Store last sending time if needed
        Long lastSendingTime = null;
        if (attributes.contains(LwM2mAttributes.MINIMUM_PERIOD)) {
            lastSendingTime = System.nanoTime();
        }

        // Store last value sent if needed;
        LwM2mNode lastValue = null;
        if (attributes.contains(LwM2mAttributes.GREATER_THAN) || attributes.contains(LwM2mAttributes.LESSER_THAN)
                || attributes.contains(LwM2mAttributes.STEP)) {
            lastValue = newValue;
        }

        // Schedule notification for Max Period if needed
        ScheduledFuture<Void> pmaxTask = null;
        if (attributes.contains(LwM2mAttributes.MAXIMUM_PERIOD)) {
            pmaxTask = executor.schedule(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    sendNotification(server, request, attributes, sender);
                    return null;
                }
            }, attributes.getLwM2mAttribute(LwM2mAttributes.MAXIMUM_PERIOD).getValue(), TimeUnit.SECONDS);
        }

        // Create State for this observe relation
        store.addNotificationData(server, request,
                new NotificationData(attributes, lastSendingTime, lastValue, pmaxTask));
    }

    protected void sendNotification(LwM2mServer server, ObserveRequest request, LwM2mAttributeSet attributes,
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
