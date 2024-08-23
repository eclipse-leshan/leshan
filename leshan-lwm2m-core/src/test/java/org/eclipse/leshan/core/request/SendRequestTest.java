/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
package org.eclipse.leshan.core.request;

import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class SendRequestTest {

    @Test
    public void send_request_with_empty_nodes_should_fail() {
        assertThrowsExactly(InvalidRequestException.class, () -> {
            new SendRequest(ContentFormat.SENML_JSON, Collections.emptyMap());
        });
    }

    @Test
    public void send_request_with_node_without_path_should_fail() {
        final Map<LwM2mPath, LwM2mNode> nodes = new HashMap<>();
        nodes.put(null, LwM2mSingleResource.newBooleanResource(1, false));

        assertThrowsExactly(InvalidRequestException.class, () -> {

            new SendRequest(ContentFormat.SENML_JSON, nodes);
        });
    }

    @Test
    public void send_request_with_path_without_node_should_fail() {
        final Map<LwM2mPath, LwM2mNode> nodes = new HashMap<>();
        nodes.put(new LwM2mPath(3, 0, 0), null);

        assertThrowsExactly(InvalidRequestException.class, () -> {

            new SendRequest(ContentFormat.SENML_JSON, nodes);
        });
    }

    private class ExtendedSendRequest extends SendRequest {
        ExtendedSendRequest(ContentFormat format, Map<LwM2mPath, LwM2mNode> nodes) {
            super(format, nodes, null);
        }

        @Override
        public boolean canEqual(Object obj) {
            return (obj instanceof ExtendedSendRequest);
        }
    }

    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(SendRequest.class).withRedefinedSubclass(ExtendedSendRequest.class)
                .withIgnoredFields("coapRequest").verify();
    }
}
