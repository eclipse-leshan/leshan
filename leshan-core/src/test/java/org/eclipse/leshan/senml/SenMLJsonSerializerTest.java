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

public class SenMLJsonSerializerTest extends AbstractSenMLTest {

    @Test
    public void serialize_device_object_to_senml_json() {
        String json = SenMLJson.toSenMLJson(givenDeviceObjectInstance());
        Assert.assertTrue(json.equals(givenSenMLJsonExample()));
    }
}
