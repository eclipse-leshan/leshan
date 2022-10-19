/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
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
package org.eclipse.leshan.server.bootstrap;

import org.eclipse.leshan.server.bootstrap.request.BootstrapDownlinkRequestSender;

/**
 * Creates {@link BootstrapHandler}.
 *
 * @see DefaultBootstrapHandler
 */
public interface BootstrapHandlerFactory {

    /**
     * Creates {@link BootstrapHandler}.
     *
     * @param sender the class responsible to send LWM2M request during a bootstrapSession.
     * @param sessionManager the manager responsible to handle bootstrap session.
     * @param listener a listener of bootstrap session life-cycle.
     * @return the new {@link BootstrapHandler}.
     */
    BootstrapHandler create(BootstrapDownlinkRequestSender sender, BootstrapSessionManager sessionManager,
            BootstrapSessionListener listener);
}
