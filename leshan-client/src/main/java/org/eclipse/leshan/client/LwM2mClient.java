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
package org.eclipse.leshan.client;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.client.request.LwM2mClientRequest;
import org.eclipse.leshan.client.response.OperationResponse;
import org.eclipse.leshan.client.util.ResponseCallback;

public interface LwM2mClient {

    public void start();

    public void stop();

    public OperationResponse send(LwM2mClientRequest request);

    public void send(LwM2mClientRequest request, ResponseCallback callback);

    public LinkObject[] getObjectModel(Integer... ids);

}