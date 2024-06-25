/*******************************************************************************
 * Copyright (c) 2022    Sierra Wireless and others.
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
package org.eclipse.leshan.client.send;

/**
 * A class responsible for collecting and sending collected data.
 * <p>
 * {@link DataSender} are stored in a in {@link DataSenderManager} and can be retrieve by name using
 * {@link DataSenderManager#getDataSender(String)}.
 */
public interface DataSender {
    /**
     * Set the {@link DataSenderManager} which holds this {@link DataSender}.
     */
    void setDataSenderManager(DataSenderManager dataSenderManager);

    String getName();
}
