/*******************************************************************************
 * Copyright (c) 2017 Bosch Software Innovations GmbH and others.
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
 *     Bosch Software Innovations GmbH - initial API
 *     RISE SICS AB - added more features 
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.server.registration.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks the status of each LWM2M client registered with Queue mode binding. Also ensures that the
 * {@link PresenceListener} are notified on state changes only for those LWM2M clients registered using Queue mode
 * binding.
 */
public final class PresenceServiceImpl implements PresenceService, Destroyable {
    private final Logger LOG = LoggerFactory.getLogger(PresenceServiceImpl.class);

    private final ConcurrentMap<String /* endpoint */, AtomicReference<ScheduledFuture<?>>> clientPresences = new ConcurrentHashMap<>();
    private final List<PresenceListener> listeners = new CopyOnWriteArrayList<>();
    private final ClientAwakeTimeProvider awakeTimeProvider;
    private final ScheduledExecutorService clientTimersExecutor = Executors
            .newSingleThreadScheduledExecutor(new NamedThreadFactory("Presence Service"));

    public PresenceServiceImpl(ClientAwakeTimeProvider awakeTimeProvider) {
        this.awakeTimeProvider = awakeTimeProvider;
    }

    @Override
    public void addListener(PresenceListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(PresenceListener listener) {
        listeners.remove(listener);
    }

    @Override
    public boolean isClientAwake(Registration registration) {
        return clientPresences.containsKey(registration.getEndpoint());
    }

    /**
     * Set the client identified by registration as awake. Listeners are notified if that client state changed to awake
     * state.
     * 
     * @param reg the client's registration object
     */
    public void setAwake(final Registration reg) {
        if (reg.usesQueueMode()) {
            boolean stateChanged;
            final AtomicReference<ScheduledFuture<?>> timerFuture = new AtomicReference<>();
            // set this device as awake
            AtomicReference<ScheduledFuture<?>> previous = clientPresences.put(reg.getEndpoint(), timerFuture);
            if (previous != null) {
                stateChanged = false;
                // cancel previous timer
                if (previous.get() != null) {
                    previous.get().cancel(false);
                }
            } else {
                stateChanged = true;
            }

            // Every time we set the clientAwakeTime, in case it changes dynamically
            int clientAwakeTime = awakeTimeProvider.getClientAwakeTime(reg);
            if (clientAwakeTime != 0) {
                timerFuture.set(clientTimersExecutor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        boolean removed = clientPresences.remove(reg.getEndpoint(), timerFuture);
                        if (removed) {
                            // success remove means we go in sleeping mode.
                            for (PresenceListener listener : listeners) {
                                listener.onSleeping(reg);
                            }
                        }
                    }
                }, clientAwakeTime, TimeUnit.MILLISECONDS));

                // There is some rare race conditions (several quick call to setAwake)
                // where the timerFuture could have been already removed but not cancelled.
                // So to be sure to not keep useless cleaning task we cancel it if this is not the current timerFuture
                // anymore.
                // (This make the code a bit more complex but the is a cost of the non-blocking implementation)
                if (clientPresences.get(reg.getEndpoint()) != timerFuture) {
                    timerFuture.get().cancel(false);
                }
            }

            // notify if state changed
            if (stateChanged) {
                for (PresenceListener listener : listeners) {
                    listener.onAwake(reg);
                }
            }
        }
    }

    /**
     * Set the client in a sleeping state. Nothing is done if it already in sleeping state. Listeners are notified if
     * that client state changed to sleeping state.
     * <p>
     * Going in sleeping state should happen when the timer expires or when the client doesn't respond to a request.
     * 
     * @param reg the client's registration object
     */
    public void setSleeping(Registration reg) {
        if (reg.usesQueueMode()) {
            AtomicReference<ScheduledFuture<?>> timerFuture = clientPresences.remove(reg.getEndpoint());
            if (timerFuture != null) {
                if (timerFuture.get() != null) {
                    // we can not be sure timerFuture is set but this is not a big deal as timer is only able to removed
                    // itself.
                    timerFuture.get().cancel(false);
                }
                for (PresenceListener listener : listeners) {
                    listener.onSleeping(reg);
                }
            }
        }
    }

    /**
     * Stop to track presence for the given registration. No event is raised.
     * 
     * @param reg the client's registration object.
     */
    public void stopPresenceTracking(Registration reg) {
        clientPresences.remove(reg.getEndpoint());
    }

    @Override
    public void destroy() {
        clientTimersExecutor.shutdownNow();
        try {
            clientTimersExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Destroying presence service was interrupted.", e);
        }
    }
}
