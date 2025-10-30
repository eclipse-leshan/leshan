/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests.security;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.integration.tests.util.LeshanTestServerBuilder;

public class RedisRpkX509Test extends RpkX509Test {

    @Override
    protected LeshanTestServerBuilder givenServerUsing(Protocol givenProtocol) {
        return super.givenServerUsing(givenProtocol).withRedisRegistrationStore().withRedisSecurityStore();
    }
}
