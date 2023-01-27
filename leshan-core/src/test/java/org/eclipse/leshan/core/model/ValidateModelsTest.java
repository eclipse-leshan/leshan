/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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

import java.io.IOException;

import org.eclipse.leshan.core.util.TestObjectLoader;
import org.junit.jupiter.api.Test;

public class ValidateModelsTest {

    @Test
    public void validate_embedded_models() throws InvalidModelException, InvalidDDFFileException, IOException {
        ObjectLoader.loadDdfResources("/models/", ObjectLoader.ddfpaths, true);
    }

    @Test
    public void validate_test_models() throws InvalidModelException, InvalidDDFFileException, IOException {
        ObjectLoader.loadDdfResources("/models/", TestObjectLoader.ddfpaths, true);
    }
}
