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

public class SenMLJsonDeserializerTest extends AbstractSenMLTest {

    @Test
    public void deserialize_device_object() {
        String dataString = givenSenMLJsonExample();
        SenMLPack pack = SenMLJson.fromSenMLJson(dataString);

        String outString = SenMLJson.toSenMLJson(pack);
        Assert.assertEquals(dataString.trim(), outString);
    }
}
