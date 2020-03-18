/*******************************************************************************
 * Copyright (c) 2018 RISE SICS AB.
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
 *     RISE SICS AB - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

import org.eclipse.leshan.server.registration.Registration;

public interface ClientAwakeTimeProvider {

    /**
     * Returns the client awake time for the corresponding client, identified by the {@link Registration} object.
     * 
     * @param reg the client's registration object
     * @return the client awake time in milliseconds
     */
    int getClientAwakeTime(Registration reg);

}
