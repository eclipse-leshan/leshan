/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
package org.eclipse.leshan.client.servers;

import org.eclipse.leshan.core.request.BindingMode;

/**
 * Sensible information about a LWM2M server
 * <p>
 * It extends {@link ServerInfo} to add information specific to LWM2M (Device Management) server. It contains mainly
 * information available in LWM2M Security Object and LWM2M Server Object.
 */
public class DmServerInfo extends ServerInfo {

    public long lifetime;
    public BindingMode binding;
    // TODO add missing information like SMS number

    @Override
    public String toString() {
        return String.format("DM Server [uri=%s, lifetime=%s, binding=%s]", serverUri, lifetime, binding);
    }
}
