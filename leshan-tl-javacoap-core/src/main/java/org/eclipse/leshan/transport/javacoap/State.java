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
package org.eclipse.leshan.transport.javacoap;

public enum State {
    INITIAL, STARTED, STOPPED, DESTROYED;

    public boolean isStarted() {
        return this.equals(STARTED);
    }

    public boolean isStopped() {
        return this.equals(STOPPED);
    }

    public boolean isDestroyed() {
        return this.equals(DESTROYED);
    }

}
