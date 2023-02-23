/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests.util.assertion;

import java.util.concurrent.TimeUnit;

import org.assertj.core.api.AbstractAssert;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.integration.tests.util.LeshanTestServer;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;

public class LeshanTestClientAssert extends AbstractAssert<LeshanTestClientAssert, LeshanTestClient> {

    public LeshanTestClientAssert(LeshanTestClient actual) {
        super(actual, LeshanTestClientAssert.class);
    }

    public static LeshanTestClientAssert assertThat(LeshanTestClient actual) {
        return new LeshanTestClientAssert(actual);
    }

    private void isNotNull(LeshanServer server) {
        if (server == null)
            failWithMessage("server MUST NOT be null");
    }

    private Registration getRegistration(LeshanServer server) {
        return server.getRegistrationService().getByEndpoint(actual.getEndpointName());
    }

    public LeshanTestClientAssert isRegisteredAt(LeshanServer server) {
        isNotNull();
        isNotNull(server);

        Registration r = getRegistration(server);
        if (r == null) {
            failWithMessage("Expected Registration for <%s> client", actual.getEndpointName());
        }
        return this;
    }

    public LeshanTestClientAssert isNotRegisteredAt(LeshanServer server) {
        isNotNull();
        isNotNull(server);

        Registration r = getRegistration(server);
        if (r != null) {
            failWithMessage("Expected No Registration for <%s> client but have <%s>", actual.getEndpointName(), r);
        }
        return this;
    }

    public LeshanTestClientAssert after(long delay, TimeUnit unit) {
        try {
            Thread.sleep(unit.toMillis(delay));
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        return this;
    }

    public LeshanTestClientAssert isAwakeOn(LeshanTestServer server) {
        isNotNull();
        isNotNull(server);

        Registration r = getRegistration(server);
        if (!server.getPresenceService().isClientAwake(r)) {
            failWithMessage("Expected <%s> client was awake", actual.getEndpointName());
        }
        return this;
    }

    public LeshanTestClientAssert isSleepingOn(LeshanTestServer server) {
        isNotNull();
        isNotNull(server);

        Registration r = getRegistration(server);
        if (r == null) {
            failWithMessage("<%s> client is not registered", actual.getEndpointName());
        }
        if (server.getPresenceService().isClientAwake(r)) {
            failWithMessage("Expected <%s> client was sleeping", actual.getEndpointName());
        }
        return this;
    }

}
