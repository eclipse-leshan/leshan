/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.json.ObjectModelSerDes;
import org.junit.Assert;
import org.junit.Test;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;

public class ObjectModelSerDesTest {

    @Test
    public void des_ser_must_be_equals() throws IOException {
        // load file
        InputStream inputStream = ObjectModelSerDesTest.class.getResourceAsStream("/model.json");
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        String smodel = result.toString("UTF-8");

        // deserialize
        ObjectModelSerDes serDes = new ObjectModelSerDes();
        JsonValue json = Json.parse(smodel);
        List<ObjectModel> models = serDes.deserialize(json.asArray());

        // serialize
        JsonArray arr = serDes.jSerialize(models);
        String res = arr.toString(WriterConfig.PRETTY_PRINT);

        Assert.assertEquals("value should be equals", smodel, res);
    }
}
