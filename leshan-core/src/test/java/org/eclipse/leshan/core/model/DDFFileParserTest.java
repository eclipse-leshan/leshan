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
package org.eclipse.leshan.core.model;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class DDFFileParserTest {

    @Test
    public void test_xxe_injection_failed() throws IOException, InvalidModelException, InvalidDDFFileException {
        // check with validation
        assertThrows(InvalidDDFFileException.class, () -> {
            ObjectLoader.loadDdfResources("/models/", new String[] { "xxe_injection.xml" }, true);
        });

        // check without validation
        assertThrows(InvalidDDFFileException.class, () -> {
            ObjectLoader.loadDdfResources("/models/", new String[] { "xxe_injection.xml" }, false);
        });
    }
}
