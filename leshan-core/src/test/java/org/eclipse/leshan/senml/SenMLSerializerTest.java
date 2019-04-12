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

import org.eclipse.leshan.util.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SenMLSerializerTest {

    private Logger LOG = LoggerFactory.getLogger(getClass());

    private SenMLPack givenDeviceObjectInstance() {
        SenMLPack pack = new SenMLPack();

        SenMLRecord elt1 = new SenMLRecord();
        elt1.setBaseName("/3/0/");
        elt1.setName("0");
        elt1.setStringValue("Open Mobile Alliance");
        pack.addRecord(elt1);

        SenMLRecord elt2 = new SenMLRecord();
        elt2.setName("1");
        elt2.setStringValue("Lightweight M2M Client");
        pack.addRecord(elt2);

        SenMLRecord elt3 = new SenMLRecord();
        elt3.setName("2");
        elt3.setStringValue("345000123");
        pack.addRecord(elt3);

        SenMLRecord elt4 = new SenMLRecord();
        elt4.setName("6/0");
        elt4.setFloatValue(1);
        pack.addRecord(elt4);

        SenMLRecord elt5 = new SenMLRecord();
        elt5.setName("6/1");
        elt5.setFloatValue(5);
        pack.addRecord(elt5);

        SenMLRecord elt6 = new SenMLRecord();
        elt6.setName("7/0");
        elt6.setFloatValue(3800);
        pack.addRecord(elt6);

        SenMLRecord elt7 = new SenMLRecord();
        elt7.setName("7/1");
        elt7.setFloatValue(5000);
        pack.addRecord(elt7);

        SenMLRecord elt8 = new SenMLRecord();
        elt8.setName("8/0");
        elt8.setFloatValue(125);
        pack.addRecord(elt8);

        SenMLRecord elt9 = new SenMLRecord();
        elt9.setName("8/1");
        elt9.setFloatValue(900);
        pack.addRecord(elt9);

        SenMLRecord elt10 = new SenMLRecord();
        elt10.setName("9");
        elt10.setFloatValue(100);
        pack.addRecord(elt10);

        SenMLRecord elt11 = new SenMLRecord();
        elt11.setName("10");
        elt11.setFloatValue(15);
        pack.addRecord(elt11);

        SenMLRecord elt12 = new SenMLRecord();
        elt12.setName("11/0");
        elt12.setFloatValue(0);
        pack.addRecord(elt12);

        SenMLRecord elt13 = new SenMLRecord();
        elt13.setName("13");
        elt13.setFloatValue(1367491215l);
        pack.addRecord(elt13);

        SenMLRecord elt14 = new SenMLRecord();
        elt14.setName("14");
        elt14.setStringValue("+02:00");
        pack.addRecord(elt14);

        SenMLRecord elt15 = new SenMLRecord();
        elt15.setName("15");
        elt15.setStringValue("U");
        pack.addRecord(elt15);

        return pack;
    }

    @Test
    public void serialize_device_object_to_senml_json() {
        String json = SenMLJson.toJsonSenML(givenDeviceObjectInstance());
        LOG.debug("JSON String: " + json);

        StringBuilder b = new StringBuilder();
        b.append("[{\"bn\":\"/3/0/\",\"n\":\"0\",\"vs\":\"Open Mobile Alliance\"},");
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

        Assert.assertTrue(json.equals(b.toString()));
    }

    @Test
    public void serialize_device_object_to_senml_cbor() {
        byte[] cbor = SenMLCbor.toSenMLCbor(givenDeviceObjectInstance());
        LOG.debug("CBOR: " + Hex.encodeHexString(cbor));

        String expectedValue = "9fbf622d32652f332f302f613061306133744f70656e204d6f62696c6520416c6c69616e6365ffbf6"
                + "13061316133764c69676874776569676874204d324d20436c69656e74ffbf61306132613369333435303030313233f"
                + "fbf613063362f306132fa3f800000ffbf613063362f316132fa40a00000ffbf613063372f306132fa456d8000ffbf6"
                + "13063372f316132fa459c4000ffbf613063382f306132fa42fa0000ffbf613063382f316132fa44610000ffbf61306"
                + "1396132fa42c80000ffbf61306231306132fa41700000ffbf61306431312f306132fa00000000ffbf61306231336132"
                + "fa4ea30485ffbf61306231346133662b30323a3030ffbf613062313561336155ffff";

        Assert.assertTrue(expectedValue.equals(Hex.encodeHexString(cbor)));
    }
}
