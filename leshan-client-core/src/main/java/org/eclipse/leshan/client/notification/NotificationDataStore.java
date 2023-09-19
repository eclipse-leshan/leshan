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

import java.util.Iterator;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.link.lwm2m.attributes.NotificationAttributeTree;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.util.Validate;

/**
 * This class store information needed to handle write attributes behavior.
 * <p>
 * It is used by {@link NotificationManager}
 */
public class NotificationDataStore {

    private final NavigableMap<NotificationDataKey, NotificationData> store = new TreeMap<>();

    public NotificationData getNotificationData(LwM2mServer server, ObserveRequest request) {
        return store.get(toKey(server, request));
    }

    public synchronized NotificationData addNotificationData(LwM2mServer server, ObserveRequest request,
            NotificationData data) {
        NotificationData previousData = store.put(toKey(server, request), data);
        if (previousData != null) {
            // cancel task of previous data
            cancelTasks(previousData);
        }
        return previousData;
    }

    public synchronized NotificationData updateNotificationData(LwM2mServer server, ObserveRequest request,
            NotificationData data) {

        NotificationData previousData = store.replace(toKey(server, request), data);
        if (previousData != null) {
            // If updated, cancel task of previous data
            cancelTasks(previousData);
        } else {
            // If NOT updated, cancel task of given data
            cancelTasks(data);
        }
        return previousData;
    }

    public synchronized void removeNotificationData(LwM2mServer server, ObserveRequest request) {
        NotificationData removed = store.remove(toKey(server, request));
        if (removed != null) {
            cancelTasks(removed);
        }
    }

    public synchronized void clearAllNotificationDataUnder(LwM2mPath parentPath) {
        Iterator<NotificationDataKey> it = store.keySet().iterator();
        while (it.hasNext()) {
            NotificationDataKey key = it.next();
            if (key.getPath().startWith(parentPath)) {
                it.remove();
            }
        }
    }

    public synchronized void clearAllNotificationDataFor(LwM2mServer server) {
        SortedMap<NotificationDataKey, NotificationData> toRemove = store.subMap(floorKeyFor(server),
                ceilKeyFor(server));
        for (NotificationData toRemoveData : toRemove.values()) {
            cancelTasks(toRemoveData);
        }
        toRemove.clear();
    }

    public synchronized void clearAllNotificationData() {
        for (NotificationData toRemoveData : store.values()) {
            cancelTasks(toRemoveData);
        }
        store.clear();
    }

    public synchronized boolean isEmpty() {
        return store.isEmpty();
    }

    private void cancelTasks(NotificationData data) {
        if (data.getPminFuture() != null) {
            data.getPminFuture().cancel(false);
        }
        if (data.getPmaxFuture() != null) {
            data.getPmaxFuture().cancel(false);
        }
    }

    private NotificationDataKey floorKeyFor(LwM2mServer server) {
        // TODO should be replaced by a ObservationRelationIdentifier probably based on Token
        return new NotificationDataKey(server.getId(), LwM2mPath.ROOTPATH);
    }

    private NotificationDataKey ceilKeyFor(LwM2mServer server) {
        // TODO should be replaced by a ObservationRelationIdentifier probably based on Token
        return new NotificationDataKey(server.getId() + 1, LwM2mPath.ROOTPATH);
    }

    private NotificationDataKey toKey(LwM2mServer server, ObserveRequest request) {
        // TODO should be replaced by a ObservationRelationIdentifier probably based on Token
        return new NotificationDataKey(server.getId(), request.getPath());
    }

    private static class NotificationDataKey implements Comparable<NotificationDataKey> {

        private final Long serverId;
        // TODO should be replaced by a ObservationRelationIdentifier probably based on Token
        private final LwM2mPath path;

        public NotificationDataKey(Long serverId, LwM2mPath path) {
            Validate.notNull(serverId);
            Validate.notNull(path);
            this.serverId = serverId;
            this.path = path;
        }

        @Override
        public int compareTo(NotificationDataKey o) {
            // check for null
            if (o == null) {
                // object can not be null following Comparable javadoc
                throw new NullPointerException();
            }

            // compare server Id
            int r = getServerId().compareTo(o.getServerId());
            if (r != 0)
                return r;

            // if server id equals, then compare path
            return path.compareTo(o.getPath());
        }

        public Long getServerId() {
            return serverId;
        }

        public LwM2mPath getPath() {
            return path;
        }
    }

    public static class NotificationData {
        private final NotificationAttributeTree attributes;
        private final Long lastSendingTime; // time of last sent notification
        private final LwM2mNode lastSentValue; // last value sent
        private final ScheduledFuture<Void> pminTask; // task which will send delayed notification for pmin.
        private final ScheduledFuture<Void> pmaxTask; // task which will send delayed notification for pmax.

        public NotificationData(NotificationAttributeTree attributes, Long lastSendingTime, LwM2mNode lastSentValue,
                ScheduledFuture<Void> nextNotification) {
            this.attributes = attributes;
            this.lastSendingTime = lastSendingTime;
            this.lastSentValue = lastSentValue;
            this.pminTask = null;
            this.pmaxTask = nextNotification;
        }

        public NotificationData(NotificationData previous, ScheduledFuture<Void> pminTask) {
            this.attributes = previous.getAttributes();
            this.lastSendingTime = previous.getLastSendingTime();
            this.lastSentValue = previous.getLastSentValue();
            this.pminTask = pminTask;
            this.pmaxTask = previous.getPmaxFuture();
        }

        public NotificationAttributeTree getAttributes() {
            return attributes;
        }

        public Long getLastSendingTime() {
            return lastSendingTime;
        }

        public LwM2mNode getLastSentValue() {
            return lastSentValue;
        }

        public ScheduledFuture<Void> getPminFuture() {
            return pminTask;
        }

        public ScheduledFuture<Void> getPmaxFuture() {
            return pmaxTask;
        }

        public boolean usePmin() {
            return lastSendingTime != null;
        }

        public boolean usePmax() {
            return pmaxTask != null;
        }

        public boolean hasCriteriaBasedOnValue() {
            return lastSentValue != null;
        }

        public boolean pminTaskScheduled() {
            return pminTask != null;
        }
    }
}
