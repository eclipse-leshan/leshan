/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
package org.eclipse.leshan.client.servers;

import org.eclipse.leshan.core.request.BindingMode;

public class DmServerInfo extends ServerInfo {

    public long lifetime;
    public BindingMode binding;

    // TODO add missing information like SMS number

    @Override
    public String toString() {
        return String.format("DM Server [uri=%s, lifetime=%s, binding=%s]", serverUri, lifetime, binding);
    }
}
