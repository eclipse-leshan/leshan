/*******************************************************************************
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
 *     Boya Zhang - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.senml;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SenMLDeserializerTest {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void deserialize_device_object() {
        StringBuilder b = new StringBuilder();
        b.append("[{\"bn\":\"/3/0\",\"n\":\"0\",\"vs\":\"Open Mobile Alliance\"},");
        b.append("{\"n\":\"1\",\"vs\":\"Lightweight M2M Client\"},");
        b.append("{\"n\":\"2\",\"vs\":\"345000123\"},");
        b.append("{\"n\":\"6/0\",\"v\":1},{\"n\":\"6/1\",\"v\":5},");
        b.append("{\"n\":\"7/0\",\"v\":3800},{\"n\":\"7/1\",\"v\":5000},");
        b.append("{\"n\":\"8/0\",\"v\":125},{\"n\":\"8/1\",\"v\":900},");
        b.append("{\"n\":\"9\",\"v\":100},");
        b.append("{\"n\":\"10\",\"v\":15},");
        b.append("{\"n\":\"11/0\",\"v\":0},");
        b.append("{\"n\":\"13\",\"v\":1.3674912E9},");
        b.append("{\"n\":\"14\",\"vs\":\"+02:00\"},");
        b.append("{\"n\":\"15\",\"vs\":\"U\"}]");

        String dataString = b.toString();
        log.debug(dataString.trim());
        SenMLPack pack = SenMLJson.fromJsonSenML(dataString);
        log.debug(pack.toString());

        String outString = SenMLJson.toJsonSenML(pack);
        Assert.assertEquals(dataString.trim(), outString);
    }
}
