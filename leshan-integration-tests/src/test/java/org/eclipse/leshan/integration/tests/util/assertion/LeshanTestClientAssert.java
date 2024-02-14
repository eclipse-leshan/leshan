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
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.NotificationAttributeTree;
import org.eclipse.leshan.core.node.LwM2mPath;
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

    public void hasNoAttributeSetFor(LwM2mServer server, LwM2mPath path) {
        Integer objectId = path.getObjectId();
        if (objectId == null) {
            throw new IllegalArgumentException("Path hasn't any object id");
        }
        LwM2mObjectEnabler objectEnabler = actual.getObjectTree().getObjectEnabler(objectId);

        if (objectEnabler != null) {
            NotificationAttributeTree attributeTree = objectEnabler.getAttributesFor(server);
            LwM2mAttributeSet attributeSet = attributeTree.get(path);
            if (attributeSet != null && !attributeSet.isEmpty()) {
                failWithMessage("Attribute Set for path %s of server %s was expected to be empty but was %s", path,
                        server.getId(), attributeSet);
            }
        }
        // else if there is no object enabler, there is no more attribute attached.
    }

    public void hasAttributesFor(LwM2mServer server, LwM2mPath path, LwM2mAttributeSet expectedAttributeSet) {
        Integer objectId = path.getObjectId();
        if (objectId == null) {
            throw new IllegalArgumentException("Path hasn't any object id");
        }
        LwM2mObjectEnabler objectEnabler = actual.getObjectTree().getObjectEnabler(objectId);
        if (objectEnabler == null) {
            failWithMessage("%s attribute set was expected for path %s of server %s but there is not object with id %s",
                    expectedAttributeSet, path, server.getId(), objectId);
        } else {
            NotificationAttributeTree attributeTree = objectEnabler.getAttributesFor(server);
            LwM2mAttributeSet attributeSet = attributeTree.get(path);
            if (attributeSet == null || attributeSet.isEmpty()) {
                failWithMessage("%s attribute set was expected for path %s of server %s  but it is empty",
                        expectedAttributeSet, path, server.getId());
            } else {
                if (!attributeSet.equals(expectedAttributeSet)) {
                    failWithMessage("%s attribute set was expected for path %s of server %s  but it was %s",
                            expectedAttributeSet, path, server.getId(), attributeSet);
                }
            }
        }

    }

    public void hasNoNotificationData() {
        if (!actual.getNotificationDataStore().isEmpty()) {
            failWithMessage("Notificatoin Data store should be empty");
        }
    }

    public void hasNotificationData() {
        if (actual.getNotificationDataStore().isEmpty()) {
            failWithMessage("Notificatoin Data store should NOT be empty");
        }
    }

}
