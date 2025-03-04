/*******************************************************************************
 * Copyright (c) 2025 Sierra Wireless and others.
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
package org.eclipse.leshan.core.node;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class LwM2mNodeUtilTest {

    @Test
    void validate_unsupported_node_is_detected() {
        String error = LwM2mNodeUtil.getUnsupportedNodeCause(new LwM2mObject(1),
                Arrays.asList(LwM2mObjectInstance.class));

        assertNotNull(error);
    }

    @Test
    void validate_supported_node_is_detected() {
        String error = LwM2mNodeUtil.getUnsupportedNodeCause(new LwM2mObject(1),
                Arrays.asList(LwM2mObjectInstance.class, LwM2mNode.class));
        assertNull(error);
    }
}
