/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.model.LwM2mModelRepository;
import org.eclipse.leshan.core.model.ObjectModel;

public class TestObjectLoader {

    /**
     * Load default models fixing version to avoid tests break when we update to last models.
     */
    public static List<ObjectModel> loadDefaultObject() {
        // load default object from the spec (with fixed version)
        LwM2mModelRepository repository = new LwM2mModelRepository(
                org.eclipse.leshan.core.util.TestObjectLoader.loadAllDefault());
        List<ObjectModel> objectModels = new ArrayList<ObjectModel>();
        objectModels.add(repository.getObjectModel(LwM2mId.SECURITY, "1.1"));
        objectModels.add(repository.getObjectModel(LwM2mId.SERVER, "1.1"));
        objectModels.add(repository.getObjectModel(LwM2mId.DEVICE, "1.1"));
        objectModels.add(repository.getObjectModel(LwM2mId.ACCESS_CONTROL, "1.0"));
        objectModels.add(repository.getObjectModel(LwM2mId.CONNECTIVITY_MONITORING, "1.0"));
        objectModels.add(repository.getObjectModel(LwM2mId.FIRMWARE, "1.0"));
        objectModels.add(repository.getObjectModel(LwM2mId.LOCATION, "1.0"));
        objectModels.add(repository.getObjectModel(LwM2mId.CONNECTIVITY_STATISTICS, "1.0"));
        objectModels.add(repository.getObjectModel(LwM2mId.OSCORE, "2.0"));

        // Test object 3442
        objectModels.add(repository.getObjectModel(3442, "1.0"));

        return objectModels;
    }
}
