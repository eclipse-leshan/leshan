/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.core.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;

public class TestObjectLoader {

    public static final String[] ddfpaths = new String[] { "3441.xml", "3442.xml" };

    public static List<ObjectModel> loadAllDefault() {
        List<ObjectModel> result = new ArrayList<>();
        try {
            // add default models
            result.addAll(ObjectLoader.loadAllDefault());
            // add test models
            result.addAll(ObjectLoader.loadDdfResources("/models/", ddfpaths));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load models", e);
        }
        return result;
    }
}
