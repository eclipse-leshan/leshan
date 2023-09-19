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

import java.util.concurrent.ScheduledFuture;

import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.request.ObserveRequest;

/**
 * This class store information needed to handle write attributes behavior.
 * <p>
 * It is used by {@link NotificationManager}
 */
public class NotificationDataStore {

    // TODO create a real data structure
    // for testing purpose we only store 1 NotificationData
    private NotificationData data;

    public NotificationData getNotificationData(LwM2mServer server, ObserveRequest request) {
        return data;
    }

    public NotificationData addNotificationData(LwM2mServer server, ObserveRequest request, NotificationData data) {
        // cancel task of previous data
        NotificationData previousData = this.data;
        if (previousData != null) {
            if (previousData.getPminFuture() != null) {
                previousData.getPminFuture().cancel(false);
            }
            if (previousData.getPmaxFuture() != null) {
                previousData.getPmaxFuture().cancel(false);
            }
        }
        // update data
        this.data = data;

        return previousData;
    }

    public static class NotificationData {
        private final LwM2mAttributeSet attributes;
        private final Long lastSendingTime; // time of last sent notification
        private final LwM2mNode lastSentValue; // last value sent
        private final ScheduledFuture<Void> pminTask; // task which will send delayed notification for pmin.
        private final ScheduledFuture<Void> pmaxTask; // task which will send delayed notification for pmax.

        public NotificationData(LwM2mAttributeSet attributes, Long lastSendingTime, LwM2mNode lastSentValue,
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

        public LwM2mAttributeSet getAttributes() {
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

        public boolean pminTaskScheduled() {
            return pminTask != null;
        }
    }
}
