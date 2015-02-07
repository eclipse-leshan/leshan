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
package org.eclipse.leshan.client.resource;

import org.eclipse.leshan.client.exchange.LwM2mExchange;

public abstract class LwM2mClientResource extends LwM2mClientNode {

    @Override
    public abstract void read(LwM2mExchange exchange);

    public abstract void write(LwM2mExchange exchange);

    public abstract void execute(LwM2mExchange exchange);

    public abstract boolean isReadable();

    public abstract void notifyResourceUpdated();
}
