/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
package org.eclipse.leshan.client.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.leshan.client.resource.BaseInstanceEnablerFactory;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.StaticModel;
import org.junit.jupiter.api.Test;

public class BaseInstanceEnablerFactoryTest {

    public static final LwM2mModel model = new StaticModel(ObjectLoader.loadDefault());
    public static List<Integer> emptyList = Collections.emptyList();

    @Test
    public void create_instance_with_unexpected_id() {
        final int id = 2;
        BaseInstanceEnablerFactory badInstanceEnablerFactory = new BaseInstanceEnablerFactory() {

            @Override
            public LwM2mInstanceEnabler create() {
                // do no respect the contract and set a bad id;
                return new DummyInstanceEnabler(id + 1);
            }
        };

        assertThrowsExactly(IllegalStateException.class, () -> {
            badInstanceEnablerFactory.create(model.getObjectModel(LwM2mId.ACCESS_CONTROL), id, emptyList);
        });
    }

    @Test
    public void create_instance() {
        BaseInstanceEnablerFactory instanceEnablerFactory = new BaseInstanceEnablerFactory() {

            @Override
            public LwM2mInstanceEnabler create() {
                return new DummyInstanceEnabler();
            }
        };

        List<Integer> alreadyUsedIds = Arrays.asList(2, 3, 5, 6);
        LwM2mInstanceEnabler instance = instanceEnablerFactory.create(model.getObjectModel(LwM2mId.ACCESS_CONTROL),
                null, alreadyUsedIds);
        assertNotNull(instance.getId(), "instance id is not set");
        assertFalse(alreadyUsedIds.contains(instance.getId()), "new id must not be already used");
    }
}
