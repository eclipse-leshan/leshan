/*******************************************************************************
 * Copyright (c) 2017 RISE SICS AB.
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
 *     Carlos Gonzalo Peces - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.leshan.server.registration.Registration;

public class PresenceServiceImpl implements PresenceService {

    private final List<PresenceListener> listeners;

    public PresenceServiceImpl() {
        listeners = new ArrayList<>();
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
    public void notifySleeping(Registration registration) {
        for (PresenceListener listener : listeners) {
            listener.onSleeping(registration);
        }
    }

    @Override
    public void notifyAwake(Registration registration) {
        for (PresenceListener listener : listeners) {
            listener.onAwake(registration);
        }
    }

}
