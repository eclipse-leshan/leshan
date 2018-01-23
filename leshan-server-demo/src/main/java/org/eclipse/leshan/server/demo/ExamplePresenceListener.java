/*******************************************************************************
 * Copyright (c) 2017 RISE SICS AB.
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
 *     RISE SICS AB - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.demo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.leshan.server.queue.PresenceListener;
import org.eclipse.leshan.server.registration.Registration;

/**
 * Just an example of a Presence listener that simply prints the state of the client and the time.
 *
 */

public class ExamplePresenceListener implements PresenceListener {
    
    private String endpoint;

    public ExamplePresenceListener(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public void onAwake(Registration registration) {
        if(registration.getEndpoint().equals(this.endpoint)) {
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            System.out.println("Queue Mode Listener: client: " + registration.getEndpoint() + " is awake at "
                    + dateFormat.format(new Date()));
        }

    }

    @Override
    public void onSleeping(Registration registration) {
        if(registration.getEndpoint().equals(this.endpoint)) {
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            System.out.println("Queue Mode Listener: client: " + registration.getEndpoint() + " is sleeping at "
                    + dateFormat.format(new Date()));
        }

    }

}
