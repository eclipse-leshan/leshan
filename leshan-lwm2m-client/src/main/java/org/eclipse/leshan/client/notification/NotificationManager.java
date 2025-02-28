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
import org.eclipse.leshan.core.link.lwm2m.attributes.InvalidAttributesException;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.link.lwm2m.attributes.NotificationAttributeTree;
import org.eclipse.leshan.core.node.LwM2mChildNode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible to modify default observe behavior based on write attributes.
 * <p>
 * It does not support Observe-Composite.
 */
public class NotificationManager {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationManager.class);

    private final DownlinkRequestReceiver receiver;
    private final NotificationDataStore store;
    private final LwM2mObjectTree objectTree;
    private final NotificationStrategy strategy;
    private final ScheduledExecutorService executor;
    private final boolean executorAttached;

    public NotificationManager(LwM2mObjectTree objectTree, DownlinkRequestReceiver requestReceiver) {
        this(objectTree, requestReceiver, new NotificationDataStore(), new DefaultNotificationStrategy(), null);
    }

    public NotificationManager(LwM2mObjectTree objectTree, DownlinkRequestReceiver requestReceiver,
            NotificationDataStore store, NotificationStrategy strategy, ScheduledExecutorService executor) {
        Validate.notNull(objectTree);
        Validate.notNull(requestReceiver);
        Validate.notNull(store);
        Validate.notNull(strategy);

        this.objectTree = objectTree;
        this.receiver = requestReceiver;
        this.store = store;
        this.strategy = strategy;

        if (executor == null) {
            this.executor = Executors.newSingleThreadScheduledExecutor();
            this.executorAttached = true;
        } else {
            this.executor = executor;
            this.executorAttached = false;
        }
        this.objectTree.addListener(new ObjectsListenerAdapter() {
            @Override
            public void objectRemoved(LwM2mObjectEnabler object) {
                // I guess ideally this SHOULD NOT be needed because
                // If an observed resource under this object is removed then a 4.04 notification should be sent
                // immediately.
                // but underlying library doesn't really implement this, e.g :
                // https://github.com/eclipse-californium/californium/issues/2223
                store.clearAllNotificationDataUnder(new LwM2mPath(object.getId()));
            }
        });
    }

    public synchronized void initRelation(LwM2mServer server, ObserveRequest request, LwM2mNode node,
            NotificationSender sender) throws InvalidAttributesException {
        // Get Attributes for this (server, request)
        LwM2mObjectEnabler objectEnabler = objectTree.getObjectEnabler(request.getPath().getObjectId());
        if (objectEnabler == null)
            return; // no object enabler : nothing to observe
        NotificationAttributeTree attributes = objectEnabler.getAttributesFor(server);
        if (attributes != null && !attributes.isEmpty()) {
            attributes = strategy.selectNotificationsAttributes(request.getPath(), attributes);
        }

        // If there is no attributes this is just classic observe so nothing to do.
        if (attributes == null || attributes.isEmpty())
            return;

        LOG.debug("Handle observe relation for {} / {}", server, request);

        // Store needed data for this observe relation.
        updateNotificationData(true, server, request, attributes, node, sender);
    }

    public synchronized void notificationTriggered(LwM2mServer server, ObserveRequest request,
            NotificationSender sender) {
        LOG.trace("Notification triggered for observe relation of {} / {}", server, request);

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
        ObserveResponse candidateNotificationToSend = null;

        // Handle if pmin = pmax, we don't need to check anything only send notification each pmin=pmax seconds
        // AFAWK, this case is not clearly defined in LWM2M v1.1.1 or in its references but we can find hints in :
        // https://datatracker.ietf.org/doc/html/draft-ietf-core-conditional-attributes-06#section-4
        // referenced by LWM2M v1.2.1
        if (notificationData.usePmax()) {
            Long pmin = strategy.getAttributeValue(attributes, request.getPath(), LwM2mAttributes.MINIMUM_PERIOD);
            Long pmax = strategy.getAttributeValue(attributes, request.getPath(), LwM2mAttributes.MAXIMUM_PERIOD);
            if (pmax.equals(pmin)) {
                // we only send notification when pmax timer is reached.
                return;
            }
        }

        // if there is criteria based on value
        if (notificationData.hasCriteriaBasedOnValue()) {
            candidateNotificationToSend = createResponse(server, request);
            if (candidateNotificationToSend.isSuccess()) {
                LwM2mChildNode newValue = candidateNotificationToSend.getContent();

                // if criteria doesn't match do not raise any event.
                if (!strategy.shouldTriggerNotificationBasedOnValueChange(attributes, request.getPath(),
                        notificationData.getLastSentValue(), newValue)) {
                    return;
                }
            }
            // else if there is an error send notification now.
        }

        // If PMIN is used check if we need to delay this notification.
        if (notificationData.usePmin()) {
            LOG.trace("handle pmin for observe relation of {} / {}", server, request);

            if (notificationData.pminTaskScheduled()) {
                // nothing to do if a task is already scheduled
                return;
            }

            // calculate time since last notification
            Long timeSinceLastNotification = TimeUnit.SECONDS
                    .convert(System.nanoTime() - notificationData.getLastSendingTime(), TimeUnit.NANOSECONDS);
            Long pmin = strategy.getAttributeValue(attributes, request.getPath(), LwM2mAttributes.MINIMUM_PERIOD);
            if (timeSinceLastNotification < pmin) {
                ScheduledFuture<Void> pminTask = executor.schedule(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        sendNotification(server, request, null, attributes, sender);
                        return null;
                    }
                }, pmin - timeSinceLastNotification, TimeUnit.SECONDS);
                // schedule next task for pmin but do not send notification
                store.updateNotificationData(server, request, new NotificationData(notificationData, pminTask));
                return;
            }
        }

        sendNotification(server, request, candidateNotificationToSend, attributes, sender);
    }

    public synchronized void clear(LwM2mServer server, ObserveRequest request) {
        // remove all data about observe relation for given server / request.
        store.removeNotificationData(server, request);
    }

    public synchronized void clear(LwM2mServer server) {
        // remove all data about observe relation for given server.
        store.clearAllNotificationDataFor(server);
    }

    public synchronized void clear() {
        // remove all data about observe relation.
        store.clearAllNotificationData();
    }

    protected synchronized void updateNotificationData(boolean newRelation, LwM2mServer server, ObserveRequest request,
            NotificationAttributeTree attributes, LwM2mNode newValue, NotificationSender sender) {
        // Get Request Path
        LwM2mPath path = request.getPath();

        // Store last sending time if needed
        Long lastSendingTime = null;
        if (strategy.hasAttribute(attributes, path, LwM2mAttributes.MINIMUM_PERIOD)) {
            lastSendingTime = System.nanoTime();
        }

        // Store last value sent if needed
        LwM2mNode lastValue = null;
        if (strategy.hasCriteriaBasedOnValue(attributes, path)) {
            lastValue = newValue;
        }

        // Schedule notification for Max Period if needed
        ScheduledFuture<Void> pmaxTask = null;
        if (strategy.hasAttribute(attributes, path, LwM2mAttributes.MAXIMUM_PERIOD)) {
            pmaxTask = executor.schedule(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    sendNotification(server, request, null, attributes, sender);
                    return null;
                }
            }, strategy.getAttributeValue(attributes, path, LwM2mAttributes.MAXIMUM_PERIOD), TimeUnit.SECONDS);
        }

        // Create State for this observe relation
        if (newRelation) {
            store.addNotificationData(server, request,
                    new NotificationData(attributes, lastSendingTime, lastValue, pmaxTask));
        } else {
            store.updateNotificationData(server, request,
                    new NotificationData(attributes, lastSendingTime, lastValue, pmaxTask));
        }
    }

    protected void sendNotification(LwM2mServer server, ObserveRequest request, ObserveResponse observeResponse,
            NotificationAttributeTree attributes, NotificationSender sender) {
        if (observeResponse == null) {
            observeResponse = createResponse(server, request);
        }
        if (!sender.sendNotification(observeResponse)) {
            // remove data as relation doesn't exist anymore
            clear(server, request);
        } else {
            if (observeResponse.isFailure()) {
                // remove data as relation must be removed on failure
                clear(server, request);
            } else {
                updateNotificationData(false, server, request, attributes, observeResponse.getContent(), sender);
            }
        }
    }

    protected ObserveResponse createResponse(LwM2mServer server, ObserveRequest request) {
        // TODO write attributes : maybe we can remove "receiver" dependencie and directly use ObjectTree ?
        return receiver.requestReceived(server, request).getResponse();
    }

    public void destroy() {
        if (executorAttached) {
            executor.shutdownNow();
        }
    }
}
