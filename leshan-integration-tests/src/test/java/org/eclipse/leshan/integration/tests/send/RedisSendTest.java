/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests.send;

import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.integration.tests.util.RedisIntegrationTestHelper;

public class RedisSendTest extends SendTest {
    public RedisSendTest() {
        helper = new RedisIntegrationTestHelper() {
            @Override
            protected ObjectsInitializer createObjectsInitializer() {
                return new ObjectsInitializer(new StaticModel(createObjectModels()));
            };
        };
    }
}
