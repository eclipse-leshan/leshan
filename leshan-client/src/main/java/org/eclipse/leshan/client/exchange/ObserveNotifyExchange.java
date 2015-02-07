/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.exchange;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.ObserveSpec;
import org.eclipse.leshan.client.resource.LwM2mClientNode;
import org.eclipse.leshan.client.response.LwM2mResponse;
import org.eclipse.leshan.client.response.ObserveResponse;

public class ObserveNotifyExchange extends ForwardingLwM2mExchange implements Runnable {

    private static final long SECONDS_TO_MILLIS = 1000;

    private ObserveSpec observeSpec;

    private ScheduledExecutorService service;
    private LwM2mClientNode node;
    private byte[] previousValue;
    private Date previousTime;

    public ObserveNotifyExchange(final LwM2mExchange exchange, LwM2mClientNode node, ObserveSpec observeSpec,
            ScheduledExecutorService service) {
        super(exchange);
        this.node = node;
        this.observeSpec = observeSpec;
        this.service = service;
        updatePrevious(null);
        scheduleNext();
    }

    @Override
    public void respond(final LwM2mResponse response) {
        if (shouldNotify(response)) {
            sendNotify(response);
        }
        scheduleNext();
    }

    private void updatePrevious(byte[] responsePayload) {
        previousValue = responsePayload;
        previousTime = new Date();
    }

    private boolean shouldNotify(final LwM2mResponse response) {
        final long diff = getTimeDiff();
        final Integer pmax = observeSpec.getMaxPeriod();
        if (pmax != null && diff > pmax * SECONDS_TO_MILLIS) {
            return true;
        }
        return !Arrays.equals(response.getResponsePayload(), previousValue);
    }

    private void sendNotify(final LwM2mResponse response) {
        updatePrevious(response.getResponsePayload());
        exchange.respond(ObserveResponse.notifyWithContent(response.getResponsePayload()));
    }

    public void setObserveSpec(final ObserveSpec observeSpec) {
        this.observeSpec = observeSpec;
    }

    private void scheduleNext() {
        if (observeSpec.getMaxPeriod() != null) {
            long diff = getTimeDiff();
            service.schedule(this, observeSpec.getMaxPeriod() * SECONDS_TO_MILLIS - diff, TimeUnit.MILLISECONDS);
        }
    }

    private long getTimeDiff() {
        return new Date().getTime() - previousTime.getTime();
    }

    @Override
    public void run() {
        node.read(this);
    }

}
