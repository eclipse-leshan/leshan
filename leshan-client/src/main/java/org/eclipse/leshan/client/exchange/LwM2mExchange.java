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
package org.eclipse.leshan.client.exchange;

import org.eclipse.leshan.ObserveSpec;
import org.eclipse.leshan.client.response.LwM2mResponse;

public interface LwM2mExchange {

    public void respond(LwM2mResponse response);

    public byte[] getRequestPayload();

    public boolean hasObjectInstanceId();

    public int getObjectInstanceId();

    public boolean isObserve();

    public ObserveSpec getObserveSpec();

}
