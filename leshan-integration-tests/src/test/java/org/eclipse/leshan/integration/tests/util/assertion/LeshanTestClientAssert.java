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

import org.assertj.core.api.AbstractAssert;
import org.eclipse.leshan.integration.tests.util.LeshanTestClient;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;

public class LeshanTestClientAssert extends AbstractAssert<LeshanTestClientAssert, LeshanTestClient> {

    public LeshanTestClientAssert(LeshanTestClient actual) {
        super(actual, LeshanTestClientAssert.class);
    }

    public static LeshanTestClientAssert assertThat(LeshanTestClient actual) {
        return new LeshanTestClientAssert(actual);
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
            failWithMessage("Expected No Registration for <%s> client but have <%>", actual.getEndpointName(), r);
        }
        return this;
    }

    private void isNotNull(LeshanServer server) {
        if (server == null)
            failWithMessage("server MUST NOT be null");
    }

    private Registration getRegistration(LeshanServer server) {
        return server.getRegistrationService().getByEndpoint(actual.getEndpointName());
    }
}
