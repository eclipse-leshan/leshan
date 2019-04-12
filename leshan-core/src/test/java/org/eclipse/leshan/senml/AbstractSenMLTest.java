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

public abstract class AbstractSenMLTest {
    protected void givenResourceWithFloatValue(SenMLPack pack, String n, Number value) {
        SenMLRecord elt = new SenMLRecord();
        elt.setName(n);
        elt.setFloatValue(value);
        pack.addRecord(elt);
    }

    protected void givenResourceWithStringValue(SenMLPack pack, String n, String value) {
        givenResourceWithStringValue(pack, null, n, value);
    }

    protected void givenResourceWithStringValue(SenMLPack pack, String bn, String n, String value) {
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
        b.append("{\"n\":\"13\",\"v\":1.3674912E9},");
        b.append("{\"n\":\"14\",\"vs\":\"+02:00\"},");
        b.append("{\"n\":\"16\",\"vs\":\"U\"}]");
        return b.toString();
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
        givenResourceWithFloatValue(pack, "13", 1367491215l);

        givenResourceWithStringValue(pack, "14", "+02:00");
        givenResourceWithStringValue(pack, "16", "U");

        return pack;
    }
}
