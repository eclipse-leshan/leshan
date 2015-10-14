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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.impl;

import java.io.IOException;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.ObservationListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ObservationRegistryImplTest extends BasicTestSupport {

    ObservationRegistryImpl registry;

    @Before
    public void setUp() throws Exception {
        registry = new ObservationRegistryImpl();
    }

    @Test
    public void add_duplicate_observation() throws IOException {
        givenASimpleClient();

        Observation obs = new ObservationImpl(client.getRegistrationId(), new LwM2mPath(3, 0, 15));
        registry.addObservation(obs);

        Observation duplicate = new ObservationImpl(client.getRegistrationId(), new LwM2mPath(3, 0, 15));

        registry.addObservation(duplicate);
        Assert.assertSame(1, registry.cancelObservations(client));
    }

    private class ObservationImpl implements Observation {

        private String registrationId;
        private LwM2mPath path;

        public ObservationImpl(String registrationId, LwM2mPath path) {
            this.registrationId = registrationId;
            this.path = path;
        }

        @Override
        public String getRegistrationId() {
            return registrationId;
        }

        @Override
        public LwM2mPath getPath() {
            return path;
        }

        @Override
        public void cancel() {
        }

        @Override
        public void addListener(ObservationListener listener) {
        }

        @Override
        public void removeListener(ObservationListener listener) {
        }
    }
}
