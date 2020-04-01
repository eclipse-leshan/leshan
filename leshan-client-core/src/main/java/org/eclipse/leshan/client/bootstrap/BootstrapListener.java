/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
package org.eclipse.leshan.client.bootstrap;

/**
 * Listen for bootstrap session event.
 */
public interface BootstrapListener {

    /**
     * Invoked when a bootstrap session is closed.<br>
     * 
     * Generally when we receive a bootstrap finished request or when the bootstrap session ends in an unexpected
     * way.(e.g. bootstrap server is not responding anymore)
     */
    void bootstrapFinished();
}
