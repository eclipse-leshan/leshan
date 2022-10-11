/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.client.engine;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.leshan.client.EndpointsManager;
import org.eclipse.leshan.client.bootstrap.BootstrapHandler;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.request.UplinkRequestSender;
import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.core.request.ContentFormat;

/**
 * A factory for {@link RegistrationEngine}
 */
public interface RegistrationEngineFactory {

    RegistrationEngine createRegistratioEngine(String endpoint, LwM2mObjectTree objectTree,
            EndpointsManager endpointsManager, UplinkRequestSender requestSender, BootstrapHandler bootstrapState,
            LwM2mClientObserver observer, Map<String, String> additionalAttributes,
            Map<String, String> bsAdditionalAttributes, Set<ContentFormat> supportedContentFormat,
            ScheduledExecutorService sharedExecutor);
}
