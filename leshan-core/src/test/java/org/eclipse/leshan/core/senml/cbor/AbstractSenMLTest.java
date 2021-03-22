/*******************************************************************************
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
 *     Boya Zhang - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.core.senml.cbor;

import org.eclipse.leshan.senml.SenMLPack;
import org.eclipse.leshan.senml.SenMLRecord;

public abstract class AbstractSenMLTest {

    private void givenResourceWithFloatValue(SenMLPack pack, String n, Number value) {
        SenMLRecord elt = new SenMLRecord();
        elt.setName(n);
        elt.setFloatValue(value);
        pack.addRecord(elt);
    }

    private void givenResourceWithStringValue(SenMLPack pack, String n, String value) {
        givenResourceWithStringValue(pack, null, n, value);
    }

    private void givenResourceWithStringValue(SenMLPack pack, String bn, String n, String value) {
        SenMLRecord elt = new SenMLRecord();
        if (bn != null) {
            elt.setBaseName(bn);
        }

        elt.setName(n);
        elt.setStringValue(value);
        pack.addRecord(elt);
    }

    /**
     * Example of JSON payload request to Device Object of the LwM2M example client (Read /3/0)
     * 
     * @return JSON payload
     */
    protected String givenSenMLJsonExample() {
        StringBuilder b = new StringBuilder();
        b.append("[{\"bn\":\"/3/0/\",\"n\":\"0\",\"vs\":\"Open Mobile Alliance\"},");
        b.append("{\"n\":\"1\",\"vs\":\"Lightweight M2M Client\"},");
        b.append("{\"n\":\"2\",\"vs\":\"345000123\"},");
        b.append("{\"n\":\"3\",\"vs\":\"1.0\"},");
        b.append("{\"n\":\"6/0\",\"v\":1},{\"n\":\"6/1\",\"v\":5},");
        b.append("{\"n\":\"7/0\",\"v\":3800},{\"n\":\"7/1\",\"v\":5000},");
        b.append("{\"n\":\"8/0\",\"v\":125},{\"n\":\"8/1\",\"v\":900},");
        b.append("{\"n\":\"9\",\"v\":100},");
        b.append("{\"n\":\"10\",\"v\":15},");
        b.append("{\"n\":\"11/0\",\"v\":0},");
        b.append("{\"n\":\"13\",\"v\":1367491215},");
        b.append("{\"n\":\"14\",\"vs\":\"+02:00\"},");
        b.append("{\"n\":\"16\",\"vs\":\"U\"}]");
        return b.toString();
    }

    /**
     * Example of CBOR payload request to Device Object of the LwM2M example client (Read /3/0)
     * 
     * @return JSON payload
     */
    protected String givenSenMLCborExample() {
        return "90a321652f332f302f00613003744f70656e204d6f62696c6520416c6c69616e6365a200613103764c696768747765696768"
                + "74204d324d20436c69656e74a20061320369333435303030313233a20061330363312e30a20063362f300201a20063362f310205a20063372f"
                + "3002190ed8a20063372f3102191388a20063382f3002187da20063382f3102190384a2006139021864a200623130020fa2006431312f300200"
                + "a200623133021a5182428fa20062313403662b30323a3030a200623136036155";
    }

    protected SenMLPack givenDeviceObjectInstance() {
        SenMLPack pack = new SenMLPack();

        givenResourceWithStringValue(pack, "/3/0/", "0", "Open Mobile Alliance");
        givenResourceWithStringValue(pack, "1", "Lightweight M2M Client");
        givenResourceWithStringValue(pack, "2", "345000123");
        givenResourceWithStringValue(pack, "3", "1.0");

        givenResourceWithFloatValue(pack, "6/0", 1);
        givenResourceWithFloatValue(pack, "6/1", 5);
        givenResourceWithFloatValue(pack, "7/0", 3800);
        givenResourceWithFloatValue(pack, "7/1", 5000);
        givenResourceWithFloatValue(pack, "8/0", 125);
        givenResourceWithFloatValue(pack, "8/1", 900);

        givenResourceWithFloatValue(pack, "9", 100);
        givenResourceWithFloatValue(pack, "10", 15);
        givenResourceWithFloatValue(pack, "11/0", 0);
        givenResourceWithFloatValue(pack, "13", 1367491215);

        givenResourceWithStringValue(pack, "14", "+02:00");
        givenResourceWithStringValue(pack, "16", "U");

        return pack;
    }

    protected SenMLPack getPackWithSingleOpaqueValue(String path, byte[] value) {
        SenMLPack pack = new SenMLPack();

        SenMLRecord r = new SenMLRecord();
        r.setBaseName(path);
        r.setOpaqueValue(value);
        pack.addRecord(r);

        return pack;
    }
}
