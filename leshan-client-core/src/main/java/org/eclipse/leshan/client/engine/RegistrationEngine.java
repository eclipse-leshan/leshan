/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.client.engine;

import org.eclipse.leshan.client.RegistrationUpdate;

public interface RegistrationEngine {

    void triggerRegistrationUpdate();

    void triggerRegistrationUpdate(RegistrationUpdate registrationUpdate);

    String getRegistrationId();

    String getEndpoint();

    void start();

    void stop(boolean deregister);

    void destroy(boolean deregister);
}
