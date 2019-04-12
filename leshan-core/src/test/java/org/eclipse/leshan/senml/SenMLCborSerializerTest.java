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

import org.eclipse.leshan.core.util.Hex;
import org.junit.Assert;
import org.junit.Test;

public class SenMLCborSerializerTest extends AbstractSenMLTest {

    @Test
    public void serialize_device_object_to_senml_cbor() throws Exception {
        byte[] cbor = SenMLCbor.toSenMLCbor(givenDeviceObjectInstance());

        String expectedValue = "90a321652f332f302f00613003744f70656e204d6f62696c6520416c6c69616e6365a200613103764c696768747765696768"
                + "74204d324d20436c69656e74a20061320369333435303030313233a20061330363312e30a20063362f300201a20063362f310205a20063372f"
                + "3002190ed8a20063372f3102191388a20063382f3002187da20063382f3102190384a2006139021864a200623130020fa2006431312f300200"
                + "a200623133021a5182428fa20062313403662b30323a3030a200623136036155";

        Assert.assertTrue(expectedValue.equals(Hex.encodeHexString(cbor)));
    }
}
