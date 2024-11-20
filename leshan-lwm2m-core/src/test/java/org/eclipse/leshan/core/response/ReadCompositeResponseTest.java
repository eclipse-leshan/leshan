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
package org.eclipse.leshan.core.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.eclipse.leshan.core.ResponseCode;
import org.junit.jupiter.api.Test;

public class ReadCompositeResponseTest {

    @Test
    public void create_failure_reponse() {
        ReadCompositeResponse response = ReadCompositeResponse.notFound();
        assertEquals(response.getCode(), ResponseCode.NOT_FOUND);
        assertNull(response.getContent());
    }
}
