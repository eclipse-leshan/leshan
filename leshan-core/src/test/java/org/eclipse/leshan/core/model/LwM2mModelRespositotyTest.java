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
package org.eclipse.leshan.core.model;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.leshan.core.model.ResourceModel.Operations;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.junit.Test;

public class LwM2mModelRespositotyTest {

    @Test
    public void validate_get_specific_version() throws InvalidModelException, InvalidDDFFileException, IOException {

        // create repository
        List<ObjectModel> models = new ArrayList<ObjectModel>();
        models.add(createModel(0, "1.0"));
        models.add(createModel(1, "1.0"));
        models.add(createModel(1, "1.1"));
        models.add(createModel(2, "1.0"));
        models.add(createModel(2, "1.1"));
        LwM2mModelRepository repository = new LwM2mModelRepository(models);

        // validate get specific version
        ObjectModel objectModel = repository.getObjectModel(1, "1.1");
        assertEquals(objectModel.id, (Integer) 1);
        assertEquals(objectModel.version, "1.1");

        objectModel = repository.getObjectModel(2, "1.0");
        assertEquals(objectModel.id, (Integer) 2);
        assertEquals(objectModel.version, "1.0");

        objectModel = repository.getObjectModel(3, "1.0");
        assertNull(objectModel);

        objectModel = repository.getObjectModel(2, "1.2");
        assertNull(objectModel);
    }

    @Test
    public void validate_get_last_version() throws InvalidModelException, InvalidDDFFileException, IOException {

        // create repository
        List<ObjectModel> models = new ArrayList<ObjectModel>();
        models.add(createModel(0, "1.0"));
        models.add(createModel(1, "1.0"));
        models.add(createModel(1, "1.1"));
        models.add(createModel(2, "1.0"));
        models.add(createModel(2, "1.1"));
        LwM2mModelRepository repository = new LwM2mModelRepository(models);

        // validate get most recent version
        ObjectModel objectModel = repository.getObjectModel(0);
        assertEquals(objectModel.id, (Integer) 0);
        assertEquals(objectModel.version, "1.0");

        objectModel = repository.getObjectModel(1);
        assertEquals(objectModel.id, (Integer) 1);
        assertEquals(objectModel.version, "1.1");

        objectModel = repository.getObjectModel(2);
        assertEquals(objectModel.id, (Integer) 2);
        assertEquals(objectModel.version, "1.1");

        objectModel = repository.getObjectModel(3);
        assertNull(objectModel);
    }

    private ObjectModel createModel(Integer objectId, String version) {
        ResourceModel resourceModel = new ResourceModel(0, "a resource", Operations.R, false, false, Type.BOOLEAN, null,
                null, null);
        return new ObjectModel(objectId, "Object " + objectId, null, version, false, false, resourceModel);

    }
}
