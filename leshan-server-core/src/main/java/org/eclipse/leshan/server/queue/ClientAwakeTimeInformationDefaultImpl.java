/*******************************************************************************
 * Copyright (c) 2018 Sierra Wireless and others.
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
package org.eclipse.leshan.server.queue;

import org.eclipse.leshan.server.registration.Registration;

public class ClientAwakeTimeInformationDefaultImpl implements ClientAwakeTimeInformation {

    int clientAwakeTime;

    public ClientAwakeTimeInformationDefaultImpl() {
        this.clientAwakeTime = 93000;
    }

    public ClientAwakeTimeInformationDefaultImpl(int defaultClientAwakeTime) {
        this.clientAwakeTime = defaultClientAwakeTime;
    }

    @Override
    public int usedClientAwakeTime(Registration reg) {

        return clientAwakeTime;
    }

}
