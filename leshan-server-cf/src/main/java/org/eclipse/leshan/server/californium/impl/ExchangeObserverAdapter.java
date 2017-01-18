/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
package org.eclipse.leshan.server.californium.impl;

import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.ExchangeObserver;

/**
 * An abstract adapter class for reacting to exchange events. The methods in this class are empty. This class exists as
 * convenience for creating exchange observer objects.
 */
// TODO should be part of californium ?
public class ExchangeObserverAdapter implements ExchangeObserver {

    @Override
    public void completed(Exchange exchange) {
    }

    @Override
    public void contextEstablished(Exchange exchange) {
    }
}
