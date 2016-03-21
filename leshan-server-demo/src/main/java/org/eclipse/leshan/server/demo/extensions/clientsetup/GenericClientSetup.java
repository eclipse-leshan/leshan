/*******************************************************************************
 * Copyright (c) 2016 Bosch Software Innovations GmbH and others.
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
 *    Achim Kraus (Bosch Software Innovations GmbH) - Initial contribution
 ******************************************************************************/
package org.eclipse.leshan.server.demo.extensions.clientsetup;

import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic client setup. Supports configured setup of device time and observes.
 */
public class GenericClientSetup extends BaseClientSetup {
    private static final Logger LOG = LoggerFactory.getLogger(GenericClientSetup.class);

    private final static int STATE_START_OBSERVES = STATE_START + 1;
    private int finish = STATE_START_OBSERVES;

    public GenericClientSetup() {
        super();
    }

    @Override
    public int applyConfiguration(Client client) {
        enableObserves(STATE_START_OBSERVES, null);
        finish = super.applyConfiguration(client);
        return finish;
    }

    @Override
    public boolean process(LeshanServer server, Client client, int stateIndex) throws InterruptedException {
        boolean done = false;
        if (finish >= stateIndex) {
            done = true;
        } else {
            LOG.error("error initialization for " + getEndpoint() + " step " + stateIndex);
        }
        return done;
    }

}
